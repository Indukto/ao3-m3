package dev.ao3m3.core.model

import java.time.Instant

/**
 * A work as it appears in a blurb (browse/search/library lists) or, enriched with chapters,
 * on a work page. All fields except [id], [title] and [summary] are optional because AO3 markup
 * varies between list views and work pages.
 */
data class Work(
    val id: Long,
    val title: String,
    val authors: List<String> = emptyList(),
    val summary: String = "",
    val rating: String? = null,
    val warnings: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val fandoms: List<String> = emptyList(),
    val relationships: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val additionalTags: List<String> = emptyList(),
    val words: Int? = null,
    val chaptersPublished: Int? = null,
    val chaptersTotal: Int? = null,
    val kudos: Int? = null,
    val hits: Int? = null,
    val bookmarks: Int? = null,
    val commentCount: Int? = null,
    val language: String? = null,
    val published: Instant? = null,
    val updated: Instant? = null,
    val series: List<String> = emptyList(),
) {
    val hasMultipleChapters: Boolean get() = (chaptersTotal ?: 1) > 1

    val isComplete: Boolean
        get() {
            val total = chaptersTotal ?: return true
            val published = chaptersPublished ?: return false
            return published >= total
        }

    /** "3/12" for chaptered works, null for one-shots. */
    val chapterLabel: String?
        get() {
            if (!hasMultipleChapters) return null
            return "${chaptersPublished ?: "?"}/$chaptersTotal"
        }

    val authorLabel: String
        get() = if (authors.isEmpty()) "Anonymous" else authors.joinToString(", ")

    val wordCountLabel: String?
        get() = words?.let { "%,d words".format(it) }

    val tagCount: Int
        get() = warnings.size + relationships.size + characters.size + additionalTags.size
}

/** A chapter entry in a work's table of contents. */
data class Chapter(
    val id: Long,
    val workId: Long,
    val index: Int,
    val title: String?,
    val published: Instant? = null,
) {
    val displayTitle: String
        get() = if (title.isNullOrBlank()) "Chapter $index" else title
}

/** A work plus its table of contents, as loaded from a work page. */
data class WorkWithChapters(
    val work: Work,
    val chapters: List<Chapter> = emptyList(),
    val notes: String? = null,
    val endNotes: String? = null,
)

/** One chapter's HTML content, exactly as AO3 delivered it. */
data class ChapterText(
    val chapterId: Long,
    val workId: Long,
    val index: Int,
    val title: String?,
    val html: String,
    val notes: String? = null,
    val endNotes: String? = null,
)

/** The reader's position inside a work. */
data class ReadingProgress(
    val workId: Long,
    val chapterId: Long,
    val chapterIndex: Int,
    val scrollFraction: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Progress plus the title needed to render a "continue reading" row offline. */
data class ProgressWithWork(
    val progress: ReadingProgress,
    val work: Work,
)
