package dev.ao3m3.core.data.parse

import com.fleeksoft.ksoup.Ksoup
import dev.ao3m3.core.data.actions.FormScraper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests run against fixtures captured from AO3's markup structure. They are the safety net
 * for scraping: when a selector regresses, these fail before the app does.
 */
class ParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "missing fixture $name"
        }.bufferedReader().readText()

    @Test
    fun `parses blurbs with stats and tags`() {
        val works = BlurbParser.parse(Ksoup.parse(fixture("browse.html")))

        assertEquals(2, works.size)
        val first = works.first()
        assertEquals(1234567L, first.id)
        assertEquals("A Long Winter of Small Kindnesses", first.title)
        assertEquals(listOf("stormandsky"), first.authors)
        assertEquals("Teen And Up Audiences", first.rating)
        assertEquals(listOf("F/M"), first.categories)
        assertEquals(12345, first.words)
        assertEquals(3, first.chaptersPublished)
        assertEquals(12, first.chaptersTotal)
        assertEquals(1203, first.kudos)
        assertEquals(45678, first.hits)
        assertEquals(88, first.bookmarks)
        assertEquals("English", first.language)
        assertEquals(listOf("A/B", "B/C"), first.relationships)
        assertEquals(listOf("Fluff", "Snowed In", "Slow Burn"), first.additionalTags)
        assertEquals(listOf("Original Work"), first.fandoms)
        assertTrue(first.summary.contains("Snowed in"))
        assertEquals("3/12", first.chapterLabel)
        assertTrue(!first.isComplete)
    }

    @Test
    fun `detects the next page link`() {
        val doc = Ksoup.parse(fixture("browse.html"))
        assertTrue(BlurbParser.hasNextPage(doc))
    }

    @Test
    fun `parses a one shot as complete without a chapter label`() {
        val works = BlurbParser.parse(Ksoup.parse(fixture("browse.html")))
        val oneShot = works.last()
        assertEquals(1, oneShot.chaptersTotal)
        assertNull(oneShot.chapterLabel)
        assertTrue(oneShot.isComplete)
    }

    @Test
    fun `parses work page metadata and table of contents`() {
        val parsed = WorkParser.parse(Ksoup.parse(fixture("work.html")), 1234567L)

        assertEquals("A Long Winter of Small Kindnesses", parsed.work.title)
        assertEquals(listOf("stormandsky"), parsed.work.authors)
        assertEquals("Teen And Up Audiences", parsed.work.rating)
        assertEquals(listOf("Original Work"), parsed.work.fandoms)
        assertEquals(2, parsed.chapters.size)
        assertEquals(111111L, parsed.chapters.first().id)
        assertEquals("The Cottage", parsed.chapters.first().title)
        assertEquals(1, parsed.chapters.first().index)
        assertEquals("2/12", parsed.work.chapterLabel)
    }

    @Test
    fun `parses chapter html preserving emphasis and structure`() {
        val text = WorkParser.parseChapterText(
            doc = Ksoup.parse(fixture("work.html")),
            chapterId = 111111L,
            workId = 1234567L,
            index = 1,
            title = "The Cottage",
        )

        assertTrue(text.html.contains("<em>Snow</em>"))
        assertTrue(text.html.contains("<blockquote>"))
        assertTrue(text.html.contains("<li>Tea</li>"))
        assertEquals(1, text.index)
    }

    @Test
    fun `finds the mark for later form and its token`() {
        val form = FormScraper.find(Ksoup.parse(fixture("work.html")), "/mark_for_later")

        assertTrue(form != null)
        assertEquals("/works/1234567/mark_for_later", form!!.action)
        assertEquals("token-abc123", form.fields["authenticity_token"])
    }

    @Test
    fun `parses chapter counts and dates defensively`() {
        assertEquals(3 to 12, parseChapterCounts("3/12"))
        assertEquals(12 to null, parseChapterCounts("12/?"))
        assertEquals(null to null, parseChapterCounts(""))
        assertEquals(1234, parseIntOrNull("1,234"))
        assertNull(parseIntOrNull("-"))
        assertEquals(2024, parseAo3Date("07 Mar 2024")?.atZone(java.time.ZoneId.systemDefault())?.year)
        assertNull(parseAo3Date("not a date"))
    }
}
