package dev.ao3m3.feature.work

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader's renderer is the one piece of UI logic that can be proven without a device, so it is
 * covered directly: markup in, styled text out. These assertions are what "it renders AO3 HTML"
 * means for this app.
 */
class ChapterRendererTest {

    private val linkColor = Color(0xFF990000)
    private val bodyColor = Color(0xFF1B1B1B)

    private fun render(html: String) = renderChapterHtml(html, linkColor, bodyColor)

    private fun blocks(html: String) = renderChapterBlocks(html, linkColor, bodyColor)

    @Test
    fun `paragraphs become separate blocks with visible text`() {
        val blocks = blocks("<p>First para.</p><p>Second <em>para</em>.</p>")

        assertEquals(2, blocks.size)
        assertEquals("First para.", blocks[0].text)
        assertEquals("Second para.", blocks[1].text)
    }

    @Test
    fun `no markup ever leaks into the rendered text`() {
        val rendered = render(
            "<div class=\"userstuff\"><p>Hello <strong>world</strong> " +
                "<a href=\"/works/42\">link</a></p></div>",
        )

        assertFalse(rendered.text.contains("<"))
        assertFalse(rendered.text.contains("&"))
        assertTrue(rendered.text.contains("Hello world link"))
    }

    @Test
    fun `emphasis and strength become spans covering only their own words`() {
        val rendered = render("<p>A <em>quiet</em> and <strong>loud</strong> line.</p>")

        val italic = rendered.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        val bold = rendered.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }

        assertEquals(1, italic.size)
        assertEquals("quiet", rendered.text.substring(italic.single().start, italic.single().end))
        assertEquals(1, bold.size)
        assertEquals("loud", rendered.text.substring(bold.single().start, bold.single().end))
    }

    @Test
    fun `relative links are absolutised and anchors are not links`() {
        val linked = render("<p><a href=\"/works/42\">read it</a></p>")
        val linkAnnotations = linked.getLinkAnnotations(0, linked.length)

        assertEquals(1, linkAnnotations.size)
        val url = linkAnnotations.single().item as LinkAnnotation.Url
        assertEquals("https://archiveofourown.org/works/42", url.url)

        val anchor = render("<p><a href=\"#end\">jump</a></p>")
        assertEquals("jump", anchor.text)
        assertTrue(anchor.getLinkAnnotations(0, anchor.length).isEmpty())
    }

    @Test
    fun `blockquotes preserve their text`() {
        val rendered = render("<blockquote><p>Some quotes are worth keeping.</p></blockquote>")

        assertEquals("Some quotes are worth keeping.", rendered.text.trim())
    }

    @Test
    fun `horizontal rules are rendered as a visual break`() {
        val rendered = render("<p>Before</p><hr/><p>After</p>")

        assertTrue(rendered.text.contains("———"))
        assertTrue(rendered.text.indexOf("Before") < rendered.text.indexOf("———"))
    }

    @Test
    fun `unordered lists keep every item with a bullet`() {
        val rendered = render("<ul><li>Tea</li><li>Firewood</li></ul>")

        assertTrue(rendered.text.contains("•  Tea"))
        assertTrue(rendered.text.contains("•  Firewood"))
    }

    @Test
    fun `headings are bold and keep their text`() {
        val rendered = render("<h2>Chapter two</h2>")
        val bold = rendered.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }

        assertEquals("Chapter two", rendered.text.trim())
        assertEquals(1, bold.size)
    }

    @Test
    fun `line breaks inside a paragraph survive`() {
        val rendered = render("<p>Snow<br/>and nowhere to be.</p>")

        assertTrue(rendered.text.contains("Snow\nand nowhere to be."))
    }

    @Test
    fun `notes markup renders as text not tags`() {
        // The reader shows chapter notes through the same renderer; AO3 wraps them in a blockquote.
        val notes = render(
            "<blockquote class=\"userstuff\"><p>Thanks to the <em>beta</em> " +
                "and the <a href=\"/users/beta\">beta reader</a>.</p></blockquote>",
        )

        assertEquals("Thanks to the beta and the beta reader.", notes.text.trim())
        assertFalse(notes.text.contains("<"))
        assertFalse(notes.text.contains("blockquote"))
    }

    @Test
    fun `empty and whitespace-only chapters render nothing`() {
        assertTrue(blocks("").isEmpty())
        assertTrue(blocks("<p>   </p>").isEmpty())
        assertTrue(render("").text.isEmpty())
    }

    @Test
    fun `trailing blank lines do not create empty blocks`() {
        val blocks = blocks("<p>Only paragraph.</p><p></p><p></p>")

        assertEquals(1, blocks.size)
        assertEquals("Only paragraph.", blocks.single().text)
    }
}
