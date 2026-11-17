package dev.ao3m3.core.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import dev.ao3m3.core.model.LibraryKind

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val authors: List<String>,
    val summary: String,
    val rating: String?,
    val warnings: List<String>,
    val categories: List<String>,
    val fandoms: List<String>,
    val relationships: List<String>,
    val characters: List<String>,
    val additionalTags: List<String>,
    val words: Int?,
    val chaptersPublished: Int?,
    val chaptersTotal: Int?,
    val kudos: Int?,
    val hits: Int?,
    val bookmarks: Int?,
    val commentCount: Int?,
    val language: String?,
    val publishedAt: Long?,
    val updatedAt: Long?,
    val cachedAt: Long,
)

@Entity(tableName = "chapters", primaryKeys = ["workId", "chapterId"])
data class ChapterEntity(
    val workId: Long,
    val chapterId: Long,
    val position: Int,
    val title: String?,
    val publishedAt: Long?,
)

@Entity(tableName = "chapter_texts")
data class ChapterTextEntity(
    @PrimaryKey val chapterId: Long,
    val workId: Long,
    val position: Int,
    val title: String?,
    val html: String,
    val notes: String?,
    val endNotes: String?,
    val fetchedAt: Long,
)

/** One cached page of a browse/search result set. */
@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val cacheKey: String,
    val fetchedAt: Long,
    val hasNext: Boolean,
)

@Entity(tableName = "page_items", primaryKeys = ["cacheKey", "position"])
data class PageItemEntity(
    val cacheKey: String,
    val position: Int,
    val workId: Long,
)

@Entity(tableName = "library", primaryKeys = ["workId", "kind"])
data class LibraryEntity(
    val workId: Long,
    val kind: LibraryKind,
    val addedAt: Long,
    val pendingSync: Boolean,
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val workId: Long,
    val chapterId: Long,
    val chapterIndex: Int,
    val scrollFraction: Float,
    val updatedAt: Long,
)

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workId: Long,
    val kind: LibraryKind,
    val createdAt: Long,
)

/** Progress joined with its work, for the resume-reading row. */
data class ProgressRow(
    @Embedded val progress: ProgressEntity,
    @Relation(parentColumn = "workId", entityColumn = "id")
    val work: WorkEntity?,
)
