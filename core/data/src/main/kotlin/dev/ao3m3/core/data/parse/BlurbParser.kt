package dev.ao3m3.core.data.parse

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import dev.ao3m3.core.model.Work

/**
 * Parses AO3 blurb lists. A blurb is the smallest self-contained unit of work metadata on AO3 and
 * appears on tag pages, search results, bookmarks, history and marked-for-later, so one parser
 * covers most of the app's read path.
 */
object BlurbParser {

    fun parse(doc: Document): List<Work> =
        doc.select(Ao3Selectors.BLURB_QUERY)
            .mapNotNull { parseBlurb(it) }
            .distinctBy { it.id }

    fun parseBlurb(element: Element): Work? {
        val titleElement = element.firstOf(Ao3Selectors.BLURB_TITLE) ?: return null
        val id = regexWorkId(titleElement.attr("href"))
            ?: regexWorkId(element.attr("id"))
            ?: return null
        val title = titleElement.text().trim()
        if (title.isEmpty()) return null

        val (published, total) = parseChapterCounts(
            element.select(Ao3Selectors.STAT_CHAPTERS).firstOrNull()?.text(),
        )

        return Work(
            id = id,
            title = stripChapterSuffix(title),
            authors = authors(element),
            summary = element.textOf(Ao3Selectors.BLURB_SUMMARY).orEmpty(),
            rating = element.select(Ao3Selectors.BLURB_RATING)
                .firstOrNull()
                ?.attr("title")
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            warnings = element.select(Ao3Selectors.BLURB_WARNINGS).texts(),
            categories = categories(element),
            fandoms = element.select(Ao3Selectors.BLURB_FANDOMS).texts(),
            relationships = element.select(Ao3Selectors.BLURB_RELATIONSHIPS).texts(),
            characters = element.select(Ao3Selectors.BLURB_CHARACTERS).texts(),
            additionalTags = element.select(Ao3Selectors.BLURB_FREEFORMS).texts(),
            words = parseIntOrNull(element.select(Ao3Selectors.STAT_WORDS).firstOrNull()?.text()),
            chaptersPublished = published,
            chaptersTotal = total,
            kudos = parseIntOrNull(element.select(Ao3Selectors.STAT_KUDOS).firstOrNull()?.text()),
            hits = parseIntOrNull(element.select(Ao3Selectors.STAT_HITS).firstOrNull()?.text()),
            bookmarks = parseIntOrNull(
                element.select(Ao3Selectors.STAT_BOOKMARKS).firstOrNull()?.text(),
            ),
            commentCount = parseIntOrNull(
                element.select(Ao3Selectors.STAT_COMMENTS).firstOrNull()?.text(),
            ),
            language = element.select(Ao3Selectors.STAT_LANGUAGE).firstOrNull()?.text()?.trim(),
            updated = parseAo3Date(element.textOf(Ao3Selectors.BLURB_DATETIME)),
        )
    }

    /** AO3 renders its next-page control as a "Next" link in the pagination list. */
    fun hasNextPage(doc: Document): Boolean =
        doc.select(Ao3Selectors.PAGINATION_LINKS)
            .any { it.text().trim().equals("Next", ignoreCase = true) }

    /** Category symbols first (the only place browsable blurbs carry them), tag list as fallback. */
    private fun categories(element: Element): List<String> {
        val symbols = element
            .select("${Ao3Selectors.BLURB_REQUIRED_TAGS} ${Ao3Selectors.BLURB_CATEGORY_SYMBOLS}")
            .mapNotNull { symbolLabel(it) }
        val tags = element.select(Ao3Selectors.BLURB_CATEGORIES).texts()
        return (symbols + tags).distinct()
    }

    /** "F/M" from the symbol's title attribute, its `.text` child, or the class suffix. */
    private fun symbolLabel(symbol: Element): String? {
        symbol.attr("title").trim().takeIf { it.isNotEmpty() }?.let { return it }
        symbol.select(".text").firstOrNull()?.text()?.trim()
            ?.takeIf { it.isNotEmpty() }?.let { return it }
        val suffix = symbol.className()
            .split(' ')
            .firstOrNull { it.startsWith(CATEGORY_PREFIX) }
            ?.removePrefix(CATEGORY_PREFIX)
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return suffix.split('-').joinToString("/") { part -> part.replaceFirstChar(Char::uppercase) }
    }

    private fun authors(element: Element): List<String> =
        Ao3Selectors.BLURB_AUTHOR
            .firstNotNullOfOrNull { selector -> element.select(selector).texts().ifEmpty { null } }
            .orEmpty()

    private const val CATEGORY_PREFIX = "category-"

    private fun regexWorkId(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        Regex("(?:/works/|work_)(\\d+)").find(raw)?.let { match ->
            return match.groupValues.getOrNull(1)?.toLongOrNull()
        }
        return raw.toLongOrNull()
    }
}
