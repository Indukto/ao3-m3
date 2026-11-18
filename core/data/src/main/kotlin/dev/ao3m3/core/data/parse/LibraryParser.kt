package dev.ao3m3.core.data.parse

import com.fleeksoft.ksoup.nodes.Document
import dev.ao3m3.core.model.Work

/**
 * Library pages (bookmarks, marked for later, subscriptions, history) are ordinary AO3 index pages
 * built from blurbs, so parsing is delegated to [BlurbParser] with one extra tolerance: history and
 * subscription rows do not always carry stats, which the blurb parser already treats as optional.
 */
object LibraryParser {

    fun parse(doc: Document): List<Work> = BlurbParser.parse(doc)

    fun hasNextPage(doc: Document): Boolean = BlurbParser.hasNextPage(doc)

    /**
     * AO3 shows the username in the masthead; used to cache per-account library URLs and as a
     * signed-in probe (guests are offered `/users/login`, which is not a username).
     */
    fun parseUserName(doc: Document): String? {
        val link = doc.select("#header .primary .dropdown a[href^=/users/]").firstOrNull()
            ?: doc.select("#header a[href^=/users/]").firstOrNull {
                it.text().trim().startsWith("Hi,", ignoreCase = true)
            }
            ?: return null
        return link.attr("href")
            .substringAfter("/users/")
            .substringBefore("/")
            .takeIf { it.isNotBlank() && !it.equals("login", ignoreCase = true) }
    }
}
