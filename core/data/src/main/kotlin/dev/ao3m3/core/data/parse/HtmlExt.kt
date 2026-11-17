package dev.ao3m3.core.data.parse

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Element.firstOf(selectors: List<String>): Element? =
    selectors.firstNotNullOfOrNull { select(it).firstOrNull() }

fun Element.textOf(selectors: List<String>): String? =
    firstOf(selectors)?.text()?.trim()?.takeIf { it.isNotEmpty() }

fun Element.htmlOf(selectors: List<String>): String? =
    firstOf(selectors)?.html()?.trim()?.takeIf { it.isNotEmpty() }

fun Elements.texts(): List<String> = map { it.text().trim() }.filter { it.isNotEmpty() }

/** "1,234" -> 1234, "12,345,678" -> 12345678. */
fun parseIntOrNull(raw: String?): Int? =
    raw?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }?.toIntOrNull()

/** AO3 renders chapter counts as "3/12", "12/?" or "1/1". */
fun parseChapterCounts(raw: String?): Pair<Int?, Int?> {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null to null
    val parts = text.split('/')
    val published = parseIntOrNull(parts.getOrNull(0))
    val total = parseIntOrNull(parts.getOrNull(1))
    return published to total
}

private val DATE_FORMATS = listOf(
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH),
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
)

/**
 * AO3 prints dates as "07 Mar 2024" (or with a time on some pages). Anything unparseable is
 * dropped rather than guessed, because a wrong date is worse than no date.
 */
fun parseAo3Date(raw: String?): Instant? {
    val text = raw?.trim()?.replace(Regex("\\s+"), " ") ?: return null
    if (text.isEmpty()) return null
    val cleaned = text.substringBefore(" (").trim()
    DATE_FORMATS.forEach { format ->
        runCatching {
            return LocalDate.parse(cleaned, format).atStartOfDay(ZoneId.systemDefault()).toInstant()
        }.onSuccess { return it }
        runCatching {
            val parsed = java.time.LocalDateTime.parse(cleaned, format)
            return parsed.atZone(ZoneId.systemDefault()).toInstant()
        }
    }
    return null
}

/**
 * AO3 titles can legitimately contain parentheses, so only a trailing chapter marker such as
 * " (3/12)" is removed — never "Song (Reprise)".
 */
fun stripChapterSuffix(title: String): String =
    title.replace(Regex("\\s*\\(\\d+\\s*/\\s*[\\d?]*\\)\\s*$"), "").trim()

/** Finds the "/works/123" id in a link inside [element]. */
fun workIdFrom(element: Element): Long? {
    val href = element.select("a[href*=/works/]").firstOrNull()?.attr("href")
        ?: element.attr("href")
    return href?.let { Regex("/works/(\\d+)").find(it)?.groupValues?.getOrNull(1)?.toLongOrNull() }
}

fun Document.baseTitle(): String =
    select("title").firstOrNull()?.text()?.substringBefore("|")?.trim().orEmpty()
