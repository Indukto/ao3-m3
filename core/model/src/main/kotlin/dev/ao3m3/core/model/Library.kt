package dev.ao3m3.core.model

/** The four read-only library surfaces AO3 exposes to a signed-in user. */
enum class LibraryKind {
    BOOKMARKS,
    MARKED_FOR_LATER,
    SUBSCRIPTIONS,
    HISTORY,
}

/**
 * A library membership stored locally. [remote] entries exist on AO3; [localOnly] marks ones
 * queued while offline (AO3 in maintenance) that still need to be pushed.
 */
data class LibraryEntry(
    val workId: Long,
    val kind: LibraryKind,
    val addedAt: Long = System.currentTimeMillis(),
    val note: String? = null,
    val pendingSync: Boolean = false,
)

/** A single change to a work's library state that could not reach AO3 yet. */
data class PendingAction(
    val id: Long = 0,
    val workId: Long,
    val kind: LibraryKind,
    val subscribe: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)
