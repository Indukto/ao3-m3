package dev.ao3m3.core.data.repo

import dev.ao3m3.core.data.net.Ao3Client
import dev.ao3m3.core.data.prefs.UserPrefs
import dev.ao3m3.core.model.AppearanceSettings
import dev.ao3m3.core.model.ReadingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns "is there a session?" and the display name that goes with it. The password never enters the
 * app: the login WebView posts it straight to AO3, and only the resulting cookie is imported.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val client: Ao3Client,
    private val prefs: UserPrefs,
) {
    private val _loggedIn = MutableStateFlow(client.hasSession())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    val userName: Flow<String?> = prefs.userName

    val readingSettings: Flow<ReadingSettings> = prefs.readingSettings
    val appearance: Flow<AppearanceSettings> = prefs.appearance
    val onboardingDone: Flow<Boolean> = prefs.onboardingDone
    val readerHintDismissed: Flow<Boolean> = prefs.readerHintDismissed
    val savedSearches: Flow<List<String>> = prefs.savedSearches

    suspend fun completeLogin(rawCookieHeader: String, userName: String?) {
        client.importWebViewCookies(rawCookieHeader)
        prefs.setUserName(userName?.trim()?.takeIf { it.isNotBlank() })
        _loggedIn.value = client.hasSession()
    }

    suspend fun logout() {
        client.clearSession()
        prefs.setUserName(null)
        _loggedIn.value = false
    }

    suspend fun setReadingSettings(settings: ReadingSettings) = prefs.setReadingSettings(settings)

    suspend fun setDarkTheme(dark: Boolean?) = prefs.setDarkTheme(dark)

    suspend fun setOnboardingDone() = prefs.setOnboardingDone()

    suspend fun dismissReaderHint() = prefs.setReaderHintDismissed()

    /** Saves a search term, newest first, de-duplicated and capped so the chip row stays usable. */
    suspend fun saveSearch(query: String) {
        val term = query.trim()
        if (term.isEmpty()) return
        val current = prefs.savedSearches.first()
        if (current.any { it.equals(term, ignoreCase = true) }) return
        prefs.setSavedSearches((listOf(term) + current).take(MAX_SAVED_SEARCHES))
    }

    suspend fun removeSearch(query: String) {
        prefs.setSavedSearches(prefs.savedSearches.first().filterNot { it == query })
    }

    /** Re-reads the cookie store, e.g. after the WebView finished a login. */
    fun refreshState() {
        _loggedIn.value = client.hasSession()
    }

    private companion object {
        const val MAX_SAVED_SEARCHES = 10
    }
}
