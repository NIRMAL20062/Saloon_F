package com.glide.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glide.android.ui.auth.LoginRoute
import com.glide.android.ui.home.SignedInRoute
import com.glide.android.ui.status.StatusRoute
import kotlinx.serialization.Serializable

/** Type-safe routes. Each screen gets one `@Serializable` route; arguments become its properties. */
@Serializable
data object LoginDestination

@Serializable
data object HomeDestination

@Serializable
data object StatusDestination

/**
 * Signed out → login; signed in → home. When the session changes (login, logout, refresh token rejected) the back stack
 * is replaced, so Back never returns to a screen of the other state.
 */
@Composable
fun GlideNavHost(
    signedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = if (signedIn) HomeDestination else LoginDestination) {
        composable<LoginDestination> { LoginRoute() }
        composable<HomeDestination> { SignedInRoute(onOpenStatus = { navController.navigate(StatusDestination) }) }
        composable<StatusDestination> { StatusRoute() }
    }

    LaunchedEffect(signedIn) {
        val target: Any = if (signedIn) HomeDestination else LoginDestination
        if (navController.currentDestination?.hasRoute(target::class) != true) {
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
