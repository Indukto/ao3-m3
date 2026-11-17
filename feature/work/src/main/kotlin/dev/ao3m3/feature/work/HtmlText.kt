package dev.ao3m3.feature.work

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

/**
 * Converts AO3 chapter HTML to styled text. We deliberately do not use a WebView: native text gives
 * proper typography, selection, font scaling and dark mode for free, and keeps rendering identical
 * across devices.
 */
fun renderChapterHtml(
    html: String,
    linkColor: Color,
    bodyColor: Color,
): AnnotatedString {
    val document = Ksoup.parse(html)
    val root: Node = document.select("body").firstOrNull() ?: document
    return buildAnnotatedString {
        root.childNodes().forEach { node ->
            appendNode(node, linkColor, bodyColor, this)
        }
    }.trimTrailingNewlines()
}

/**
 * Splits a rendered chapter into paragraph-sized blocks so the reader can virtualise a long
 * chapter instead of laying out one enormous Text, and so scroll progress is meaningful.
 */
fun renderChapterBlocks(html: String, linkColor: Color, bodyColor: Color): List<AnnotatedString> {
    val full = renderChapterHtml(html, linkColor, bodyColor)
    val blocks = mutableListOf<AnnotatedString>()
    var start = 0
    var index = 0
    while (index < full.length) {
        if (full[index] == '\n' && index + 1 < full.length && full[index + 1] == '\n') {
            if (index > start) blocks += full.subSequence(start, index)
            start = index + 2
            index += 2
        } else {
            index++
        }
    }
    if (start < full.length) blocks += full.subSequence(start, full.length)
    return blocks.filter { it.text.isNotBlank() }
}

private fun appendNode(
    node: Node,
    linkColor: Color,
    bodyColor: Color,
    builder: AnnotatedString.Builder,
) {
    when (node) {
        is TextNode -> builder.append(node.text().replace(Regex("\\s+"), " "))
        is Element -> appendElement(node, linkColor, bodyColor, builder)
    }
}

private fun appendElement(
    element: Element,
    linkColor: Color,
    bodyColor: Color,
    builder: AnnotatedString.Builder,
) {
    val tag = element.tagName().lowercase()
    when (tag) {
        "br" -> builder.append("\n")
        "hr" -> builder.append("\n———\n")
        "p" -> {
            element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
            builder.append("\n\n")
        }

        "em", "i", "cite" -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
        }

        "strong", "b" -> builder.withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
        }

        "a" -> {
            val href = element.attr("href")
            val absolute = absolutize(href)
            if (absolute != null) {
                builder.withLink(LinkAnnotation.Url(absolute)) {
                    withStyle(SpanStyle(color = linkColor)) {
                        element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
                    }
                }
            } else {
                element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
            }
        }

        "blockquote" -> {
            builder.append("\n")
            element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
            builder.append("\n")
        }

        "ul", "ol" -> {
            element.children().forEach { child ->
                if (child is Element && child.tagName().equals("li", ignoreCase = true)) {
                    builder.append("•  ")
                    child.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
                    builder.append("\n")
                }
            }
            builder.append("\n")
        }

        "li" -> {
            builder.append("•  ")
            element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
            builder.append("\n")
        }

        "h1", "h2", "h3", "h4", "h5", "h6" -> {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
            }
            builder.append("\n\n")
        }

        "sup", "sub" -> element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }

        else -> {
            // div, span, section, figure: transparent containers.
            element.childNodes().forEach { appendNode(it, linkColor, bodyColor, builder) }
            if (tag == "div") builder.append("\n")
        }
    }
}

private fun absolutize(href: String): String? {
    if (href.isBlank()) return null
    return when {
        href.startsWith("http://") || href.startsWith("https://") -> href
        href.startsWith("/") -> "https://archiveofourown.org$href"
        href.startsWith("#") -> null
        else -> "https://archiveofourown.org/$href"
    }
}

private fun AnnotatedString.trimTrailingNewlines(): AnnotatedString {
    var end = length
    while (end > 0 && this[end - 1] == '\n') end--
    return subSequence(0, end)
}
