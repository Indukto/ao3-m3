package dev.ao3m3.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ao3m3.core.model.AppearanceSettings
import dev.ao3m3.core.model.ReadingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "ao3_prefs")

@Singleton
class UserPrefs @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val readingSettings: Flow<ReadingSettings> = store.data.map { prefs ->
        prefs[Keys.READING]?.let { encoded ->
            runCatching { json.decodeFromString<ReadingSettings>(encoded) }.getOrNull()
        } ?: ReadingSettings()
    }

    val appearance: Flow<AppearanceSettings> = store.data.map { prefs ->
        AppearanceSettings(
            darkTheme = when (prefs[Keys.DARK_THEME]) {
                "light" -> false
                "dark" -> true
                else -> null
            },
        )
    }

    val userName: Flow<String?> = store.data.map { it[Keys.USER_NAME]?.takeIf(String::isNotBlank) }

    val onboardingDone: Flow<Boolean> = store.data.map { it[Keys.ONBOARDED] ?: false }

    val readerHintDismissed: Flow<Boolean> = store.data.map {
        it[Keys.READER_HINT_DISMISSED] ?: false
    }

    /** Newest first; the search screen lists these in its saved-searches sheet. */
    val savedSearches: Flow<List<String>> = store.data.map { prefs ->
        prefs[Keys.SAVED_SEARCHES]
            ?.let { encoded -> runCatching { json.decodeFromString<List<String>>(encoded) }.getOrNull() }
            .orEmpty()
    }

    /** Newest first; the search screen shows these as one-tap fandom chips. */
    val savedFandoms: Flow<List<String>> = store.data.map { prefs ->
        prefs[Keys.SAVED_FANDOMS]
            ?.let { encoded -> runCatching { json.decodeFromString<List<String>>(encoded) }.getOrNull() }
            .orEmpty()
    }

    suspend fun setReadingSettings(settings: ReadingSettings) {
        store.edit { it[Keys.READING] = json.encodeToString(settings) }
    }

    suspend fun setDarkTheme(dark: Boolean?) {
        store.edit { prefs ->
            if (dark == null) prefs.remove(Keys.DARK_THEME)
            else prefs[Keys.DARK_THEME] = if (dark) "dark" else "light"
        }
    }

    suspend fun setUserName(name: String?) {
        store.edit { prefs ->
            if (name.isNullOrBlank()) prefs.remove(Keys.USER_NAME) else prefs[Keys.USER_NAME] = name
        }
    }

    suspend fun setOnboardingDone() {
        store.edit { it[Keys.ONBOARDED] = true }
    }

    suspend fun setReaderHintDismissed() {
        store.edit { it[Keys.READER_HINT_DISMISSED] = true }
    }

    suspend fun setSavedSearches(values: List<String>) {
        store.edit { it[Keys.SAVED_SEARCHES] = json.encodeToString(values) }
    }

    suspend fun setSavedFandoms(values: List<String>) {
        store.edit { it[Keys.SAVED_FANDOMS] = json.encodeToString(values) }
    }

    private object Keys {
        val READING = stringPreferencesKey("reading_settings")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SAVED_SEARCHES = stringPreferencesKey("saved_searches")
        val SAVED_FANDOMS = stringPreferencesKey("saved_fandoms")
        val READER_HINT_DISMISSED = booleanPreferencesKey("reader_hint_dismissed")
    }
}
