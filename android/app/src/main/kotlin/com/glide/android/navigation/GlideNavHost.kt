package com.glide.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glide.android.ui.home.HomeScreen
import kotlinx.serialization.Serializable

/** Type-safe routes. Each screen gets one `@Serializable` route; arguments become its properties. */
@Serializable
data object HomeRoute

@Composable
fun GlideNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen() }
    }
}
