package dev.ao3m3.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ao3m3.core.designsystem.component.EmptyState
import dev.ao3m3.core.designsystem.component.MaintenanceBanner
import dev.ao3m3.core.designsystem.component.SectionTitle
import dev.ao3m3.core.designsystem.component.SkeletonList
import dev.ao3m3.core.designsystem.component.WorkCard
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.ProgressWithWork
import dev.ao3m3.core.model.WorkLink

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DiscoverScreen(
    onOpenWork: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var openLinkDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Discover") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Ao3Icons.Settings, contentDescription = "Settings")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Ao3Icons.More, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open a work link") },
                                onClick = {
                                    menuOpen = false
                                    openLinkDialog = true
                                },
                                leadingIcon = {
                                    Icon(Ao3Icons.OpenLink, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.load()
                                },
                                leadingIcon = {
                                    Icon(Ao3Icons.Refresh, contentDescription = null)
                                },
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
            ),
        ) {
            if (state.maintenance) {
                item {
                    MaintenanceBanner(modifier = Modifier.padding(top = Spacing.sm))
                }
            }

            if (state.resumable.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Continue reading",
                        actionLabel = "Library",
                        onAction = onOpenLibrary,
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.gutter),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(state.resumable, key = { it.work.id }) { item ->
                            ResumeCard(
                                item = item,
                                onClick = { onOpenReader(item.work.id, item.progress.chapterId) },
                            )
                        }
                    }
                }
            }

            if (state.loading && state.popular.isEmpty()) {
                item {
                    SectionTitle(
                        title = "Popular this week",
                        actionLabel = "Search",
                        onAction = onOpenSearch,
                    )
                }
                item { SkeletonList(rows = 4) }
            } else {
                if (state.popular.isNotEmpty()) {
                    item {
                        SectionTitle(
                            title = "Popular this week",
                            actionLabel = "Search",
                            onAction = onOpenSearch,
                        )
                    }
                    if (state.loading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = Spacing.gutter,
                                        vertical = Spacing.xs,
                                    ),
                            )
                        }
                    }
                    items(state.popular, key = { it.id }) { work ->
                        WorkCard(
                            work = work,
                            onClick = { onOpenWork(work.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (state.updates.isNotEmpty()) {
                    item { SectionTitle(title = "From your subscriptions") }
                    items(state.updates, key = { "sub-${it.id}" }) { work ->
                        WorkCard(
                            work = work,
                            onClick = { onOpenWork(work.id) },
                            modifier = Modifier.animateItem(),
                            showSummary = false,
                            maxTags = 2,
                        )
                    }
                }

                if (state.popular.isEmpty() && state.updates.isEmpty() && !state.loading) {
                    item {
                        val failed = state.maintenance || state.error != null
                        val retry: () -> Unit = { viewModel.load(force = true) }
                        EmptyState(
                            icon = if (state.maintenance) Ao3Icons.Offline else Ao3Icons.Search,
                            title = when {
                                state.maintenance -> "The Archive is offline for maintenance."
                                state.error != null -> "Couldn't reach the Archive."
                                else -> "Nothing to show yet."
                            },
                            message = if (state.maintenance) {
                                "Everything you've saved is still available offline in your Library."
                            } else {
                                state.error ?: "Search for a fandom or tag to start reading."
                            },
                            actionLabel = if (failed) "Try again" else "Find something to read",
                            onAction = if (failed) retry else onOpenSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                        )
                    }
                }
            }
        }
    }

    if (openLinkDialog) {
        OpenWorkLinkDialog(
            onDismiss = { openLinkDialog = false },
            onOpen = { workId ->
                openLinkDialog = false
                onOpenWork(workId)
            },
        )
    }
}

@Composable
private fun OpenWorkLinkDialog(
    onDismiss: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val workId = remember(input) { WorkLink.parseWorkId(input) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open a work") },
        text = {
            Column {
                Text(
                    text = "Paste an AO3 work or chapter link, or type the work number.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    label = { Text("Link or work id") },
                    isError = input.isNotBlank() && workId == null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { workId?.let(onOpen) },
                enabled = workId != null,
            ) { Text("Open") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ResumeCard(item: ProgressWithWork, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.width(260.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = item.work.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Chapter ${item.progress.chapterIndex} · ${item.work.authorLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { item.progress.scrollFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
