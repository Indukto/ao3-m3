package dev.ao3m3.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.repo.LibraryRepository
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.LibraryKind
import dev.ao3m3.core.model.ProgressWithWork
import dev.ao3m3.core.model.SortOption
import dev.ao3m3.core.model.Work
import dev.ao3m3.core.model.WorkQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val resumable: List<ProgressWithWork> = emptyList(),
    val popular: List<Work> = emptyList(),
    val updates: List<Work> = emptyList(),
    val loading: Boolean = true,
    val maintenance: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val works: WorkRepository,
    private val library: LibraryRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    init {
        load()
        observeSubscriptions()
    }

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val resumable = works.recentProgress(limit = 6)
            // Cached page one paints immediately; the request below only revalidates it. Keeping
            // `loading` true until that request returns is what shows the skeleton instead of an
            // empty "nothing to show" screen while AO3 takes its time.
            val cached = if (force) emptyList() else works.cachedPage(popularQuery())
            _state.value = _state.value.copy(
                resumable = resumable,
                popular = cached.ifEmpty { _state.value.popular },
            )

            val result = runCatching { works.loadPage(popularQuery()) }
            val failure = result.exceptionOrNull()
            val page = result.getOrNull()
            _state.value = _state.value.copy(
                popular = page?.works?.take(8) ?: _state.value.popular,
                loading = false,
                maintenance = failure is Ao3Exception.Maintenance,
                error = when {
                    page != null -> null
                    failure is Ao3Exception.Maintenance -> null
                    failure != null -> failure.message ?: "Couldn't refresh"
                    else -> null
                },
            )
        }
    }

    private fun observeSubscriptions() {
        viewModelScope.launch {
            session.loggedIn
                .flatMapLatest { loggedIn ->
                    if (!loggedIn) {
                        flowOf(emptyList())
                    } else {
                        runCatching { library.refresh(LibraryKind.SUBSCRIPTIONS) }
                        library.observe(LibraryKind.SUBSCRIPTIONS)
                    }
                }
                .collect { entries ->
                    _state.value = _state.value.copy(updates = entries.take(5))
                }
        }
    }

    /** Popular this week, expressed with the only lever AO3 gives us: sort by kudos. */
    private fun popularQuery(): WorkQuery = WorkQuery(
        sort = SortOption.KUDOS,
        filters = dev.ao3m3.core.model.SearchFilters(
            minWords = 1_000,
            completeOnly = true,
        ),
    )
}
