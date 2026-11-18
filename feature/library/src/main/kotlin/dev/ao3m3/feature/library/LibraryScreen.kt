package dev.ao3m3.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ao3m3.core.designsystem.component.EmptyState
import dev.ao3m3.core.designsystem.component.MaintenanceBanner
import dev.ao3m3.core.designsystem.component.SkeletonList
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.LibraryKind
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenWork: (Long) -> Unit,
    onOpenReader: (Long, Long) -> Unit,
    onLogin: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val libraryItems by viewModel.items.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val maintenance by viewModel.maintenance.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    val needsLogin = tab.needsLogin && !loggedIn

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !syncing && loggedIn && tab.needsLogin,
                    ) {
                        if (syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Ao3Icons.Refresh, contentDescription = "Sync library")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                edgePadding = Spacing.md,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                LibraryTab.entries.forEach { item ->
                    Tab(
                        selected = item == tab,
                        onClick = { viewModel.select(item) },
                        text = { Text(item.label) },
                    )
                }
            }

            if (maintenance) {
                MaintenanceBanner(message = "The Archive is offline — showing saved items.")
            }

            if (needsLogin && libraryItems.isNotEmpty()) {
                LoginPrompt(onLogin = onLogin)
            }

            when {
                syncing && libraryItems.isEmpty() -> SkeletonList(rows = 5)

                libraryItems.isEmpty() -> EmptyState(
                    icon = if (needsLogin) Ao3Icons.Lock else tab.icon,
                    title = if (needsLogin) "Sign in to sync" else tab.emptyTitle,
                    message = if (needsLogin) {
                        "Your saved lists sync from AO3 when you're signed in. " +
                            "Reading and history stay on this device."
                    } else {
                        tab.emptyMessage
                    },
                    actionLabel = if (needsLogin) "Log in" else null,
                    onAction = if (needsLogin) onLogin else null,
                    modifier = Modifier.padding(top = Spacing.xl),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = Spacing.sm,
                        bottom = Spacing.lg,
                    ),
                ) {
                    items(libraryItems, key = { it.work.id }) { item ->
                        LibraryRow(
                            item = item,
                            onClick = {
                                val progress = item.progress
                                if (progress != null) {
                                    onOpenReader(item.work.id, progress.chapterId)
                                } else {
                                    onOpenWork(item.work.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginPrompt(onLogin: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sign in to sync", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Saved works and subscriptions update from AO3.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            TextButton(onClick = onLogin) { Text("Log in") }
        }
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, onClick: () -> Unit) {
    val work = item.work
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = work.authorLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = item.progress?.let {
                    "Chapter ${it.chapterIndex} · ${(it.scrollFraction * 100).roundToInt()}%"
                } ?: item.source ?: work.chapterLabel
                detail?.let { label ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        // Accent marks reading progress; a plain source label is metadata.
                        color = if (item.progress != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(horizontal = Spacing.gutter),
        )
    }
}

/**
 * Library tabs. Reading and History are this device's own record, so they work signed out; the
 * AO3-synced lists need an account. [kind] is the single AO3 list a tab mirrors, or null when the
 * tab is local or merges more than one list.
 */
enum class LibraryTab(
    val label: String,
    val icon: ImageVector,
    val kind: LibraryKind?,
    val needsLogin: Boolean,
    val emptyTitle: String,
    val emptyMessage: String,
) {
    READING(
        label = "Reading",
        icon = Ao3Icons.Read,
        kind = null,
        needsLogin = false,
        emptyTitle = "Nothing in progress",
        emptyMessage = "Works you start reading appear here. No account needed.",
    ),
    SAVED(
        label = "Saved",
        icon = Ao3Icons.Bookmark,
        kind = null,
        needsLogin = true,
        emptyTitle = "Nothing saved yet",
        emptyMessage = "Bookmarked and marked-for-later works appear here.",
    ),
    SUBSCRIPTIONS(
        label = "Subscriptions",
        icon = Ao3Icons.Subscriptions,
        kind = LibraryKind.SUBSCRIPTIONS,
        needsLogin = true,
        emptyTitle = "No subscriptions yet",
        emptyMessage = "Subscribe to a work or author to see updates here.",
    ),
    HISTORY(
        label = "History",
        icon = Ao3Icons.History,
        kind = LibraryKind.HISTORY,
        needsLogin = false,
        emptyTitle = "No reading history yet",
        emptyMessage = "Works you read will appear here.",
    ),
}
