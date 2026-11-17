package dev.ao3m3.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.repo.LibraryRepository
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.model.LibraryKind
import dev.ao3m3.core.model.Work
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    session: SessionRepository,
) : ViewModel() {
    private val _tab = MutableStateFlow(LibraryKind.MARKED_FOR_LATER)
    val tab: StateFlow<LibraryKind> = _tab.asStateFlow()

    val loggedIn: StateFlow<Boolean> = session.loggedIn

    val works: StateFlow<List<Work>> = _tab
        .flatMapLatest { kind -> library.observe(kind) }
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

    fun select(kind: LibraryKind) {
        if (_tab.value == kind) return
        _tab.value = kind
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_tab.value == LibraryKind.HISTORY) {
                _maintenance.value = false
                return@launch
            }
            if (!loggedIn.value) {
                _maintenance.value = false
                return@launch
            }
            if (_syncing.value) return@launch
            _syncing.value = true
            _message.value = null
            val failure = library.refresh(_tab.value).exceptionOrNull()
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
