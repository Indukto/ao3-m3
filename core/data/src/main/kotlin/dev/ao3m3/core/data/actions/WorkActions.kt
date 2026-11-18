package dev.ao3m3.core.data.actions

import com.fleeksoft.ksoup.nodes.Document
import dev.ao3m3.core.data.net.Ao3Client
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.net.Ao3Urls
import dev.ao3m3.core.data.parse.Ao3Selectors
import dev.ao3m3.core.data.parse.LibraryParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Write actions against AO3. Each one re-posts an existing form from the work page, which keeps the
 * app honest about what it is doing and resilient to AO3 adding fields. Where AO3 only exposes the
 * action as a plain link (mark for later, bookmark) the known endpoint is used with the page's own
 * authenticity token. Any failure is reported to the caller so the UI can roll its change back.
 */
@Singleton
class WorkActions @Inject constructor(
    private val client: Ao3Client,
) {

    /**
     * AO3 toggles this one with two routes: PATCH /mark_for_later adds the work to your Marked for
     * Later list, PATCH /mark_as_read takes it off again.
     */
    suspend fun setMarkedForLater(workId: Long, marked: Boolean): Result<Unit> = runCatching {
        val doc = signedInWorkPage(workId)
        val path = if (marked) "mark_for_later" else "mark_as_read"
        val form = FormScraper.find(doc, "/$path")
        if (form != null) {
            client.postForm(form.action, form.fields)
        } else {
            client.postForm("/works/$workId/$path", methodFields(doc, "patch"))
        }
    }

    /**
     * Creating and removing a bookmark are different routes: POST /works/:id/bookmarks with AO3's
     * own form fields (the notes field is `bookmark[bookmarker_notes]`), and DELETE /bookmarks/:id
     * for the bookmark that the work page links to as "Edit Bookmark".
     */
    suspend fun setBookmarked(
        workId: Long,
        bookmarked: Boolean,
        notes: String = "",
    ): Result<Unit> = runCatching {
        val doc = signedInWorkPage(workId)
        if (bookmarked) {
            val form = FormScraper.findIn(doc, Ao3Selectors.BOOKMARK_FORM)
                ?: FormScraper.find(doc, "/bookmarks")
                ?: newBookmarkForm(workId)
                ?: throw Ao3Exception.Parse("no bookmark form for $workId")
            client.postForm(
                form.action,
                form.fields + ("bookmark[bookmarker_notes]" to notes),
            )
        } else {
            val bookmarkId = bookmarkId(doc)
                ?: throw Ao3Exception.Parse("no bookmark to remove for $workId")
            client.postForm("/bookmarks/$bookmarkId", methodFields(doc, "delete"))
        }
    }

    suspend fun setSubscribed(workId: Long): Result<Unit> = runCatching {
        val doc = signedInWorkPage(workId)
        val form = FormScraper.find(doc, "/subscriptions")
            ?: throw Ao3Exception.Parse("no subscription form for $workId")
        client.postForm(form.action, form.fields)
    }

    /**
     * Loads the work page and proves the session is real. AO3 hands a session cookie to anonymous
     * visitors too, so a stored cookie alone is not a login: without a username the signed-in
     * controls are absent and every write would fail with a confusing parse error instead of
     * telling the reader to log in again.
     */
    private suspend fun signedInWorkPage(workId: Long): Document {
        if (!client.hasSession()) throw Ao3Exception.Unauthorized()
        val doc = client.getHtml(Ao3Urls.workPage(workId))
        if (!looksSignedIn(doc)) throw Ao3Exception.Unauthorized()
        return doc
    }

    /** Any user-only control (masthead name, bookmark or mark-for-later form) proves a session. */
    private fun looksSignedIn(doc: Document): Boolean =
        LibraryParser.parseUserName(doc) != null ||
            doc.select(Ao3Selectors.BOOKMARK_FORM).isNotEmpty() ||
            FormScraper.has(doc, "/mark_for_later") ||
            FormScraper.has(doc, "/mark_as_read") ||
            FormScraper.has(doc, "/bookmarks") ||
            FormScraper.has(doc, "/subscriptions")

    /** AO3's routes want PATCH/DELETE; Rails reads that from `_method` on a POST. */
    private fun methodFields(doc: Document, method: String): Map<String, String> = buildMap {
        put("_method", method)
        FormScraper.token(doc)?.let { put("authenticity_token", it) }
    }

    /** The create form, for pages that render the bookmark form as a link rather than inline. */
    private suspend fun newBookmarkForm(workId: Long): FormScraper.Form? = runCatching {
        val doc = client.getHtml(Ao3Urls.newBookmark(workId))
        FormScraper.findIn(doc, Ao3Selectors.BOOKMARK_FORM) ?: FormScraper.find(doc, "/bookmarks")
    }.getOrNull()

    /** The existing bookmark's id, from the edit form's action or an "Edit Bookmark" link. */
    private fun bookmarkId(doc: Document): Long? {
        val targets = doc.select("form[action]").map { it.attr("action") } +
            doc.select("a[href]").map { it.attr("href") }
        return targets.firstNotNullOfOrNull { raw ->
            Regex("/bookmarks/(\\d+)(?:/edit)?/?$")
                .find(raw.substringBefore('?').substringBefore('#'))
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
        }
    }
}
