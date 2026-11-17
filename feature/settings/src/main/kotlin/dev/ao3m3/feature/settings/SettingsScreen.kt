package dev.ao3m3.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ao3m3.core.designsystem.component.ReadingSettingsPanel
import dev.ao3m3.core.designsystem.component.SectionTitle
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val reading by viewModel.reading.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSize.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Ao3Icons.Back, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Spacing.xxl),
        ) {
            item { SectionTitle("Appearance") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                ) {
                    Text("Dynamic colour", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Colours follow your wallpaper. Choose a brightness below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        ThemeChip("System", appearance.darkTheme == null) {
                            viewModel.setDarkTheme(null)
                        }
                        ThemeChip("Light", appearance.darkTheme == false) {
                            viewModel.setDarkTheme(false)
                        }
                        ThemeChip("Dark", appearance.darkTheme == true) {
                            viewModel.setDarkTheme(true)
                        }
                    }
                }
            }

            item { SectionTitle("Reading") }
            item {
                ReadingSettingsPanel(
                    settings = reading,
                    onChange = viewModel::setReading,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
            }

            item { SectionTitle("Account") }
            item {
                if (loggedIn) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                    ) {
                        Text(
                            text = userName?.let { "Signed in as $it" } ?: "Signed in",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedButton(onClick = viewModel::logout) { Text("Log out") }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                    ) {
                        Text(
                            "Sign in with AO3's own login page. Your password never enters this app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Button(onClick = onLogin) { Text("Log in to AO3") }
                    }
                }
            }

            item { SectionTitle("Storage") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                ) {
                    Text(
                        "Cached chapters: ${formatBytes(cacheSize)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(onClick = viewModel::clearCache) {
                        Text("Clear cached chapters")
                    }
                }
            }

            item { SectionTitle("About") }
            item {
                Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
                    Text(
                        "Archive is an unofficial, read-only client for the Archive of Our Own. " +
                            "It is not affiliated with the OTW.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Requests are made one at a time with a two-second gap, the app identifies " +
                            "itself, and nothing is crawled in the background. Logging in only stores " +
                            "AO3's session cookie, encrypted with a hardware-backed Android Keystore key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "none"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
