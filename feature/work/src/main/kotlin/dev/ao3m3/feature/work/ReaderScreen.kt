package dev.ao3m3.feature.work

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ao3m3.core.designsystem.component.ErrorState
import dev.ao3m3.core.designsystem.component.ReadingSettingsPanel
import dev.ao3m3.core.designsystem.component.SheetLayout
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.ReaderDarkBackground
import dev.ao3m3.core.designsystem.theme.ReaderDarkOnBackground
import dev.ao3m3.core.designsystem.theme.SepiaBackground
import dev.ao3m3.core.designsystem.theme.SepiaOnBackground
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.ReadingFont
import dev.ao3m3.core.model.ReadingTheme
import kotlin.math.roundToInt

private data class ReaderColors(val background: Color, val content: Color)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    workId: Long,
    startChapterId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(workId, startChapterId) { viewModel.start(workId, startChapterId) }

    val settings = state.settings
    val colors = readerColors(settings.theme)
    val listState = rememberLazyListState()
    var toolbarVisible by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val latestProgress by rememberUpdatedState(progress)
    val showHint by rememberUpdatedState(state.showControlsHint)

    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(listState, state.index) {
        snapshotFlow { listState.scrollFraction() }.collect { progress = it }
    }

    // Restore saved progress in the chapter it belongs to; every other chapter opens at the top.
    LaunchedEffect(state.text?.chapterId, state.resumeFraction) {
        if (state.text == null) return@LaunchedEffect
        val fraction = state.resumeFraction
        if (fraction <= 0.01f) {
            listState.scrollToItem(0)
            return@LaunchedEffect
        }
        val total = snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > 1 }
        listState.scrollToItem(((total - 1) * fraction).roundToInt().coerceIn(0, total - 1))
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveProgress(latestProgress) }
    }

    val textStyle = TextStyle(
        fontFamily = if (settings.font == ReadingFont.SERIF) FontFamily.Serif else FontFamily.SansSerif,
        fontSize = settings.fontSizeSp.sp,
        lineHeight = (settings.fontSizeSp * settings.lineHeight).sp,
        color = colors.content,
        textAlign = if (settings.justify) TextAlign.Justify else TextAlign.Start,
    )

    val linkColor = MaterialTheme.colorScheme.primary
    val blocks = remember(state.text?.html, colors.content, linkColor) {
        state.text?.html?.let { html ->
            renderChapterBlocks(html, linkColor, colors.content)
        }.orEmpty()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.loading && state.text == null -> ReaderLoading(colors)
                state.text == null -> ErrorState(
                    message = state.error ?: "This chapter isn't available yet.",
                    onRetry = viewModel::retry,
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = Spacing.proseMaxWidth)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    toolbarVisible = !toolbarVisible
                                    if (showHint) viewModel.dismissControlsHint()
                                },
                            )
                        },
                    contentPadding = PaddingValues(
                        start = settings.marginDp.dp,
                        end = settings.marginDp.dp,
                        top = Spacing.lg,
                        bottom = Spacing.readerToolbarClearance,
                    ),
                ) {
                    item {
                        Text(
                            text = state.chapters.getOrNull(state.index)?.displayTitle
                                ?: state.work?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium.copy(color = colors.content),
                        )
                        Spacer(Modifier.height(Spacing.lg))
                    }
                    state.text?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelLarge.copy(color = colors.content.copy(alpha = 0.7f)),
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = renderChapterHtml(notes, linkColor, colors.content),
                                style = textStyle.copy(fontSize = 15.sp),
                            )
                            Spacer(Modifier.height(Spacing.lg))
                        }
                    }

                    items(blocks) { block ->
                        Text(
                            text = block,
                            style = textStyle,
                            modifier = Modifier.padding(bottom = Spacing.lg),
                        )
                    }

                    state.text?.endNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item {
                            Spacer(Modifier.height(Spacing.md))
                            Text(
                                text = "End notes",
                                style = MaterialTheme.typography.labelLarge.copy(color = colors.content.copy(alpha = 0.7f)),
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = renderChapterHtml(notes, linkColor, colors.content),
                                style = textStyle.copy(fontSize = 15.sp),
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(Spacing.xxl))
                        if (state.index < state.chapters.lastIndex) {
                            Button(
                                onClick = viewModel::next,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Next chapter") }
                        } else {
                            Text(
                                text = "You've reached the end of this work.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.content.copy(alpha = 0.7f),
                                ),
                            )
                        }
                        Spacer(Modifier.height(Spacing.xl))
                        TextButton(onClick = onBack) { Text("Back to work") }
                    }
                }
            }

            if (state.loading) {
                // Chapter switches wait on the archive's two-second request gap; show that work is
                // happening instead of leaving the old chapter on screen with no feedback.
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = colors.background,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = colors.background,
                )
            }

            AnimatedVisibility(
                visible = toolbarVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    modifier = Modifier.padding(bottom = Spacing.xl),
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Ao3Icons.Back, contentDescription = "Back")
                    }
                    IconButton(
                        onClick = viewModel::previous,
                        enabled = state.index > 0,
                    ) {
                        Icon(Ao3Icons.PrevChapter, contentDescription = "Previous chapter")
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Ao3Icons.FontSize, contentDescription = "Reading settings")
                    }
                    IconButton(
                        onClick = viewModel::next,
                        enabled = state.index < state.chapters.lastIndex,
                    ) {
                        Icon(Ao3Icons.NextChapter, contentDescription = "Next chapter")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Ao3Icons.Settings, contentDescription = "Settings")
                    }
                }
            }

            if (state.showControlsHint && state.text != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = Spacing.xl)
                        .padding(bottom = Spacing.readerToolbarClearance + Spacing.xl),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Text(
                        text = "Tap the page to show reading controls",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            horizontal = Spacing.lg,
                            vertical = Spacing.md,
                        ),
                    )
                }
            }
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
            SheetLayout(title = "Reading") {
                ReadingSettingsPanel(
                    settings = settings,
                    onChange = viewModel::updateSettings,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReaderLoading(colors: ReaderColors) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(modifier = Modifier.size(56.dp), color = colors.content)
    }
}

@Composable
private fun readerColors(theme: ReadingTheme): ReaderColors = when (theme) {
    ReadingTheme.SYSTEM -> ReaderColors(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.onSurface,
    )

    ReadingTheme.LIGHT -> ReaderColors(Color(0xFFFFFBF8), Color(0xFF1B1B1B))
    ReadingTheme.SEPIA -> ReaderColors(SepiaBackground, SepiaOnBackground)
    ReadingTheme.DARK -> ReaderColors(ReaderDarkBackground, ReaderDarkOnBackground)
}

/**
 * 0..1 based on how far the first visible paragraph has scrolled through the chapter.
 *
 * Anchoring to the first visible item (and its offset) keeps the bar monotonic while items
 * enter and leave at the bottom of the viewport. The old bottom-anchored calculation changed
 * whenever another paragraph became visible, which made the bar jump around and start
 * partway across at the top.
 */
private fun LazyListState.scrollFraction(): Float {
    val info = layoutInfo
    val total = info.totalItemsCount
    if (total <= 1) return 0f
    val firstIndex = firstVisibleItemIndex
    val firstSize = info.visibleItemsInfo
        .firstOrNull { it.index == firstIndex }
        ?.size
        ?.coerceAtLeast(1)
        ?: 1
    val itemFraction = firstVisibleItemScrollOffset.toFloat() / firstSize
    return ((firstIndex + itemFraction) / (total - 1)).coerceIn(0f, 1f)
}
