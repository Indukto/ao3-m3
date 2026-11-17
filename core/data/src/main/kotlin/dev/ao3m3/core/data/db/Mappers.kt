package dev.ao3m3.core.data.db

import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.ChapterText
import dev.ao3m3.core.model.ReadingProgress
import dev.ao3m3.core.model.Work
import java.time.Instant

fun WorkEntity.toModel(): Work = Work(
    id = id,
    title = title,
    authors = authors,
    summary = summary,
    rating = rating,
    warnings = warnings,
    categories = categories,
    fandoms = fandoms,
    relationships = relationships,
    characters = characters,
    additionalTags = additionalTags,
    words = words,
    chaptersPublished = chaptersPublished,
    chaptersTotal = chaptersTotal,
    kudos = kudos,
    hits = hits,
    bookmarks = bookmarks,
    commentCount = commentCount,
    language = language,
    published = publishedAt?.let(Instant::ofEpochMilli),
    updated = updatedAt?.let(Instant::ofEpochMilli),
)

fun Work.toEntity(cachedAt: Long = System.currentTimeMillis()): WorkEntity = WorkEntity(
    id = id,
    title = title,
    authors = authors,
    summary = summary,
    rating = rating,
    warnings = warnings,
    categories = categories,
    fandoms = fandoms,
    relationships = relationships,
    characters = characters,
    additionalTags = additionalTags,
    words = words,
    chaptersPublished = chaptersPublished,
    chaptersTotal = chaptersTotal,
    kudos = kudos,
    hits = hits,
    bookmarks = bookmarks,
    commentCount = commentCount,
    language = language,
    publishedAt = published?.toEpochMilli(),
    updatedAt = updated?.toEpochMilli(),
    cachedAt = cachedAt,
)

fun ChapterEntity.toModel(): Chapter = Chapter(
    id = chapterId,
    workId = workId,
    index = position,
    title = title,
    published = publishedAt?.let(Instant::ofEpochMilli),
)

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    workId = workId,
    chapterId = id,
    position = index,
    title = title,
    publishedAt = published?.toEpochMilli(),
)

fun ChapterTextEntity.toModel(): ChapterText = ChapterText(
    chapterId = chapterId,
    workId = workId,
    index = position,
    title = title,
    html = html,
    notes = notes,
    endNotes = endNotes,
)

fun ChapterText.toEntity(): ChapterTextEntity = ChapterTextEntity(
    chapterId = chapterId,
    workId = workId,
    position = index,
    title = title,
    html = html,
    notes = notes,
    endNotes = endNotes,
    fetchedAt = System.currentTimeMillis(),
)

fun ProgressEntity.toModel(): ReadingProgress = ReadingProgress(
    workId = workId,
    chapterId = chapterId,
    chapterIndex = chapterIndex,
    scrollFraction = scrollFraction,
    updatedAt = updatedAt,
)

fun ReadingProgress.toEntity(): ProgressEntity = ProgressEntity(
    workId = workId,
    chapterId = chapterId,
    chapterIndex = chapterIndex,
    scrollFraction = scrollFraction,
    updatedAt = updatedAt,
)
