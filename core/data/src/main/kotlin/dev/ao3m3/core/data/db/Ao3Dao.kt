package dev.ao3m3.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.ao3m3.core.model.LibraryKind
import kotlinx.coroutines.flow.Flow

@Dao
interface Ao3Dao {

    // --- Works ---
    @Query("SELECT * FROM works WHERE id = :id")
    suspend fun work(id: Long): WorkEntity?

    @Query("SELECT * FROM works WHERE id = :id")
    fun observeWork(id: Long): Flow<WorkEntity?>

    @Upsert
    suspend fun upsertWorks(works: List<WorkEntity>)

    @Upsert
    suspend fun upsertWork(work: WorkEntity)

    @Query("SELECT COUNT(*) FROM works")
    suspend fun workCount(): Int

    // --- Chapters ---
    @Upsert
    suspend fun upsertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE workId = :workId ORDER BY position")
    suspend fun chapters(workId: Long): List<ChapterEntity>

    @Upsert
    suspend fun upsertChapterText(text: ChapterTextEntity)

    @Query("SELECT * FROM chapter_texts WHERE chapterId = :chapterId")
    suspend fun chapterText(chapterId: Long): ChapterTextEntity?

    @Query("SELECT * FROM chapter_texts WHERE workId = :workId ORDER BY position")
    suspend fun chapterTexts(workId: Long): List<ChapterTextEntity>

    // --- Cached pages ---
    @Upsert
    suspend fun upsertPage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageItems(items: List<PageItemEntity>)

    @Query("DELETE FROM page_items WHERE cacheKey = :cacheKey")
    suspend fun clearPageItems(cacheKey: String)

    @Transaction
    suspend fun cachePage(page: PageEntity, items: List<PageItemEntity>) {
        upsertPage(page)
        clearPageItems(page.cacheKey)
        insertPageItems(items)
    }

    @Query(
        """
        SELECT w.* FROM works w
        INNER JOIN page_items p ON w.id = p.workId
        WHERE p.cacheKey = :cacheKey
        ORDER BY p.position
        """,
    )
    suspend fun cachedPage(cacheKey: String): List<WorkEntity>

    @Query("SELECT * FROM pages WHERE cacheKey = :cacheKey")
    suspend fun page(cacheKey: String): PageEntity?

    // --- Library ---
    @Upsert
    suspend fun upsertLibrary(entry: LibraryEntity)

    @Query("DELETE FROM library WHERE workId = :workId AND kind = :kind")
    suspend fun deleteLibrary(workId: Long, kind: LibraryKind)

    @Query("SELECT * FROM library WHERE workId = :workId AND kind = :kind")
    suspend fun libraryEntry(workId: Long, kind: LibraryKind): LibraryEntity?

    @Query("SELECT * FROM library WHERE workId = :workId AND kind = :kind")
    fun observeLibraryEntry(workId: Long, kind: LibraryKind): Flow<LibraryEntity?>

    @Query("SELECT * FROM library WHERE kind = :kind")
    fun observeLibraryEntries(kind: LibraryKind): Flow<List<LibraryEntity>>

    @Query("SELECT workId FROM library WHERE kind = :kind")
    suspend fun libraryWorkIds(kind: LibraryKind): List<Long>

    @Query(
        """
        DELETE FROM library WHERE kind = :kind AND workId NOT IN (
            SELECT workId FROM library WHERE kind = :kind ORDER BY addedAt DESC LIMIT :keep
        )
        """,
    )
    suspend fun trimLibrary(kind: LibraryKind, keep: Int)

    @Query(
        """
        SELECT w.* FROM works w
        INNER JOIN library l ON w.id = l.workId
        WHERE l.kind = :kind
        ORDER BY l.addedAt DESC
        """,
    )
    fun observeLibrary(kind: LibraryKind): Flow<List<WorkEntity>>

    @Query(
        """
        SELECT w.* FROM works w
        INNER JOIN library l ON w.id = l.workId
        WHERE l.kind = :kind
        ORDER BY l.addedAt DESC
        """,
    )
    suspend fun library(kind: LibraryKind): List<WorkEntity>

    // --- Progress ---
    @Upsert
    suspend fun upsertProgress(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE workId = :workId")
    suspend fun progress(workId: Long): ProgressEntity?

    @Transaction
    @Query("SELECT * FROM progress ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentProgress(limit: Int): Flow<List<ProgressRow>>

    @Transaction
    @Query("SELECT * FROM progress ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun recentProgressRows(limit: Int): List<ProgressRow>

    // --- Pending offline actions ---
    @Insert
    suspend fun insertPending(action: PendingActionEntity): Long

    @Query("SELECT * FROM pending_actions ORDER BY id")
    suspend fun pendingActions(): List<PendingActionEntity>

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deletePending(id: Long)

    // --- Maintenance ---
    @Query("DELETE FROM chapter_texts")
    suspend fun clearChapterTexts()

    @Query("DELETE FROM page_items")
    suspend fun clearPageItems()

    @Query("DELETE FROM pages")
    suspend fun clearPages()

    @Query("SELECT SUM(LENGTH(html)) FROM chapter_texts")
    suspend fun cachedTextBytes(): Long?
}
