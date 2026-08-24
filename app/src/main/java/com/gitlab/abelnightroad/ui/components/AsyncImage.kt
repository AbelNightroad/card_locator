package com.gitlab.abelnightroad.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

private const val USER_AGENT = "MtGCardTracker/1.0"

/** Scryfall image with the app's custom User-Agent header and loading/error slots. */
@Composable
fun ScryfallAsyncImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    loading: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    },
    error: @Composable () -> Unit = {
        Text("Failed to load image")
    }
) {
    if (url.isNotBlank()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .setHeader("User-Agent", USER_AGENT)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = { loading() },
            error = { error() }
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No image available")
        }
    }
}