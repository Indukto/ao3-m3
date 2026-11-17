package dev.ao3m3.feature.work

import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.ReadingProgress
import org.junit.Assert.assertEquals
import org.junit.Test

/** Resume behaviour is pure logic, so it is asserted rather than eyeballed on a device. */
class ReadingPositionTest {

    private fun chapters(vararg ids: Long): List<Chapter> =
        ids.mapIndexed { position, id ->
            Chapter(id = id, workId = 99L, index = position + 1, title = "Chapter ${position + 1}")
        }

    private fun progress(chapterId: Long, index: Int, fraction: Float) =
        ReadingProgress(workId = 99L, chapterId = chapterId, chapterIndex = index, scrollFraction = fraction)

    @Test
    fun `an explicitly requested chapter wins over saved progress`() {
        val start = resolveReadingStart(
            chapters = chapters(11L, 22L, 33L),
            startChapterId = 33L,
            progress = progress(chapterId = 11L, index = 1, fraction = 0.6f),
        )

        assertEquals(2, start.index)
        assertEquals(0f, start.resumeFraction)
    }

    @Test
    fun `saved progress decides the chapter when none is requested`() {
        val start = resolveReadingStart(
            chapters = chapters(11L, 22L, 33L),
            startChapterId = 0L,
            progress = progress(chapterId = 22L, index = 2, fraction = 0.25f),
        )

        assertEquals(1, start.index)
        assertEquals(0.25f, start.resumeFraction)
    }

    @Test
    fun `the saved fraction is restored only for the chapter it belongs to`() {
        val sameChapter = resolveReadingStart(
            chapters = chapters(11L, 22L),
            startChapterId = 22L,
            progress = progress(chapterId = 22L, index = 2, fraction = 0.8f),
        )
        assertEquals(1, sameChapter.index)
        assertEquals(0.8f, sameChapter.resumeFraction)

        val otherChapter = resolveReadingStart(
            chapters = chapters(11L, 22L),
            startChapterId = 11L,
            progress = progress(chapterId = 22L, index = 2, fraction = 0.8f),
        )
        assertEquals(0, otherChapter.index)
        assertEquals(0f, otherChapter.resumeFraction)
    }

    @Test
    fun `first chapter is the default when nothing is saved`() {
        val start = resolveReadingStart(chapters(11L, 22L), startChapterId = 0L, progress = null)

        assertEquals(0, start.index)
        assertEquals(0f, start.resumeFraction)
    }

    @Test
    fun `an unknown chapter id falls back to the first chapter at the top`() {
        val start = resolveReadingStart(
            chapters = chapters(11L, 22L),
            startChapterId = 404L,
            progress = null,
        )

        assertEquals(0, start.index)
        assertEquals(0f, start.resumeFraction)
    }

    @Test
    fun `stored fractions are clamped and NaN becomes the top`() {
        val beyondEnd = resolveReadingStart(
            chapters = chapters(11L),
            startChapterId = 0L,
            progress = progress(chapterId = 11L, index = 1, fraction = 1.7f),
        )
        assertEquals(1f, beyondEnd.resumeFraction)

        val negative = resolveReadingStart(
            chapters = chapters(11L),
            startChapterId = 0L,
            progress = progress(chapterId = 11L, index = 1, fraction = -0.4f),
        )
        assertEquals(0f, negative.resumeFraction)

        assertEquals(0f, clampFraction(Float.NaN))
        assertEquals(0.5f, clampFraction(0.5f))
    }

    @Test
    fun `a work with no chapters resolves to no index`() {
        val start = resolveReadingStart(emptyList(), startChapterId = 5L, progress = null)

        assertEquals(-1, start.index)
        assertEquals(0f, start.resumeFraction)
    }
}
