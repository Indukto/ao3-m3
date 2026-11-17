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

    /** AO3 shows the username in the masthead; used to cache per-account library URLs. */
    fun parseUserName(doc: Document): String? =
        doc.select("#header .primary .dropdown a[href^=/users/]")
            .firstOrNull()
            ?.attr("href")
            ?.substringAfter("/users/")
            ?.substringBefore("/")
            ?.takeIf { it.isNotBlank() }
}
