package com.aashik.music.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aashik.music.viewmodel.MusicViewModel

@SuppressLint("SetJavaScriptEnabled", "MissingPermission")
@Composable
fun NavigationMapView(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapLoading by remember { mutableStateOf(false) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Real-time GPS location tracking
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                userLat = location.latitude
                userLng = location.longitude
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastGps != null) {
                userLat = lastGps.latitude
                userLng = lastGps.longitude
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                5f,
                locationListener
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000L,
                5f,
                locationListener
            )
        } catch (_: Exception) {}

        onDispose {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (_: Exception) {}
        }
    }

    val safeDarkThemeScript = """
        (function() {
            function applyThemeAndClean() {
                try {
                    // Click 'Keep using web' / 'Stay on web' / 'Use web' automatically
                    var allElements = document.querySelectorAll('button, a, span, div');
                    for (var i = 0; i < allElements.length; i++) {
                        var text = (allElements[i].innerText || allElements[i].textContent || '').trim().toLowerCase();
                        if (text === 'keep using web' || text === 'stay on web' || text === 'use web') {
                            allElements[i].click();
                        }
                    }

                    // Remove all 'Open app' web buttons, promo banners, and Google logo watermarks
                    var selectors = [
                        'a[href*="maps.app"]',
                        'a[href*="google.com/maps/dir"]',
                        '[aria-label*="Open app"]',
                        'button[jsaction*="app_banner"]',
                        'div[data-id="app-banner"]',
                        '.app-banner',
                        'div[role="dialog"]',
                        'div[aria-modal="true"]',
                        'header button',
                        'header a',
                        'button[aria-label*="Open in Maps"]',
                        'div.gm-style-cc',
                        'a[title*="Google Maps"]',
                        'div[class*="watermark"]',
                        'a[href*="maps.google.com/maps"]',
                        'div[class*="omnibox-header-action"]',
                        'button[class*="app-banner"]'
                    ];

                    var targets = document.querySelectorAll(selectors.join(', '));
                    for (var j = 0; j < targets.length; j++) {
                        targets[j].style.setProperty('display', 'none', 'important');
                    }

                    var target = document.head || document.documentElement;
                    if (!target) return;
                    var style = document.getElementById('night-mode-style');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'night-mode-style';
                        style.type = 'text/css';
                        target.appendChild(style);
                    }

                    style.textContent = `
                        /* Completely remove Open App buttons, headers, and Google watermarks */
                        [class*="app-banner"], [aria-label*="Open app"], a[href*="app.goo.gl"], a[href*="maps.app"],
                        div[role="dialog"], div[aria-modal="true"], header button, header a, div[data-ved] button,
                        button[jsaction*="app_banner"], div[data-id="app-banner"], div.gm-style-cc, 
                        a[title*="Google Maps"], .gm-bundled-control, div[class*="watermark"],
                        a[href*="maps.google.com/maps"], div[class*="logo"], div[aria-label*="Google"],
                        button[aria-label*="Open"], a[aria-label*="Open"], [data-value*="open_app"],
                        .omnibox-header-action, .widget-pane-visible header, div[id*="omnibox"] button:not([aria-label*="Search"]) {
                            display: none !important;
                            visibility: hidden !important;
                            opacity: 0 !important;
                            pointer-events: none !important;
                        }

                        /* Make native map search box sleek and automotive rounded */
                        input {
                            border-radius: 12px !important;
                        }

                        ${if (isDarkTheme) """
                            html, body {
                                filter: invert(90%) hue-rotate(180deg) !important;
                                background-color: #121212 !important;
                            }
                            img, video, canvas, [role="button"] img, svg {
                                filter: invert(100%) hue-rotate(180deg) !important;
                            }
                        """ else ""}
                    `;
                } catch (e) {}
            }

            applyThemeAndClean();
            if (!window.__cleanInterval) {
                window.__cleanInterval = setInterval(applyThemeAndClean, 200);
            }
        })();
    """.trimIndent()

    LaunchedEffect(isDarkTheme) {
        webViewInstance?.evaluateJavascript(safeDarkThemeScript, null)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-Bleed Map Canvas (Kept stable across recompositions to avoid flickering)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setGeolocationEnabled(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowContentAccess = true
                            allowFileAccess = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: ""
                                if (url.startsWith("intent:") || url.startsWith("geo:") || url.startsWith("market:")) {
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isMapLoading = true
                                view?.evaluateJavascript(safeDarkThemeScript, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isMapLoading = false
                                view?.evaluateJavascript(safeDarkThemeScript, null)
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
                            }
                        }

                        val mapUrl = if (userLat != null && userLng != null) {
                            "https://www.google.com/maps/@$userLat,$userLng,15z"
                        } else {
                            "https://www.google.com/maps"
                        }
                        loadUrl(mapUrl)
                        webViewInstance = this
                    }
                },
                update = { view ->
                    // Smooth CSS injection without reloading or flickering
                    view.evaluateJavascript(safeDarkThemeScript, null)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top-Right Clean Close Button (Aligned cleanly inside top padding)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(top = 10.dp, end = 10.dp)
                    .align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Map",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Loading Indicator
            if (isMapLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
