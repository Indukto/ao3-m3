package dev.ao3m3.core.data.actions

import dev.ao3m3.core.data.net.Ao3Client
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.net.Ao3Urls
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Write actions against AO3. Each one re-posts an existing form from the work page, which keeps the
 * app honest about what it is doing and resilient to AO3 adding fields. Any failure is reported to
 * the caller so the UI can keep the optimistic change or roll it back.
 */
@Singleton
class WorkActions @Inject constructor(
    private val client: Ao3Client,
) {

    suspend fun setMarkedForLater(workId: Long): Result<Unit> =
        submit(workId, listOf("/mark_for_later", "readings"))

    suspend fun setSubscribed(workId: Long): Result<Unit> =
        submit(workId, listOf("/subscriptions"))

    suspend fun createBookmark(workId: Long, notes: String): Result<Unit> =
        submit(
            workId = workId,
            actions = listOf("/bookmarks"),
            overrides = mapOf("bookmark[notes]" to notes),
        )

    private suspend fun submit(
        workId: Long,
        actions: List<String>,
        overrides: Map<String, String> = emptyMap(),
    ): Result<Unit> = runCatching {
        if (!client.hasSession()) throw Ao3Exception.Unauthorized()
        val doc = client.getHtml(Ao3Urls.workPage(workId))
        val form = actions.firstNotNullOfOrNull { FormScraper.find(doc, it) }
            ?: throw Ao3Exception.Parse("no action form for $workId")
        client.postForm(form.action, form.fields + overrides)
    }
}
