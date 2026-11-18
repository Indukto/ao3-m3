package dev.ao3m3.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.SearchFilters
import dev.ao3m3.core.model.SortOption
import dev.ao3m3.core.model.Work
import dev.ao3m3.core.model.WorkQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val works: WorkRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _query = MutableStateFlow(WorkQuery(useSearchEndpoint = true))
    val query: StateFlow<WorkQuery> = _query.asStateFlow()

    val savedSearches: StateFlow<List<String>> = session.savedSearches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val savedFandoms: StateFlow<List<String>> = session.savedFandoms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val fandomTerm = MutableStateFlow("")

    /** Debounced fandom names for the filter sheet; empty until three characters are typed. */
    val fandomSuggestions: StateFlow<List<String>> = fandomTerm
        .debounce(SUGGESTION_DEBOUNCE_MS)
        .mapLatest { works.fandomSuggestions(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val results = _query
        .flatMapLatest { request ->
            if (request.hasNothingToSearch()) {
                flowOf(PagingData.empty<Work>())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        enablePlaceholders = false,
                        prefetchDistance = 3,
                    ),
                ) { works.browse(request) }.flow
            }
        }
        .cachedIn(viewModelScope)

    fun submit(text: String) {
        _query.update { it.copy(query = text.trim(), page = 1) }
    }

    fun applyFilters(filters: SearchFilters) {
        _query.update { it.copy(filters = filters, page = 1) }
    }

    fun setSort(sort: SortOption) {
        _query.update { it.copy(sort = sort, page = 1) }
    }

    fun clearFilters() {
        _query.update { it.copy(filters = SearchFilters(), page = 1) }
    }

    fun saveSearch(term: String) {
        val value = term.trim()
        if (value.isEmpty()) return
        viewModelScope.launch { session.saveSearch(value) }
    }

    fun removeSavedSearch(term: String) {
        viewModelScope.launch { session.removeSearch(term) }
    }

    /** One tap on a saved fandom: filter by it, keeping any free-text terms already typed. */
    fun searchFandom(name: String) {
        val value = name.trim()
        if (value.isEmpty()) return
        _query.update { it.copy(filters = it.filters.copy(fandom = value), page = 1) }
    }

    fun saveFandom(name: String) {
        val value = name.trim()
        if (value.isEmpty()) return
        viewModelScope.launch { session.saveFandom(value) }
    }

    fun removeFandom(name: String) {
        viewModelScope.launch { session.removeFandom(name) }
    }

    fun suggestFandoms(term: String) {
        fandomTerm.value = term
    }

    private companion object {
        const val SUGGESTION_DEBOUNCE_MS = 400L
    }
}

/** An untouched search screen should not spend a request listing the whole archive. */
private fun WorkQuery.hasNothingToSearch(): Boolean =
    query.isBlank() && tag.isNullOrBlank() && filters.isEmpty
