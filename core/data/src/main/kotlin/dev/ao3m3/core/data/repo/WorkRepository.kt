package dev.ao3m3.core.data.repo

import androidx.paging.PagingSource
import dev.ao3m3.core.data.db.Ao3Dao
import dev.ao3m3.core.data.db.PageEntity
import dev.ao3m3.core.data.db.PageItemEntity
import dev.ao3m3.core.data.db.toEntity
import dev.ao3m3.core.data.db.toModel
import dev.ao3m3.core.data.net.Ao3Client
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.net.Ao3Urls
import dev.ao3m3.core.data.parse.BlurbParser
import dev.ao3m3.core.data.parse.WorkParser
import dev.ao3m3.core.data.paging.BrowsePagingSource
import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.ChapterText
import dev.ao3m3.core.model.ProgressWithWork
import dev.ao3m3.core.model.ReadingProgress
import dev.ao3m3.core.model.Work
import dev.ao3m3.core.model.WorkQuery
import dev.ao3m3.core.model.WorkWithChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class AutocompleteTag(val id: String = "", val name: String = "")

private val suggestionJson = Json { ignoreUnknownKeys = true }
private const val MIN_SUGGESTION_CHARS = 3
private const val MAX_SUGGESTIONS = 8
private const val MAX_CACHED_TERMS = 60

data class PageResult(
    val works: List<Work>,
    val hasNext: Boolean,
    val fromCache: Boolean,
    val failure: Ao3Exception? = null,
)

/**
 * Everything the read path needs. Reads are cache-first: whatever is on disk is returned
 * immediately, and network refreshes replace it when they succeed. A maintenance response never
 * destroys cached content — it is reported to the caller so the UI can say so quietly.
 */
@Singleton
class WorkRepository @Inject constructor(
    private val client: Ao3Client,
    private val dao: Ao3Dao,
) {
    private val suggestionCache = ConcurrentHashMap<String, List<String>>()

    fun observeWork(id: Long): Flow<Work?> =
        dao.observeWork(id).map { entity -> entity?.toModel() }

    suspend fun cachedWork(id: Long): Work? = dao.work(id)?.toModel()

    suspend fun cachedChapters(workId: Long): List<Chapter> =
        dao.chapters(workId).map { it.toModel() }

    suspend fun cachedChapterText(chapterId: Long): ChapterText? =
        dao.chapterText(chapterId)?.toModel()

    /**
     * Fandom suggestions from AO3's tag autocomplete. The caller debounces; results are cached per
     * term because every request costs the client's politeness gap.
     */
    suspend fun fandomSuggestions(term: String): List<String> {
        val value = term.trim()
        if (value.length < MIN_SUGGESTION_CHARS) return emptyList()
        val key = value.lowercase()
        suggestionCache[key]?.let { return it }
        val names = runCatching {
            val body = client.getText(
                Ao3Urls.autocomplete("fandom"),
                query = listOf("term" to value),
                accept = Ao3Urls.JSON_ACCEPT,
            )
            withContext(Dispatchers.Default) {
                suggestionJson.decodeFromString<List<AutocompleteTag>>(body)
                    .map { it.name }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(MAX_SUGGESTIONS)
            }
        }.getOrDefault(emptyList())
        if (names.isNotEmpty()) {
            if (suggestionCache.size > MAX_CACHED_TERMS) suggestionCache.clear()
            suggestionCache[key] = names
        }
        return names
    }

    suspend fun refreshWork(workId: Long): Result<WorkWithChapters> = runCatching {
        val document = client.getHtml(Ao3Urls.workPage(workId))
        val parsed = withContext(Dispatchers.Default) { WorkParser.parse(document, workId) }
        val chapters = if (parsed.hasUsableIndex()) {
            parsed.chapters
        } else {
            chapterIndex(workId) ?: parsed.chapters
        }
        dao.upsertWork(parsed.work.toEntity())
        dao.upsertChapters(chapters.map { it.toEntity() })
        parsed.copy(chapters = chapters)
    }

    /**
     * A chaptered work whose page showed only the invented chapter-1 row still needs its real
     * index; AO3 serves that on its own page at /works/:id/navigate.
     */
    private fun WorkWithChapters.hasUsableIndex(): Boolean =
        chapters.isNotEmpty() && !(
            (work.chaptersTotal ?: 1) > 1 &&
                chapters.size == 1 &&
                chapters.single().id == work.id
            )

    private suspend fun chapterIndex(workId: Long): List<Chapter>? = runCatching {
        val document = client.getHtml(Ao3Urls.navigate(workId))
        withContext(Dispatchers.Default) { WorkParser.parseChapterIndex(document, workId) }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    /**
     * Loads one chapter's text, from disk when possible. One-shots live on the work page itself,
     * which is why the chapter id doubles as the work id in that case.
     */
    suspend fun loadChapterText(
        workId: Long,
        chapter: Chapter,
        force: Boolean = false,
    ): Result<ChapterText> = runCatching {
        if (!force) {
            dao.chapterText(chapter.id)?.let { return@runCatching it.toModel() }
        }
        val path = if (chapter.id == workId) {
            Ao3Urls.workPage(workId)
        } else {
            Ao3Urls.chapter(workId, chapter.id)
        }
        val document = client.getHtml(path)
        val text = WorkParser.parseChapterText(
            doc = document,
            chapterId = chapter.id,
            workId = workId,
            index = chapter.index,
            title = chapter.title,
        )
        dao.upsertChapterText(text.toEntity())
        text
    }

    fun browse(query: WorkQuery): PagingSource<Int, Work> = BrowsePagingSource(this, query)

    /** A previously cached page, so a list can paint before the archive answers. */
    suspend fun cachedPage(query: WorkQuery): List<Work> =
        dao.cachedPage(query.cacheKey()).map { it.toModel() }

    suspend fun loadPage(query: WorkQuery): PageResult {
        val cacheKey = query.cacheKey()
        return try {
            val params = searchParams(query)
            val document = when {
                query.isTagPage -> client.getHtml(Ao3Urls.tagWorks(query.tag!!), params)
                query.useSearchEndpoint -> client.getHtml(Ao3Urls.search(), params)
                else -> client.getHtml(Ao3Urls.browse(), params)
            }
            val (works, hasNext) = withContext(Dispatchers.Default) {
                BlurbParser.parse(document) to BlurbParser.hasNextPage(document)
            }
            dao.upsertWorks(works.map { it.toEntity() })
            dao.cachePage(
                page = PageEntity(cacheKey, System.currentTimeMillis(), hasNext),
                items = works.mapIndexed { index, work ->
                    PageItemEntity(cacheKey, index, work.id)
                },
            )
            PageResult(works, hasNext, fromCache = false)
        } catch (failure: Ao3Exception) {
            val cached = dao.cachedPage(cacheKey).map { it.toModel() }
            if (cached.isEmpty()) throw failure
            PageResult(
                works = cached,
                hasNext = dao.page(cacheKey)?.hasNext ?: false,
                fromCache = true,
                failure = failure,
            )
        }
    }

    /** Works with reading progress, most recent first. Local only, so it needs no account. */
    fun observeReading(limit: Int = 50): Flow<List<ProgressWithWork>> =
        dao.observeRecentProgress(limit).map { rows ->
            rows.mapNotNull { row ->
                row.work?.let { ProgressWithWork(row.progress.toModel(), it.toModel()) }
            }
        }

    suspend fun recentProgress(limit: Int = 6): List<ProgressWithWork> =
        dao.recentProgressRows(limit).mapNotNull { row ->
            row.work?.let { ProgressWithWork(row.progress.toModel(), it.toModel()) }
        }

    suspend fun progressFor(workId: Long): ReadingProgress? =
        dao.progress(workId)?.toModel()

    suspend fun saveProgress(progress: ReadingProgress) {
        dao.upsertProgress(progress.toEntity())
    }

    suspend fun cacheSizeBytes(): Long = dao.cachedTextBytes() ?: 0L

    suspend fun clearCachedText() {
        dao.clearChapterTexts()
        dao.clearPages()
        dao.clearPageItems()
    }

    private fun searchParams(query: WorkQuery): List<Pair<String, String>> = buildList {
        if (query.query.isNotBlank()) add("work_search[query]" to query.query)
        if (query.sort.param.isNotBlank()) {
            add("work_search[sort_column]" to query.sort.param)
        }
        query.filters.fandom.takeIf { it.isNotBlank() }?.let {
            add("work_search[fandom_names]" to it)
        }
        query.filters.relationship.takeIf { it.isNotBlank() }?.let {
            add("work_search[relationship_names]" to it)
        }
        query.filters.character.takeIf { it.isNotBlank() }?.let {
            add("work_search[character_names]" to it)
        }
        query.filters.otherTags.takeIf { it.isNotBlank() }?.let {
            add("work_search[other_tag_names]" to it)
        }
        query.filters.rating.forEach { add("work_search[rating_ids][]" to it) }
        query.filters.warnings.forEach { add("work_search[archive_warning_ids][]" to it) }
        query.filters.categories.forEach { add("work_search[category_ids][]" to it) }
        wordCountParam(query.filters.minWords, query.filters.maxWords)?.let {
            add("work_search[word_count]" to it)
        }
        if (query.filters.singleChapterOnly) add("work_search[chapters]" to "1")
        if (query.filters.completeOnly) add("work_search[complete]" to "T")
        if (query.filters.excludeCrossovers) add("work_search[crossover]" to "F")
        if (query.page > 1) add("page" to query.page.toString())
    }

    /** AO3's word_count field accepts ">10000", "<5000" and "1000-5000". */
    private fun wordCountParam(min: Int?, max: Int?): String? = when {
        min != null && max != null -> "$min-$max"
        min != null -> ">$min"
        max != null -> "<$max"
        else -> null
    }
}
