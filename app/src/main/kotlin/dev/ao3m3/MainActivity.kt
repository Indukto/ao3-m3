package dev.ao3m3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.Ao3Theme
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.AppearanceSettings
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {
    val appearance: StateFlow<AppearanceSettings> = session.appearance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppearanceSettings(),
    )

    val onboardingDone: StateFlow<Boolean?> = session.onboardingDone.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun completeOnboarding() {
        viewModelScope.launch { session.setOnboardingDone() }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { Root() }
    }
}

@Composable
private fun Root() {
    val viewModel: RootViewModel = hiltViewModel()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    Ao3Theme(
        darkTheme = appearance.darkTheme ?: systemDark,
    ) {
        // Wait for the stored flag instead of briefly showing the app to a first-run reader.
        when (val done = onboardingDone) {
            null -> Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {}

            true -> Ao3App()
            false -> OnboardingScreen(onContinue = viewModel::completeOnboarding)
        }
    }
}

/**
 * First run only: says what this app is (and is not) before it ever touches the archive.
 */
@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                ) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start reading") }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "You can log in later from Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Ao3Icons.Read,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Text(
                    text = "Archive",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = "A quieter way to read AO3.",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Offline-first, one request at a time, and no background crawling.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))
            Benefit(Ao3Icons.Offline, "Read offline", "Chapters you open are saved on this device.")
            Benefit(
                Ao3Icons.Lock,
                "Your account stays yours",
                "AO3 handles the login; only the session cookie is stored, encrypted.",
            )
            Benefit(Ao3Icons.Refresh, "No crawling", "Requests go one at a time, with a two-second gap.")
            Benefit(
                Ao3Icons.Info,
                "Unofficial",
                "Not affiliated with the OTW or the Archive of Our Own.",
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
