package dev.ao3m3.feature.work

import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.ReadingProgress

/** Which chapter to open and how far down it to restore, both derived without any Android types. */
data class ReadingStart(val index: Int, val resumeFraction: Float)

/**
 * Resolves where reading should begin:
 * 1. an explicit chapter (from a "read chapter 3" tap or the resume row),
 * 2. otherwise the chapter saved in reading progress,
 * 3. otherwise the first chapter.
 *
 * The saved scroll fraction is only restored when the resolved chapter is the one it belongs to;
 * jumping into a different chapter always starts at the top.
 */
fun resolveReadingStart(
    chapters: List<Chapter>,
    startChapterId: Long,
    progress: ReadingProgress?,
): ReadingStart {
    if (chapters.isEmpty()) return ReadingStart(index = -1, resumeFraction = 0f)

    val targetId = when {
        startChapterId > 0L -> startChapterId
        progress != null -> progress.chapterId
        else -> chapters.first().id
    }
    val found = chapters.indexOfFirst { it.id == targetId }
    val index = if (found >= 0) found else 0
    val chapter = chapters[index]
    val fraction = if (progress != null && progress.chapterId == chapter.id) {
        clampFraction(progress.scrollFraction)
    } else {
        0f
    }
    return ReadingStart(index = index, resumeFraction = fraction)
}

/** Keeps a scroll fraction usable even if a stored value is corrupt. */
fun clampFraction(value: Float): Float =
    if (value.isNaN()) 0f else value.coerceIn(0f, 1f)
