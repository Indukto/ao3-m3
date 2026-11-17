package dev.ao3m3.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkLinkTest {

    @Test
    fun `accepts a bare work id`() {
        assertEquals(1234567L, WorkLink.parseWorkId("1234567"))
        assertEquals(1234567L, WorkLink.parseWorkId("  1234567  "))
    }

    @Test
    fun `accepts a work url`() {
        assertEquals(
            1234567L,
            WorkLink.parseWorkId("https://archiveofourown.org/works/1234567"),
        )
    }

    @Test
    fun `accepts a chapter url and returns the work id`() {
        assertEquals(
            1234567L,
            WorkLink.parseWorkId("https://archiveofourown.org/works/1234567/chapters/7654321"),
        )
    }

    @Test
    fun `rejects text that is not a work`() {
        assertNull(WorkLink.parseWorkId(""))
        assertNull(WorkLink.parseWorkId("   "))
        assertNull(WorkLink.parseWorkId("https://archiveofourown.org/tags/Fluff"))
        assertNull(WorkLink.parseWorkId("hello"))
        assertNull(WorkLink.parseWorkId("0"))
    }
}
