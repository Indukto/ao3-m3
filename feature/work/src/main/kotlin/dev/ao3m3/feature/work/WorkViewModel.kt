package dev.ao3m3.feature.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.repo.LibraryRepository
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.LibraryKind
import dev.ao3m3.core.model.ReadingProgress
import dev.ao3m3.core.model.Work
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkUiState(
    val work: Work? = null,
    val chapters: List<Chapter> = emptyList(),
    val notes: String? = null,
    val loading: Boolean = true,
    val maintenance: Boolean = false,
    val error: String? = null,
    val progress: ReadingProgress? = null,
    val bookmarked: Boolean = false,
    val markedForLater: Boolean = false,
    val subscribed: Boolean = false,
    val actionMessage: String? = null,
    val loggedIn: Boolean = false,
)

@HiltViewModel
class WorkViewModel @Inject constructor(
    private val works: WorkRepository,
    private val library: LibraryRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkUiState())
    val state: StateFlow<WorkUiState> = _state.asStateFlow()

    private var workId: Long = 0L
    private var loadJob: Job? = null

    fun start(id: Long, refresh: Boolean = true) {
        if (workId == id && _state.value.work != null && !refresh) return
        val switching = workId != id
        workId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                maintenance = false,
                notes = if (switching) null else _state.value.notes,
            )
            val cachedWork = works.cachedWork(id)
            val cachedChapters = works.cachedChapters(id)
            _state.value = _state.value.copy(
                work = cachedWork,
                chapters = cachedChapters,
                progress = works.progressFor(id),
                loggedIn = session.loggedIn.value,
            )

            if (refresh || cachedWork == null) {
                val result = works.refreshWork(id)
                val failure = result.exceptionOrNull()
                val parsed = result.getOrNull()
                _state.value = _state.value.copy(
                    work = parsed?.work ?: cachedWork,
                    chapters = parsed?.chapters ?: cachedChapters,
                    notes = parsed?.notes ?: _state.value.notes,
                    loading = false,
                    maintenance = failure is Ao3Exception.Maintenance,
                    error = if (parsed == null) {
                        failure?.message ?: "Couldn't load this work."
                    } else {
                        null
                    },
                )
            }
            if (_state.value.work != null) library.recordHistory(id)
            refreshMembership()
        }
    }

    fun refresh() {
        if (workId != 0L) start(workId, refresh = true)
    }

    fun toggle(kind: LibraryKind) {
        if (workId == 0L) return
        viewModelScope.launch {
            val result = library.toggle(workId, kind)
            val failure = result.exceptionOrNull()
            if (failure != null) {
                _state.value = _state.value.copy(
                    actionMessage = when (failure) {
                        is Ao3Exception.Unauthorized -> "Log in to use your library."
                        is Ao3Exception.Maintenance -> "The Archive is offline. Try again later."
                        else -> failure.message
                    },
                )
            }
            refreshMembership()
        }
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }

    private suspend fun refreshMembership() {
        if (workId == 0L) return
        _state.value = _state.value.copy(
            bookmarked = library.isIn(workId, LibraryKind.BOOKMARKS),
            markedForLater = library.isIn(workId, LibraryKind.MARKED_FOR_LATER),
            subscribed = library.isIn(workId, LibraryKind.SUBSCRIPTIONS),
            progress = works.progressFor(workId),
            loggedIn = session.loggedIn.value,
        )
    }
}
