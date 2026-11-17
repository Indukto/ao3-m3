package dev.ao3m3.core.data.parse

import com.fleeksoft.ksoup.Ksoup
import dev.ao3m3.core.model.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chapter selection and the boundary between chapter text and notes are exactly the places where a
 * scraping client silently shows the wrong thing, so they are asserted here.
 */
class WorkParserChapterTest {

    private fun work(id: Long = 1234567L, title: String = "A Work") =
        Work(id = id, title = title)

    private fun doc(body: String) = Ksoup.parse(
        """
        <html><body>
        <div id="main">
          <h2 class="title">Story title</h2>
          <dl class="stats">
            <dd class="chapters">2/?</dd>
            <dd class="published">07 Mar 2024</dd>
          </dl>
          $body
        </div>
        </body></html>
        """.trimIndent(),
    )

    @Test
    fun `table of contents drives chapters in order`() {
        val parsed = WorkParser.parseChapters(
            doc(
                """
                <div id="chapters"><ol class="chapter index group">
                  <li><a href="/works/1234567/chapters/222">2. Snowfall</a></li>
                  <li><a href="/works/1234567/chapters/111">1. The Cottage</a></li>
                </ol></div>
                """.trimIndent(),
            ),
            work(),
        )

        assertEquals(listOf(111L, 222L), parsed.map { it.id })
        assertEquals(listOf(1, 2), parsed.map { it.index })
        assertEquals("The Cottage", parsed.first().title)
    }

    @Test
    fun `duplicate table of contents entries collapse to one chapter`() {
        val parsed = WorkParser.parseChapters(
            doc(
                """
                <div id="chapters"><ol class="chapter index group">
                  <li><a href="/works/1234567/chapters/111">1. The Cottage</a></li>
                  <li><a href="/works/1234567/chapters/111">1. The Cottage</a></li>
                </ol></div>
                """.trimIndent(),
            ),
            work(),
        )

        assertEquals(1, parsed.size)
        assertEquals(111L, parsed.single().id)
    }

    @Test
    fun `a one shot has a single chapter whose id is the work id`() {
        val parsed = WorkParser.parseChapters(doc(""), work(id = 555L, title = "One Shot"))

        assertEquals(1, parsed.size)
        assertEquals(555L, parsed.single().id)
        assertEquals(555L, parsed.single().workId)
        assertEquals(1, parsed.single().index)
        assertEquals("One Shot", parsed.single().title)
    }

    @Test
    fun `chapter title numbers are stripped but real text kept`() {
        val parsed = WorkParser.parseChapters(
            doc(
                """
                <div id="chapters"><ol class="chapter index group">
                  <li><a href="/works/1234567/chapters/111">1. Scene (Reprise)</a></li>
                </ol></div>
                """.trimIndent(),
            ),
            work(),
        )

        assertEquals("Scene (Reprise)", parsed.single().title)
    }

    @Test
    fun `chapter counts in the title are removed but real parentheses kept`() {
        assertEquals("A Long Winter", stripChapterSuffix("A Long Winter (3/12)"))
        assertEquals("A Long Winter", stripChapterSuffix("A Long Winter (12/?)"))
        assertEquals("Song (Reprise)", stripChapterSuffix("Song (Reprise)"))
        assertEquals("Untouched", stripChapterSuffix("Untouched"))
    }

    @Test
    fun `work page titles drop the chapter suffix`() {
        val parsed = WorkParser.parse(
            Ksoup.parse(
                """
                <html><body><div id="main">
                  <h2 class="title">A Long Winter (2/12)</h2>
                  <h3 class="byline"><a rel="author" href="/users/x">author</a></h3>
                  <dl class="stats"><dd class="chapters">2/12</dd></dl>
                </div></body></html>
                """.trimIndent(),
            ),
            1234567L,
        )

        assertEquals("A Long Winter", parsed.work.title)
        assertEquals(2, parsed.work.chaptersPublished)
        assertEquals(12, parsed.work.chaptersTotal)
    }

    @Test
    fun `chapter body excludes notes and end notes`() {
        val text = WorkParser.parseChapterText(
            doc = doc(
                """
                <div id="chapters">
                  <div class="chapter" id="chapter-111">
                    <div class="notes"><blockquote class="userstuff">
                      <p>Thanks <em>beta</em>.</p>
                    </blockquote></div>
                    <div class="userstuff module"><p>Body text.</p></div>
                    <div class="end notes"><blockquote class="userstuff">
                      <p>See you next time.</p>
                    </blockquote></div>
                  </div>
                </div>
                """.trimIndent(),
            ),
            chapterId = 111L,
            workId = 1234567L,
            index = 1,
            title = "The Cottage",
        )

        assertTrue(text.html.contains("Body text."))
        assertFalse(text.html.contains("Thanks"))
        assertFalse(text.html.contains("See you next time"))

        assertTrue(text.notes.orEmpty().contains("Thanks"))
        assertFalse(text.notes.orEmpty().contains("Body text."))
        assertTrue(text.endNotes.orEmpty().contains("See you next time"))
    }

    @Test
    fun `a page without a chapter body fails instead of returning empty text`() {
        val failure = runCatching {
            WorkParser.parseChapterText(
                doc = doc("<p>no body here</p>"),
                chapterId = 111L,
                workId = 1234567L,
                index = 1,
                title = null,
            )
        }.exceptionOrNull()

        assertTrue(failure is dev.ao3m3.core.data.net.Ao3Exception.Parse)
    }

    @Test
    fun `missing notes are null rather than empty strings`() {
        val text = WorkParser.parseChapterText(
            doc = doc(
                """
                <div id="chapters"><div class="chapter">
                  <div class="userstuff module"><p>Only body.</p></div>
                </div></div>
                """.trimIndent(),
            ),
            chapterId = 111L,
            workId = 1234567L,
            index = 1,
            title = null,
        )

        assertNull(text.notes)
        assertNull(text.endNotes)
    }
}
