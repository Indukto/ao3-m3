package dev.ao3m3.core.data.parse

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.ChapterText
import dev.ao3m3.core.model.Work
import dev.ao3m3.core.model.WorkWithChapters

/**
 * Parses a single work page (or a chapter page, which shares the same content containers).
 * A one-shot has no table of contents, so a single chapter is synthesised whose id is the work id;
 * the reader uses that to know it should load the work page rather than a /chapters/ URL.
 */
object WorkParser {

    fun parse(doc: Document, workId: Long): WorkWithChapters {
        val titleElement = doc.firstOf(Ao3Selectors.WORK_TITLE)
            ?: throw Ao3Exception.Parse("no work title")

        val stats: Element = doc.firstOf(Ao3Selectors.WORK_STATS)
            ?: titleElement.parent()
            ?: titleElement
        val (published, total) = parseChapterCounts(
            stats.select(Ao3Selectors.STAT_CHAPTERS).firstOrNull()?.text(),
        )
        val firstDate = parseAo3Date(
            stats.select(Ao3Selectors.STAT_PUBLISHED).firstOrNull()?.text(),
        )

        val work = Work(
            id = workId,
            title = stripChapterSuffix(titleElement.text().trim()),
            authors = authors(doc),
            summary = doc.htmlOf(Ao3Selectors.WORK_SUMMARY)?.let(WorkParser::stripTags).orEmpty(),
            rating = doc.textOf(listOf(Ao3Selectors.WORK_RATING)),
            warnings = doc.select(Ao3Selectors.WORK_WARNINGS).texts(),
            categories = doc.select(Ao3Selectors.WORK_CATEGORIES).texts(),
            fandoms = doc.select(Ao3Selectors.WORK_FANDOMS).texts(),
            relationships = doc.select(Ao3Selectors.WORK_RELATIONSHIPS).texts(),
            characters = doc.select(Ao3Selectors.WORK_CHARACTERS).texts(),
            additionalTags = doc.select(Ao3Selectors.WORK_FREEFORMS).texts(),
            words = parseIntOrNull(stats.select(Ao3Selectors.STAT_WORDS).firstOrNull()?.text()),
            chaptersPublished = published,
            chaptersTotal = total,
            kudos = parseIntOrNull(stats.select(Ao3Selectors.STAT_KUDOS).firstOrNull()?.text()),
            hits = parseIntOrNull(stats.select(Ao3Selectors.STAT_HITS).firstOrNull()?.text()),
            bookmarks = parseIntOrNull(
                stats.select(Ao3Selectors.STAT_BOOKMARKS).firstOrNull()?.text(),
            ),
            commentCount = parseIntOrNull(
                stats.select(Ao3Selectors.STAT_COMMENTS).firstOrNull()?.text(),
            ),
            language = stats.select(Ao3Selectors.STAT_LANGUAGE).firstOrNull()?.text()?.trim(),
            published = firstDate,
        )

        return WorkWithChapters(
            work = work,
            chapters = parseChapters(doc, work),
            notes = doc.htmlOf(Ao3Selectors.WORK_NOTES)?.let(WorkParser::stripTags),
            endNotes = doc.htmlOf(listOf(Ao3Selectors.WORK_END_NOTES))
                ?.let(WorkParser::stripTags),
        )
    }

    fun parseChapters(doc: Document, work: Work): List<Chapter> {
        val links = doc.select(Ao3Selectors.WORK_CHAPTER_TOC.first())
        val chapters = links.mapIndexedNotNull { position, link ->
            val chapterId = Regex("/chapters/(\\d+)")
                .find(link.attr("href"))
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return@mapIndexedNotNull null
            val label = link.text().trim()
            val index = Regex("^(\\d+)").find(label)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: (position + 1)
            Chapter(
                id = chapterId,
                workId = work.id,
                index = index,
                title = label.replace(Regex("^\\s*\\d+\\.?\\s*"), "").trim()
                    .takeIf { it.isNotEmpty() },
            )
        }.distinctBy { it.id }.sortedBy { it.index }

        if (chapters.isNotEmpty()) return chapters

        return listOf(Chapter(id = work.id, workId = work.id, index = 1, title = work.title))
    }

    /** Extracts one chapter's HTML body. Works for both work and chapter pages. */
    fun parseChapterText(
        doc: Document,
        chapterId: Long,
        workId: Long,
        index: Int,
        title: String?,
    ): ChapterText {
        val content = doc.select(Ao3Selectors.WORK_USERSTUFF).firstOrNull()
            ?: throw Ao3Exception.Parse("no chapter body")

        return ChapterText(
            chapterId = chapterId,
            workId = workId,
            index = index,
            title = title,
            html = content.html(),
            notes = doc.select("#chapters .chapter .notes .userstuff")
                .firstOrNull()
                ?.html()
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
            endNotes = doc.select(Ao3Selectors.WORK_END_NOTES)
                .firstOrNull()
                ?.html()
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
        )
    }

    private fun authors(doc: Document): List<String> =
        Ao3Selectors.WORK_AUTHOR
            .firstNotNullOfOrNull { selector -> doc.select(selector).texts().ifEmpty { null } }
            .orEmpty()

    /** Minimal tag stripping for summaries/notes; the reader does the real HTML rendering. */
    fun stripTags(html: String): String = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
