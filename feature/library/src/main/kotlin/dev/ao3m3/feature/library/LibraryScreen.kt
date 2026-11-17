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
import dev.ao3m3.core.model.Work

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenWork: (Long) -> Unit,
    onLogin: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val works by viewModel.works.collectAsStateWithLifecycle()
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

    val selected = LibraryTab.entries.first { it.kind == tab }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !syncing && loggedIn && tab != LibraryKind.HISTORY,
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
                selectedTabIndex = selected.ordinal,
                edgePadding = Spacing.md,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                LibraryTab.entries.forEach { item ->
                    Tab(
                        selected = item == selected,
                        onClick = { viewModel.select(item.kind) },
                        text = { Text(item.label) },
                    )
                }
            }

            if (maintenance) {
                MaintenanceBanner(message = "The Archive is offline — showing saved items.")
            }

            if (!loggedIn && tab != LibraryKind.HISTORY && works.isNotEmpty()) {
                LoginPrompt(onLogin = onLogin)
            }

            val needsLogin = !loggedIn && tab != LibraryKind.HISTORY
            when {
                syncing && works.isEmpty() -> SkeletonList(rows = 5)

                works.isEmpty() -> EmptyState(
                    icon = if (needsLogin) Ao3Icons.Lock else selected.icon,
                    title = if (needsLogin) "Sign in to sync" else selected.emptyTitle,
                    message = if (needsLogin) {
                        "Your saved lists sync from AO3 when you're signed in. " +
                            "History stays on this device."
                    } else {
                        selected.emptyMessage
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
                    items(works, key = { it.id }) { work ->
                        LibraryRow(work = work, onClick = { onOpenWork(work.id) })
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
                    text = "Bookmarks, Later and Subscriptions update from AO3.",
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
private fun LibraryRow(work: Work, onClick: () -> Unit) {
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
                work.chapterLabel?.let { chapters ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = chapters,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
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

private enum class LibraryTab(
    val kind: LibraryKind,
    val label: String,
    val icon: ImageVector,
    val emptyTitle: String,
    val emptyMessage: String,
) {
    BOOKMARKS(
        LibraryKind.BOOKMARKS,
        "Bookmarks",
        Ao3Icons.Bookmark,
        "No bookmarks yet",
        "Bookmark a work to keep it here.",
    ),
    LATER(
        LibraryKind.MARKED_FOR_LATER,
        "Later",
        Ao3Icons.BookmarkOutline,
        "Nothing marked for later",
        "Mark a work for later to keep it here.",
    ),
    SUBSCRIPTIONS(
        LibraryKind.SUBSCRIPTIONS,
        "Subscriptions",
        Ao3Icons.Subscriptions,
        "No subscriptions yet",
        "Subscribe to a work or author to see updates here.",
    ),
    HISTORY(
        LibraryKind.HISTORY,
        "History",
        Ao3Icons.History,
        "No reading history yet",
        "Works you read will appear here.",
    ),
}
