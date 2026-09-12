package io.github.mickaelmagniez.windbubble.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mickaelmagniez.windbubble.ui.home.HomeRoute
import io.github.mickaelmagniez.windbubble.ui.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

@Serializable
data object SettingsDestination

@Composable
fun WindBubbleNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = modifier,
        enterTransition = { slideInHorizontally { it / 4 } },
        exitTransition = { slideOutHorizontally { -it / 4 } },
        popEnterTransition = { slideInHorizontally { -it / 4 } },
        popExitTransition = { slideOutHorizontally { it / 4 } },
    ) {
        composable<HomeDestination> {
            HomeRoute(onOpenSettings = { navController.navigate(SettingsDestination) })
        }
        composable<SettingsDestination> {
            SettingsRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}
