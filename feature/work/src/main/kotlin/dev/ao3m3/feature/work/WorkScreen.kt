package dev.ao3m3.feature.work

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ao3m3.core.designsystem.component.ErrorState
import dev.ao3m3.core.designsystem.component.MaintenanceBanner
import dev.ao3m3.core.designsystem.component.SectionTitle
import dev.ao3m3.core.designsystem.component.SkeletonList
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.Ao3Theme
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.Chapter
import dev.ao3m3.core.model.LibraryKind
import dev.ao3m3.core.model.Work

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkScreen(
    workId: Long,
    onBack: () -> Unit,
    onRead: (Long) -> Unit,
    viewModel: WorkViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var chaptersOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(workId) { viewModel.start(workId) }
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Ao3Icons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.loading,
                    ) {
                        Icon(Ao3Icons.Refresh, contentDescription = "Refresh work")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            state.loading && state.work == null -> SkeletonList(
                rows = 5,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )

            state.work == null -> ErrorState(
                message = state.error ?: "This work isn't available offline yet.",
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )

            else -> {
                val work = state.work!!
                val progress = state.progress
                val resumeChapter = state.chapters
                    .firstOrNull { it.id == progress?.chapterId }
                    ?: state.chapters.firstOrNull()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + Spacing.sm,
                        bottom = innerPadding.calculateBottomPadding() + Spacing.xxl,
                    ),
                ) {
                    if (state.maintenance) {
                        item {
                            MaintenanceBanner(message = "Showing the saved copy. The Archive is in maintenance.")
                        }
                    }
                    item {
                        WorkHeader(
                            work = work,
                            resumeLabel = if (progress != null && progress.scrollFraction > 0.02f) {
                                "Continue · Ch ${progress.chapterIndex}"
                            } else {
                                "Start reading"
                            },
                            onRead = { onRead(resumeChapter?.id ?: -1L) },
                            onChapters = { chaptersOpen = true },
                            bookmarked = state.bookmarked,
                            markedForLater = state.markedForLater,
                            subscribed = state.subscribed,
                            loggedIn = state.loggedIn,
                            onToggle = viewModel::toggle,
                        )
                    }

                    state.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        item {
                            SectionBlock(title = "Notes") {
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        SectionBlock(title = "Tags") {
                            TagSections(work)
                        }
                    }

                    if (state.chapters.size > 1) {
                        item {
                            SectionTitle(
                                title = "Chapters",
                                actionLabel = "View all ${state.chapters.size}",
                                onAction = { chaptersOpen = true },
                            )
                        }
                        items(state.chapters.take(4), key = { it.id }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                isCurrent = chapter.id == progress?.chapterId,
                                onClick = { onRead(chapter.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (chaptersOpen) {
        ModalBottomSheet(onDismissRequest = { chaptersOpen = false }) {
            ChapterSheet(
                chapters = state.chapters,
                currentId = state.progress?.chapterId,
                onSelect = {
                    chaptersOpen = false
                    onRead(it.id)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkHeader(
    work: Work,
    resumeLabel: String,
    onRead: () -> Unit,
    onChapters: () -> Unit,
    bookmarked: Boolean,
    markedForLater: Boolean,
    subscribed: Boolean,
    loggedIn: Boolean,
    onToggle: (LibraryKind) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
        Text(
            text = work.title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "by ${work.authorLabel}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = metadataLine(work),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(Spacing.lg))
        SplitButtonLayout(
            leadingButton = {
                Button(onClick = onRead, modifier = Modifier.fillMaxWidth()) {
                    Icon(Ao3Icons.Read, contentDescription = null)
                    Text("  $resumeLabel", maxLines = 1)
                }
            },
            trailingButton = {
                FilledTonalIconButton(onClick = onChapters) {
                    Icon(Ao3Icons.ChapterList, contentDescription = "Chapters")
                }
            },
        )

        Spacer(Modifier.height(Spacing.md))
        ButtonGroup(
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            expandedRatio = 0.1f,
        ) {
            clickableItem(
                onClick = { onToggle(LibraryKind.BOOKMARKS) },
                label = if (bookmarked) "Bookmarked" else "Bookmark",
                enabled = loggedIn,
            )
            clickableItem(
                onClick = { onToggle(LibraryKind.MARKED_FOR_LATER) },
                label = if (markedForLater) "Saved for later" else "Mark for later",
                enabled = loggedIn,
            )
            clickableItem(
                onClick = { onToggle(LibraryKind.SUBSCRIPTIONS) },
                label = if (subscribed) "Subscribed" else "Subscribe",
                enabled = loggedIn,
            )
        }

        if (work.summary.isNotBlank()) {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = work.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSections(work: Work) {
    val sections = buildList {
        if (work.rating != null) add("Rating" to listOfNotNull(work.rating))
        if (work.warnings.isNotEmpty()) add("Warnings" to work.warnings)
        if (work.relationships.isNotEmpty()) add("Relationships" to work.relationships)
        if (work.characters.isNotEmpty()) add("Characters" to work.characters)
        if (work.additionalTags.isNotEmpty()) add("Additional tags" to work.additionalTags)
        if (work.categories.isNotEmpty()) add("Categories" to work.categories)
    }
    val allTags = sections.flatMap { it.second }.distinct()
    if (allTags.isEmpty()) {
        Text(
            text = "No tags",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    var expanded by rememberSaveable(work.id) { mutableStateOf(false) }
    if (!expanded) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            maxLines = 3,
        ) {
            allTags.take(8).forEach { TagPill(it) }
        }
        if (allTags.size > 8) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = Spacing.none, vertical = Spacing.xs),
            ) {
                Text("Show all ${allTags.size} tags")
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            sections.forEach { (label, tags) ->
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        tags.forEach { TagPill(it) }
                    }
                }
            }
        }
        TextButton(
            onClick = { expanded = false },
            contentPadding = PaddingValues(horizontal = Spacing.none, vertical = Spacing.xs),
        ) {
            Text("Show fewer tags")
        }
    }
}

@Composable
private fun TagPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
        )
    }
}

@Composable
private fun SectionBlock(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.md),
    ) {
        SectionTitle(title = title, topPadding = Spacing.xl)
        Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
            content()
        }
    }
}

@Composable
private fun ChapterRow(chapter: Chapter, isCurrent: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${chapter.index}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )
            Text(
                text = chapter.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isCurrent) {
                Icon(
                    imageVector = Ao3Icons.Check,
                    contentDescription = "Current chapter",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .size(20.dp),
                )
            } else {
                Icon(
                    imageVector = Ao3Icons.NextChapter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .size(18.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

@Composable
private fun ChapterSheet(chapters: List<Chapter>, currentId: Long?, onSelect: (Chapter) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
    ) {
        item {
            Text(
                "Chapters",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md),
            )
        }
        items(chapters, key = { it.id }) { chapter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(chapter) }
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${chapter.index}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )
                Text(
                    text = chapter.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (chapter.id == currentId) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.md),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapter.id == currentId) {
                    Icon(
                        imageVector = Ao3Icons.Check,
                        contentDescription = "Current chapter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

private fun metadataLine(work: Work): String = buildList {
    work.fandoms.firstOrNull()?.let(::add)
    work.words?.let { add("%,d words".format(it)) }
    work.chapterLabel?.let { add("$it chapters") }
    work.kudos?.takeIf { it > 0 }?.let { add("%,d kudos".format(it)) }
}.joinToString(" · ")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, heightDp = 720)
@Composable
private fun WorkHeaderPreview() {
    Ao3Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WorkHeader(
                work = Work(
                    id = 1L,
                    title = "A Long Winter of Small Kindnesses",
                    authors = listOf("stormandsky"),
                    summary = "Snowed in at a cottage, two strangers learn to share a fire.",
                    fandoms = listOf("Original Work"),
                    relationships = listOf("A/B"),
                    additionalTags = listOf("Fluff", "Slow Burn"),
                    words = 12_345,
                    chaptersPublished = 3,
                    chaptersTotal = 12,
                    kudos = 1_203,
                ),
                resumeLabel = "Start reading",
                onRead = {},
                onChapters = {},
                bookmarked = false,
                markedForLater = false,
                subscribed = false,
                loggedIn = true,
                onToggle = {},
            )
        }
    }
}
