package dev.ao3m3.feature.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.data.repo.WorkRepository
import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.ChapterText
import dev.ao3m3.core.model.ReadingProgress
import dev.ao3m3.core.model.ReadingSettings
import dev.ao3m3.core.model.Work
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val work: Work? = null,
    val chapters: List<Chapter> = emptyList(),
    val index: Int = 0,
    val text: ChapterText? = null,
    val loading: Boolean = true,
    val maintenance: Boolean = false,
    val error: String? = null,
    val settings: ReadingSettings = ReadingSettings(),
    /** Where to restore the scroll position inside the current chapter, 0..1. */
    val resumeFraction: Float = 0f,
    /** One-time guidance shown until the reader first taps the page. */
    val showControlsHint: Boolean = false,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val works: WorkRepository,
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var workId: Long = 0L
    private var started = false

    init {
        viewModelScope.launch {
            session.readingSettings.collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            session.readerHintDismissed.collect { dismissed ->
                _state.value = _state.value.copy(showControlsHint = !dismissed)
            }
        }
    }

    fun start(id: Long, startChapterId: Long) {
        if (started && workId == id) return
        started = true
        workId = id
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            var work = works.cachedWork(id)
            var chapters = works.cachedChapters(id)

            if (work == null || chapters.isEmpty()) {
                val result = works.refreshWork(id)
                val failure = result.exceptionOrNull()
                val parsed = result.getOrNull()
                if (parsed != null) {
                    work = parsed.work
                    chapters = parsed.chapters
                } else if (failure is Ao3Exception.Maintenance || work == null) {
                    _state.value = _state.value.copy(
                        loading = false,
                        maintenance = failure is Ao3Exception.Maintenance,
                        error = failure?.message ?: "This chapter isn't saved yet.",
                    )
                    return@launch
                }
            }

            // A chapter index we could not read must never block reading: the work page itself
            // still serves chapter one.
            if (chapters.isEmpty()) {
                chapters = listOf(Chapter(id = id, workId = id, index = 1, title = work.title))
            }

            val start = resolveReadingStart(
                chapters = chapters,
                startChapterId = startChapterId,
                progress = works.progressFor(id),
            )

            _state.value = _state.value.copy(
                work = work,
                chapters = chapters,
                index = start.index,
                resumeFraction = start.resumeFraction,
            )
            loadChapter(start.index)
        }
    }

    fun openChapter(index: Int) {
        if (index !in _state.value.chapters.indices) return
        _state.value = _state.value.copy(resumeFraction = 0f)
        // loadChapter saves progress only once the chapter actually loaded; a failed jump must
        // leave "continue reading" pointing at the last chapter that worked.
        viewModelScope.launch { loadChapter(index) }
    }

    fun next() = openChapter(_state.value.index + 1)

    fun previous() = openChapter(_state.value.index - 1)

    fun retry() {
        viewModelScope.launch { loadChapter(_state.value.index, force = true) }
    }

    fun saveProgress(fraction: Float) {
        val state = _state.value
        val chapter = state.chapters.getOrNull(state.index) ?: return
        viewModelScope.launch {
            works.saveProgress(
                ReadingProgress(
                    workId = workId,
                    chapterId = chapter.id,
                    chapterIndex = chapter.index,
                    scrollFraction = clampFraction(fraction),
                ),
            )
        }
    }

    fun updateSettings(settings: ReadingSettings) {
        _state.value = _state.value.copy(settings = settings)
        viewModelScope.launch { session.setReadingSettings(settings) }
    }

    fun dismissControlsHint() {
        if (!_state.value.showControlsHint) return
        _state.value = _state.value.copy(showControlsHint = false)
        viewModelScope.launch { session.dismissReaderHint() }
    }

    private suspend fun loadChapter(index: Int, force: Boolean = false) {
        val chapter = _state.value.chapters.getOrNull(index) ?: return
        _state.value = _state.value.copy(
            index = index,
            loading = true,
            error = null,
            maintenance = false,
        )
        val result = works.loadChapterText(workId, chapter, force)
        val loaded = result.getOrNull()
        val failure = result.exceptionOrNull()
        _state.value = _state.value.copy(
            // Never keep another chapter's text: a failed jump shows an error instead of the
            // previous chapter's prose under the new chapter's title.
            text = loaded ?: _state.value.text?.takeIf { it.chapterId == chapter.id },
            loading = false,
            maintenance = failure is Ao3Exception.Maintenance,
            error = if (loaded == null) {
                (failure as? Ao3Exception)?.userMessage()
                    ?: failure?.message
                    ?: "Couldn't load this chapter."
            } else {
                null
            },
        )
    }
}
