package dev.ao3m3.core.model

/**
 * Parses what a reader is likely to paste: a bare work id, an AO3 work URL, or a chapter URL.
 * Kept free of Android types so it can be unit tested.
 */
object WorkLink {

    private val WORK_PATH = Regex("""/works/(\d+)""", RegexOption.IGNORE_CASE)

    fun parseWorkId(input: String): Long? {
        val text = input.trim()
        if (text.isEmpty()) return null

        WORK_PATH.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }

        if (text.all { it.isDigit() }) {
            return text.toLongOrNull()?.takeIf { it > 0 }
        }
        return null
    }
}
