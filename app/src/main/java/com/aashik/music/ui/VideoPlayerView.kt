package com.aashik.music.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private val VideoContainerShape = RoundedCornerShape(16.dp)

/**
 * High-performance, zero-external-dependency Video & YouTube Player.
 * Uses Android's hardware-accelerated WebView engine to stream YouTube music videos
 * directly within the Automotive Headunit and Mobile HUD without any APK size bloat.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    videoId: String?,
    videoUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            setBackgroundColor(Color.BLACK)
        }
    }

    DisposableEffect(videoId, videoUrl) {
        val htmlContent = if (!videoId.isNullOrBlank()) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe 
                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&modestbranding=1&playsinline=1&rel=0" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                    allowfullscreen>
                </iframe>
            </body>
            </html>
            """.trimIndent()
        } else if (!videoUrl.isNullOrBlank()) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    video { width: 100%; height: 100%; object-fit: contain; }
                </style>
            </head>
            <body>
                <video src="$videoUrl" autoplay controls playsinline></video>
            </body>
            </html>
            """.trimIndent()
        } else ""

        if (htmlContent.isNotBlank()) {
            webView.loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
        }

        onDispose {
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webView.destroy()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .clip(VideoContainerShape)
            .background(androidx.compose.ui.graphics.Color.Black, VideoContainerShape)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
