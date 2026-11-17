package dev.ao3m3.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.AppearanceSettings
import dev.ao3m3.core.model.ReadingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: SessionRepository,
    private val works: WorkRepository,
) : ViewModel() {

    val reading: StateFlow<ReadingSettings> = session.readingSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ReadingSettings(),
    )

    val appearance: StateFlow<AppearanceSettings> = session.appearance.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppearanceSettings(),
    )

    val loggedIn: StateFlow<Boolean> = session.loggedIn

    val userName: StateFlow<String?> = session.userName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshCacheSize()
    }

    fun setDarkTheme(dark: Boolean?) {
        viewModelScope.launch { session.setDarkTheme(dark) }
    }

    fun setReading(settings: ReadingSettings) {
        viewModelScope.launch { session.setReadingSettings(settings) }
    }

    fun logout() {
        viewModelScope.launch {
            session.logout()
            _message.value = "Logged out. Cached works stay available offline."
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            works.clearCachedText()
            refreshCacheSize()
            _message.value = "Cached chapters cleared."
        }
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = works.cacheSizeBytes()
        }
    }
}
