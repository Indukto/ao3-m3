package dev.ao3m3.core.data.repo

import dev.ao3m3.core.data.actions.WorkActions
import dev.ao3m3.core.data.db.Ao3Dao
import dev.ao3m3.core.data.db.LibraryEntity
import dev.ao3m3.core.data.db.toEntity
import dev.ao3m3.core.data.db.toModel
import dev.ao3m3.core.data.net.Ao3Client
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.net.Ao3Urls
import dev.ao3m3.core.data.parse.LibraryParser
import dev.ao3m3.core.data.prefs.UserPrefs
import dev.ao3m3.core.model.LibraryKind
import dev.ao3m3.core.model.Work
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in user's library. Lists are cached in Room so the library tab works offline; a
 * refresh replaces page one and keeps older cached rows so nothing disappears during maintenance.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val client: Ao3Client,
    private val dao: Ao3Dao,
    private val prefs: UserPrefs,
    private val actions: WorkActions,
) {

    fun observe(kind: LibraryKind): Flow<List<Work>> =
        dao.observeLibrary(kind).map { works -> works.map { it.toModel() } }

    fun observeMembership(workId: Long, kind: LibraryKind): Flow<Boolean> =
        dao.observeLibraryEntry(workId, kind).map { it != null }

    suspend fun isIn(workId: Long, kind: LibraryKind): Boolean =
        dao.libraryEntry(workId, kind) != null

    /**
     * Adds or removes [kind] locally first, then mirrors the change to AO3. A failed remote call
     * rolls the local state back so the UI never lies about what is stored on the archive.
     */
    suspend fun toggle(workId: Long, kind: LibraryKind): Result<Boolean> {
        val existing = dao.libraryEntry(workId, kind)
        if (existing != null) {
            dao.deleteLibrary(workId, kind)
            val remote = remoteToggle(workId, kind, on = false)
            if (remote.isFailure) {
                dao.upsertLibrary(existing)
                return Result.failure(remote.exceptionOrNull()!!)
            }
            return Result.success(false)
        }

        dao.upsertLibrary(LibraryEntity(workId, kind, System.currentTimeMillis(), pendingSync = true))
        val remote = remoteToggle(workId, kind, on = true)
        if (remote.isFailure) {
            dao.deleteLibrary(workId, kind)
            return Result.failure(remote.exceptionOrNull()!!)
        }
        dao.upsertLibrary(LibraryEntity(workId, kind, System.currentTimeMillis(), pendingSync = false))
        return Result.success(true)
    }

    /**
     * History is this device's own record of works you opened — it is not an AO3 list, so it never
     * touches the network and works signed out.
     */
    suspend fun recordHistory(workId: Long) {
        if (dao.work(workId) == null) return
        dao.upsertLibrary(
            LibraryEntity(
                workId = workId,
                kind = LibraryKind.HISTORY,
                addedAt = System.currentTimeMillis(),
                pendingSync = false,
            ),
        )
        dao.trimLibrary(LibraryKind.HISTORY, MAX_HISTORY)
    }

    suspend fun refresh(kind: LibraryKind): Result<Unit> {
        if (kind == LibraryKind.HISTORY) return Result.success(Unit)
        return runCatching {
            val user = prefs.userName.first() ?: throw Ao3Exception.Unauthorized()
            val path = when (kind) {
                LibraryKind.BOOKMARKS -> Ao3Urls.userBookmarks(user)
                LibraryKind.MARKED_FOR_LATER -> Ao3Urls.userReadings(user)
                LibraryKind.SUBSCRIPTIONS -> Ao3Urls.userSubscriptions(user)
                LibraryKind.HISTORY -> return Result.success(Unit)
            }
            val query = if (kind == LibraryKind.MARKED_FOR_LATER) {
                listOf("show" to "to-read")
            } else {
                emptyList()
            }
            val document = client.getHtml(path, query)
            val works = LibraryParser.parse(document)
            dao.upsertWorks(works.map { it.toEntity() })
            val now = System.currentTimeMillis()
            works.forEachIndexed { index, work ->
                dao.upsertLibrary(
                    LibraryEntity(
                        workId = work.id,
                        kind = kind,
                        addedAt = now - index,
                        pendingSync = false,
                    ),
                )
            }
            // Page one is only the newest slice. Prune works that AO3 no longer lists only when
            // the whole library fits on this page; otherwise cached later pages would vanish.
            libraryEntriesToPrune(
                listed = works.map { it.id }.toSet(),
                cached = dao.libraryWorkIds(kind),
                morePagesExist = LibraryParser.hasNextPage(document),
            ).forEach { dao.deleteLibrary(it, kind) }
        }
    }

    private companion object {
        /** Long enough to be useful, short enough that the tab stays fast. */
        const val MAX_HISTORY = 100
    }

    private suspend fun remoteToggle(
        workId: Long,
        kind: LibraryKind,
        on: Boolean,
    ): Result<Unit> {
        if (!client.hasSession()) {
            return Result.failure(Ao3Exception.Unauthorized())
        }
        return when (kind) {
            LibraryKind.MARKED_FOR_LATER -> actions.setMarkedForLater(workId, marked = on)
            LibraryKind.SUBSCRIPTIONS -> actions.setSubscribed(workId)
            LibraryKind.BOOKMARKS -> actions.setBookmarked(workId, bookmarked = on)
            LibraryKind.HISTORY -> Result.success(Unit)
        }
    }
}

/**
 * Decides which cached library rows a refresh may delete. Only a single-page library is complete
 * enough to prune against; when AO3 reports another page, entries from cached later pages must
 * survive even though page one did not mention them.
 */
internal fun libraryEntriesToPrune(
    listed: Set<Long>,
    cached: List<Long>,
    morePagesExist: Boolean,
): List<Long> = if (morePagesExist) emptyList() else cached.filterNot { it in listed }
