package dev.ao3m3.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.Work
import dev.ao3m3.core.model.WorkQuery

/**
 * Pages through AO3 results one request at a time. The repository already enforces a single
 * in-flight request and a minimum gap, so this source never fans out — it just maps page keys.
 */
class BrowsePagingSource(
    private val repository: WorkRepository,
    private val query: WorkQuery,
) : PagingSource<Int, Work>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Work> {
        val page = params.key ?: 1
        return try {
            val result = repository.loadPage(query.withPage(page))
            LoadResult.Page(
                data = result.works,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (result.hasNext) page + 1 else null,
            )
        } catch (failure: Ao3Exception) {
            LoadResult.Error(failure)
        } catch (other: Exception) {
            LoadResult.Error(other)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Work>): Int? = null
}
