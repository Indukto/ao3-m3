package dev.ao3m3.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.repo.LibraryRepository
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.LibraryKind
import dev.ao3m3.core.model.ReadingProgress
import dev.ao3m3.core.model.Work
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row of the library. [source] labels a saved work once Bookmarks and Later share a list. */
data class LibraryItem(
    val work: Work,
    val source: String? = null,
    val progress: ReadingProgress? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val works: WorkRepository,
    session: SessionRepository,
) : ViewModel() {

    private val _tab = MutableStateFlow(LibraryTab.READING)
    val tab: StateFlow<LibraryTab> = _tab.asStateFlow()

    val loggedIn: StateFlow<Boolean> = session.loggedIn

    /**
     * Reading progress is this device's own record, so the Reading tab never needs an account.
     * Bookmarks and marked for later are the same "keep this" intent on AO3 and are shown as one
     * Saved list, with each row labelled so the distinction is not lost.
     */
    val items: StateFlow<List<LibraryItem>> = _tab
        .flatMapLatest { tab ->
            when (tab) {
                LibraryTab.READING -> works.observeReading().map { entries ->
                    entries.map { LibraryItem(work = it.work, progress = it.progress) }
                }

                LibraryTab.SAVED -> combine(
                    library.observe(LibraryKind.BOOKMARKS),
                    library.observe(LibraryKind.MARKED_FOR_LATER),
                ) { bookmarked, later ->
                    bookmarked.map { LibraryItem(it, source = "Bookmarked") } +
                        later.map { LibraryItem(it, source = "Marked for later") }
                }

                LibraryTab.SUBSCRIPTIONS -> library.observe(LibraryKind.SUBSCRIPTIONS)
                    .map { entries -> entries.map { LibraryItem(it) } }

                LibraryTab.HISTORY -> library.observe(LibraryKind.HISTORY)
                    .map { entries -> entries.map { LibraryItem(it) } }
            }.map { list -> list.distinctBy { it.work.id } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _maintenance = MutableStateFlow(false)
    val maintenance: StateFlow<Boolean> = _maintenance.asStateFlow()

    init {
        refresh()
    }

    fun select(tab: LibraryTab) {
        if (_tab.value == tab) return
        _tab.value = tab
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val tab = _tab.value
            if (!tab.needsLogin || !loggedIn.value) {
                _maintenance.value = false
                return@launch
            }
            if (_syncing.value) return@launch
            _syncing.value = true
            _message.value = null
            val failure = when (tab) {
                LibraryTab.SAVED ->
                    library.refresh(LibraryKind.BOOKMARKS).exceptionOrNull()
                        ?: library.refresh(LibraryKind.MARKED_FOR_LATER).exceptionOrNull()

                else -> tab.kind?.let { library.refresh(it).exceptionOrNull() }
            }
            _syncing.value = false
            _maintenance.value = failure is Ao3Exception.Maintenance
            _message.value = when (failure) {
                is Ao3Exception.Maintenance -> null
                is Ao3Exception.Unauthorized -> "Log in to sync your library."
                null -> null
                else -> failure.message
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
