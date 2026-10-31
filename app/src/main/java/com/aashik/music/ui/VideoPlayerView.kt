package com.aashik.music.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.StateFlow

// Precomputed shape constant strictly adhering to Automotive Mali GPU optimization guidelines
private val VideoContainerShape = RoundedCornerShape(16.dp)
private val WV_VERSION_REGEX = Regex("""Version/\d+\.\d+ """)

/**
 * Ultra-high-performance, zero-external-dependency Video & YouTube Stream Player.
 * Uses Android's hardware-accelerated WebView engine to stream YouTube music videos
 * directly within the Automotive Headunit and Mobile HUD without any APK size bloat.
 * 
 * Synchronizes video playback clock and play/pause states directly with the primary
 * audio service, keeping EQ, Crossfade, and DSP intact while running video silently in sync.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    videoId: String?,
    videoUrl: String?,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            // Strip Android WebView markers so YouTube does not block embedding with Error 150
            val defaultUa = settings.userAgentString ?: ""
            settings.userAgentString = defaultUa.replace("; wv", "").replace(WV_VERSION_REGEX, "")

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }
            setBackgroundColor(Color.BLACK)
        }
    }

    // Sync play/pause changes with the video surface
    LaunchedEffect(isPlaying) {
        webView.evaluateJavascript("if (typeof setPlaying === 'function') { setPlaying($isPlaying); }", null)
    }

    // Sync user seek jumps (> 1500ms difference) to the video surface without triggering UI recompositions
    LaunchedEffect(positionFlow) {
        var lastMs = positionFlow.value
        positionFlow.collect { pos ->
            if (kotlin.math.abs(pos - lastMs) > 1500L) {
                val seekSec = pos / 1000f
                webView.evaluateJavascript("if (typeof seekToTime === 'function') { seekToTime($seekSec); }", null)
            }
            lastMs = pos
        }
    }

    // Reload content whenever video target changes
    DisposableEffect(videoId, videoUrl) {
        val initialStartSec = (positionFlow.value / 1000L).coerceAtLeast(0L)
        val initialPlaying = isPlaying

        val htmlContent = if (!videoId.isNullOrBlank()) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body, html { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    iframe { width: 100%; height: 100%; border: none; }
                </style>
            </head>
            <body>
                <iframe id="player"
                    src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&autoplay=1&mute=1&controls=0&playsinline=1&modestbranding=1&rel=0&origin=https://www.youtube-nocookie.com&widget_referrer=https://www.youtube-nocookie.com&start=$initialStartSec"
                    allow="autoplay; encrypted-media; picture-in-picture"
                    allowfullscreen>
                </iframe>
                <script>
                    var frame = document.getElementById('player');
                    function postCmd(func, args) {
                        try {
                            if (frame && frame.contentWindow) {
                                frame.contentWindow.postMessage(JSON.stringify({
                                    "event": "command",
                                    "func": func,
                                    "args": args || []
                                }), "*");
                            }
                        } catch(e) {}
                    }

                    function seekToTime(sec) {
                        postCmd("seekTo", [sec, true]);
                    }

                    function setPlaying(play) {
                        if (play) {
                            postCmd("playVideo", []);
                        } else {
                            postCmd("pauseVideo", []);
                        }
                    }

                    window.onload = function() {
                        postCmd("mute", []);
                        if ($initialPlaying) {
                            postCmd("playVideo", []);
                        } else {
                            postCmd("pauseVideo", []);
                        }
                    };
                </script>
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
                    * { margin: 0; padding: 0; }
                    body, html { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                    video { width: 100%; height: 100%; object-fit: contain; }
                </style>
            </head>
            <body>
                <video id="vid" src="$videoUrl" autoplay muted playsinline></video>
                <script>
                    var vid = document.getElementById('vid');
                    if (vid) {
                        vid.currentTime = $initialStartSec;
                        if (!$initialPlaying) vid.pause();
                    }
                    function seekToTime(sec) {
                        if (vid) vid.currentTime = sec;
                    }
                    function setPlaying(play) {
                        if (!vid) return;
                        if (play) vid.play(); else vid.pause();
                    }
                </script>
            </body>
            </html>
            """.trimIndent()
        } else ""

        if (htmlContent.isNotBlank()) {
            webView.loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlContent, "text/html", "UTF-8", null)
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
            .background(androidx.compose.ui.graphics.Color.Black)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
        // Transparent touch-blocking overlay: ALL gestures on the video surface are consumed here.
        // Seek / play-pause must be driven ONLY by the bottom control strip.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        )
    }
}
