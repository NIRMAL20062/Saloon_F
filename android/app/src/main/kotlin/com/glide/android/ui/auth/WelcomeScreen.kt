package com.glide.android.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.glide.android.R
import com.glide.android.ui.components.GlideIcons
import com.glide.android.ui.components.GlideWordmark
import com.glide.android.ui.components.PrimaryButton
import com.glide.android.ui.components.StatusBarIcons
import com.glide.android.ui.theme.GlideTheme
import com.glide.android.ui.theme.Spacing

/**
 * The first screen when signed out (mockup 1, D-041): the team's salon photo full screen, the white wordmark with the
 * leaf, "Look Good / Feel Amazing", and Get Started → phone login. The words float up when it opens.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatusBarIcons(darkIcons = false)
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.welcome_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Shade the top and bottom so the white text and the status bar always read well on the photo.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f),
                        0.38f to Color.Black.copy(alpha = 0.12f),
                        0.65f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(ENTER_MS)) + slideInVertically(tween(ENTER_MS)) { it / 6 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier =
                    Modifier.fillMaxWidth().statusBarsPadding().padding(
                        top = Spacing.xxl,
                        start = Spacing.l,
                        end = Spacing.l,
                    ),
            ) {
                GlideWordmark(name = stringResource(R.string.app_name), size = 44.sp, onPhoto = true)
                Text(
                    stringResource(R.string.welcome_line_1) + "\n" + stringResource(R.string.welcome_line_2),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.welcome_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.welcome_get_started),
            onClick = onGetStarted,
            trailingIcon = GlideIcons.ArrowForward,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.l, vertical = Spacing.l),
        )
    }
}

private const val ENTER_MS = 700

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun WelcomePreview() {
    GlideTheme { WelcomeScreen(onGetStarted = {}) }
}
