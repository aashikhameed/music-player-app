package com.aashik.music.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aashik.music.viewmodel.MusicViewModel

// Precomputed shape constant strictly adhering to Automotive Mali GPU optimization guidelines
private val VideoContainerShape = RoundedCornerShape(16.dp)
private val WV_VERSION_REGEX = Regex("""Version/\d+\.\d+ """)

/**
 * Ultra-high-performance, zero-external-dependency Video & YouTube Stream Player.
 * Uses Android's hardware-accelerated WebView engine to stream unmuted YouTube and local music videos
 * directly within the Automotive Headunit and Mobile HUD without any APK size bloat.
 * 
 * Synchronizes video playback clock, play/pause states, and completion events directly with
 * MusicViewModel and MusicPlayer, driving all UI seeker/controls directly from the video engine.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    videoId: String?,
    videoUrl: String?,
    viewModel: MusicViewModel,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val maxQualityConfig by viewModel.maxVideoQuality.collectAsState()
    val ytQuality = when (maxQualityConfig) {
        "360p" -> "medium"
        "480p" -> "large"
        "1080p" -> "hd1080"
        else -> "hd720"
    }

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

    // Register lightweight JavaScript interface for video telemetry, state, and onEnded callbacks
    DisposableEffect(webView, viewModel) {
        val bridge = object {
            @JavascriptInterface
            fun onVideoProgress(currentTimeSec: Float, durationSec: Float) {
                val posMs = (currentTimeSec * 1000f).toLong()
                val durMs = (durationSec * 1000f).toLong()
                viewModel.onVideoTelemetry(posMs, durMs)
            }

            @JavascriptInterface
            fun onVideoStateChange(state: Int) {
                // YT.PlayerState: 1 = PLAYING, 2 = PAUSED, 3 = BUFFERING, 0 = ENDED
                when (state) {
                    1 -> viewModel.onVideoStateChange(true)
                    2 -> viewModel.onVideoStateChange(false)
                    0 -> viewModel.onVideoEnded()
                }
            }

            @JavascriptInterface
            fun onVideoEnded() {
                viewModel.onVideoEnded()
            }

            @JavascriptInterface
            fun onVideoError(errorCode: Int) {
                viewModel.onVideoError(errorCode)
            }
        }
        webView.addJavascriptInterface(bridge, "AndroidVideoBridge")
        viewModel.attachVideoPlayer(webView)

        onDispose {
            viewModel.detachVideoPlayer(webView)
            try {
                webView.removeJavascriptInterface("AndroidVideoBridge")
            } catch (_: Exception) {}
        }
    }

    // Sync play/pause changes with the video surface
    val isPlayingFlow = viewModel.isPlaying
    LaunchedEffect(isPlayingFlow) {
        isPlayingFlow.collect { playing ->
            webView.evaluateJavascript("if (typeof setPlaying === 'function') { setPlaying($playing); }", null)
        }
    }

    // Reload content whenever video target changes.
    // Newly loaded video always starts at 0s so seekbar resets and video plays from beginning.
    DisposableEffect(videoId, videoUrl, ytQuality) {
        val initialStartSec = 0L
        val initialPlaying = viewModel.isPlaying.value

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
                    #player { width: 100%; height: 100%; border: none; }
                    iframe { width: 100%; height: 100%; border: none; pointer-events: none; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script src="https://www.youtube.com/iframe_api"></script>
                <script>
                    var player = null;
                    var progressTimer = null;
                    var targetQuality = '$ytQuality';

                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            videoId: '$videoId',
                            host: 'https://www.youtube-nocookie.com',
                            playerVars: {
                                'autoplay': $initialPlaying ? 1 : 0,
                                'controls': 0,
                                'playsinline': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'enablejsapi': 1,
                                'origin': 'https://www.youtube-nocookie.com',
                                'start': $initialStartSec,
                                'suggestedQuality': targetQuality
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        try {
                            event.target.unMute();
                            event.target.setVolume(100);
                            if (targetQuality && typeof event.target.setPlaybackQuality === 'function') {
                                event.target.setPlaybackQuality(targetQuality);
                            }
                            if ($initialPlaying) {
                                event.target.playVideo();
                            }
                        } catch(e) {}
                        if (progressTimer) clearInterval(progressTimer);
                        progressTimer = setInterval(function() {
                            try {
                                if (player && typeof player.getCurrentTime === 'function' && typeof player.getPlayerState === 'function' && player.getPlayerState() === 1) {
                                    var cur = player.getCurrentTime() || 0;
                                    var dur = player.getDuration() || 0;
                                    if (window.AndroidVideoBridge) {
                                        window.AndroidVideoBridge.onVideoProgress(cur, dur);
                                    }
                                }
                            } catch(e) {}
                        }, 250);
                    }

                    function onPlayerStateChange(event) {
                        try {
                            if (event.data === 1 && targetQuality && player && typeof player.setPlaybackQuality === 'function') {
                                player.setPlaybackQuality(targetQuality);
                            }
                            if (window.AndroidVideoBridge) {
                                // YT.PlayerState: -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (cued)
                                window.AndroidVideoBridge.onVideoStateChange(event.data);
                            }
                        } catch(e) {}
                    }

                    function onPlayerError(event) {
                        try {
                            if (window.AndroidVideoBridge) {
                                window.AndroidVideoBridge.onVideoError(event.data || 0);
                            }
                        } catch(e) {}
                    }

                    function setPlaying(play) {
                        if (!player) return;
                        try {
                            if (play) {
                                player.unMute();
                                player.setVolume(100);
                                player.playVideo();
                            } else {
                                player.pauseVideo();
                            }
                        } catch(e) {}
                    }

                    function seekToTime(sec) {
                        if (!player) return;
                        try {
                            player.seekTo(sec, true);
                        } catch(e) {}
                    }

                    function seekToFraction(fraction) {
                        if (!player) return;
                        try {
                            var d = player.getDuration();
                            if (d > 0) {
                                player.seekTo(d * fraction, true);
                            }
                        } catch(e) {}
                    }
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
                <video id="vid" src="$videoUrl" autoplay playsinline></video>
                <script>
                    var vid = document.getElementById('vid');
                    if (vid) {
                        vid.currentTime = $initialStartSec;
                        vid.muted = false;
                        if (!$initialPlaying) vid.pause();
                        
                        vid.addEventListener('timeupdate', function() {
                            if (window.AndroidVideoBridge) {
                                window.AndroidVideoBridge.onVideoProgress(vid.currentTime, vid.duration || 0);
                            }
                        });
                        vid.addEventListener('play', function() {
                            if (window.AndroidVideoBridge) {
                                window.AndroidVideoBridge.onVideoStateChange(1);
                            }
                        });
                        vid.addEventListener('pause', function() {
                            if (window.AndroidVideoBridge) {
                                window.AndroidVideoBridge.onVideoStateChange(2);
                            }
                        });
                        vid.addEventListener('ended', function() {
                            if (window.AndroidVideoBridge) {
                                window.AndroidVideoBridge.onVideoEnded();
                            }
                        });
                    }

                    function seekToTime(sec) {
                        if (vid) vid.currentTime = sec;
                    }

                    function seekToFraction(fraction) {
                        if (vid && vid.duration) vid.currentTime = vid.duration * fraction;
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
        // Transparent touch-blocking overlay: consumes taps so WebView doesn't trigger web UI or open links,
        // and safely delegates taps to toggle HUD controls.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onTap) {
                    detectTapGestures(
                        onTap = {
                            onTap?.invoke()
                        }
                    )
                }
        )
    }
}
