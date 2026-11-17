package dev.ao3m3.core.data.actions

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element

/**
 * AO3's write actions (mark for later, subscribe, bookmark) are ordinary HTML forms protected by
 * an authenticity token. Rather than hard-coding every field — which breaks whenever AO3 adds or
 * renames one — we submit the form exactly as the browser would, with optional overrides.
 */
object FormScraper {

    data class Form(val action: String, val fields: Map<String, String>)

    fun find(doc: Document, actionContains: String): Form? {
        val form = doc.select("form").firstOrNull {
            it.attr("action").contains(actionContains, ignoreCase = true)
        } ?: return null

        val fields = linkedMapOf<String, String>()
        form.select("input[name]").forEach { input ->
            val name = input.attr("name")
            when (input.attr("type").lowercase()) {
                "submit", "button", "image", "file", "reset" -> Unit
                "checkbox", "radio" -> {
                    if (input.hasAttr("checked")) {
                        fields[name] = input.attr("value").ifEmpty { "1" }
                    }
                }

                else -> fields[name] = input.attr("value")
            }
        }
        form.select("select[name]").forEach { select ->
            val option = select.select("option[selected]").firstOrNull()
                ?: select.select("option").firstOrNull()
            option?.let { fields[select.attr("name")] = it.attr("value").ifEmpty { it.text() } }
        }
        form.select("textarea[name]").forEach { fields[it.attr("name")] = it.text() }

        return Form(form.attr("action"), fields)
    }

    /** True when the page contains a form that would submit to [actionContains]. */
    fun has(doc: Document, actionContains: String): Boolean =
        doc.select("form").any { it.attr("action").contains(actionContains, ignoreCase = true) }

    fun firstFormAction(doc: Document): String? =
        doc.select("form").mapNotNull { it.attr("action").takeIf(String::isNotBlank) }.firstOrNull()

    fun Element.fieldNames(): Set<String> =
        select("input[name], select[name], textarea[name]")
            .mapNotNull { it.attr("name").takeIf(String::isNotBlank) }
            .toSet()
}
