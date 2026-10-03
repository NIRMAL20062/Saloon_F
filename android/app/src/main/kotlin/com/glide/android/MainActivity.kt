package com.glide.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glide.android.navigation.GlideNavHost
import com.glide.android.ui.AppViewModel
import com.glide.android.ui.theme.GlideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlideTheme {
                val signedIn by appViewModel.signedIn.collectAsStateWithLifecycle()
                GlideNavHost(signedIn = signedIn)
            }
        }
    }
}
