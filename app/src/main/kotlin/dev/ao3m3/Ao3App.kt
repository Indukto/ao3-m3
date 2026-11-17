package dev.ao3m3

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.feature.discover.DiscoverScreen
import dev.ao3m3.feature.discover.SearchScreen
import dev.ao3m3.feature.library.LibraryScreen
import dev.ao3m3.feature.settings.LoginScreen
import dev.ao3m3.feature.settings.SettingsScreen
import dev.ao3m3.feature.work.ReaderScreen
import dev.ao3m3.feature.work.WorkScreen
import dev.ao3m3.navigation.DiscoverRoute
import dev.ao3m3.navigation.LibraryRoute
import dev.ao3m3.navigation.LoginRoute
import dev.ao3m3.navigation.ReaderRoute
import dev.ao3m3.navigation.SearchRoute
import dev.ao3m3.navigation.SettingsRoute
import dev.ao3m3.navigation.WorkRoute

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

/**
 * Three top-level destinations, in the order the app cares about them: what to read, how to find
 * it, and what is already saved. Everything else is a push, not a tab.
 */
@Composable
fun Ao3App() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val destinations = listOf(
        TopLevelDestination(DiscoverRoute, "Discover", Ao3Icons.Discover),
        TopLevelDestination(SearchRoute, "Search", Ao3Icons.Search),
        TopLevelDestination(LibraryRoute, "Library", Ao3Icons.Library),
    )
    val showBar = destinations.any { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                // The bar below the NavHost owns the bottom system inset. Consume it here so
                // top-level screens do not reserve that space a second time.
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 900.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .consumeWindowInsets(
                        if (showBar) WindowInsets.navigationBars else WindowInsets(0, 0, 0, 0),
                    ),
                // Caps line length on tablets and foldables instead of stretching cards edge to edge.
                startDestination = DiscoverRoute,
                // A shared-axis move: the outgoing screen fades, the incoming one settles in on an
                // expressive spring rather than a linear tween.
                enterTransition = {
                    fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) {
                            it / 12
                        }
                },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = {
                    fadeOut(tween(160)) +
                        slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) {
                            it / 12
                        }
                },
            ) {
                composable<DiscoverRoute> {
                    DiscoverScreen(
                        onOpenWork = { navController.navigate(WorkRoute(it)) },
                        onOpenSearch = { navController.navigate(SearchRoute) },
                        onOpenLibrary = { navController.navigate(LibraryRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onOpenReader = { workId, chapterId ->
                            navController.navigate(ReaderRoute(workId, chapterId))
                        },
                    )
                }
                composable<SearchRoute> {
                    SearchScreen(onOpenWork = { navController.navigate(WorkRoute(it)) })
                }
                composable<LibraryRoute> {
                    LibraryScreen(
                        onOpenWork = { navController.navigate(WorkRoute(it)) },
                        onLogin = { navController.navigate(LoginRoute) },
                    )
                }
                composable<WorkRoute> { entry ->
                    val route = entry.toRoute<WorkRoute>()
                    WorkScreen(
                        workId = route.workId,
                        onBack = { navController.popBackStack() },
                        onRead = { chapterId ->
                            navController.navigate(ReaderRoute(route.workId, chapterId))
                        },
                    )
                }
                composable<ReaderRoute> { entry ->
                    val route = entry.toRoute<ReaderRoute>()
                    ReaderScreen(
                        workId = route.workId,
                        startChapterId = route.chapterId,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onLogin = { navController.navigate(LoginRoute) },
                    )
                }
                composable<LoginRoute> {
                    LoginScreen(
                        onDone = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() },
                    )
                }
            }

            if (showBar) {
                NavigationBar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 900.dp)
                        .fillMaxWidth(),
                ) {
                    destinations.forEach { destination ->
                        val selected =
                            currentDestination?.hasRoute(destination.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        }
    }
}
