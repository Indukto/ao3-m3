package dev.ao3m3.core.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Refreshing a library must never delete cached entries that simply live on a later page. */
class LibraryPruneTest {

    @Test
    fun `stale page-one entries are pruned when the library fits on one page`() {
        val pruned = libraryEntriesToPrune(
            listed = setOf(1L, 2L),
            cached = listOf(1L, 2L, 3L),
            morePagesExist = false,
        )

        assertEquals(listOf(3L), pruned)
    }

    @Test
    fun `cached entries survive while AO3 reports more pages`() {
        val pruned = libraryEntriesToPrune(
            listed = setOf(1L),
            cached = listOf(1L, 2L, 3L),
            morePagesExist = true,
        )

        assertTrue(pruned.isEmpty())
    }

    @Test
    fun `nothing is pruned when every cached entry is still listed`() {
        val pruned = libraryEntriesToPrune(
            listed = setOf(1L, 2L),
            cached = listOf(1L, 2L),
            morePagesExist = false,
        )

        assertTrue(pruned.isEmpty())
    }
}
