package com.aashik.music.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Data model for destination info
data class WebMapDestination(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float
)

// Data model for active in-app navigation guidance
data class InAppNavigationRoute(
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val nextManeuverType: String,
    val nextManeuverModifier: String,
    val nextStreetName: String,
    val nextStepDistanceMeters: Double
)

/**
 * 100% In-App Interactive Topographic & Terrain Navigation Map for Automotive Cockpit.
 * Realtime GPS tracking, Terrain Elevation View, Zero API keys needed!
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NavigationMapView(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    isMapOpen: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<WebMapDestination?>(null) }
    var activeNavRoute by remember { mutableStateOf<InAppNavigationRoute?>(null) }
    var isNavigatingActive by remember { mutableStateOf(false) }

    var currentSpeedKmH by remember { mutableIntStateOf(0) }
    var currentBearing by remember { mutableFloatStateOf(0f) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(isMapOpen) {
        if (isMapOpen) {
            webViewRef?.evaluateJavascript("if(window.invalidateMapSize) window.invalidateMapSize();", null)
            kotlinx.coroutines.delay(150)
            webViewRef?.evaluateJavascript("if(window.centerUser) window.centerUser();", null)
        }
    }

    // Auto-request location permissions if not granted
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            kotlinx.coroutines.delay(2500)
            isSearching = false
        }
    }

    // High-Accuracy Real-Time Continuous GPS Tracking via FusedLocationProviderClient
    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(250L)
            .setMinUpdateDistanceMeters(0.5f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLoc = locationResult.lastLocation ?: return
                userLocation = lastLoc

                if (lastLoc.hasSpeed()) {
                    currentSpeedKmH = (lastLoc.speed * 3.6f).roundToInt()
                } else {
                    currentSpeedKmH = 0
                }

                if (lastLoc.hasBearing()) {
                    currentBearing = lastLoc.bearing
                }

                // Push realtime location to WebView Leaflet map
                webViewRef?.evaluateJavascript(
                    "if(window.updateLocation){ window.updateLocation(${lastLoc.latitude}, ${lastLoc.longitude}, ${currentBearing}, ${isNavigatingActive}); }",
                    null
                )
            }
        }

        try {
            // Immediate GPS coordinate fetch
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        userLocation = loc
                        if (loc.hasSpeed()) currentSpeedKmH = (loc.speed * 3.6f).roundToInt()
                        if (loc.hasBearing()) currentBearing = loc.bearing
                        webViewRef?.evaluateJavascript(
                            "if(window.updateLocation){ window.updateLocation(${loc.latitude}, ${loc.longitude}, ${currentBearing}, true); }",
                            null
                        )
                    }
                }

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && userLocation == null) {
                    userLocation = loc
                    if (loc.hasSpeed()) currentSpeedKmH = (loc.speed * 3.6f).roundToInt()
                    if (loc.hasBearing()) currentBearing = loc.bearing
                    webViewRef?.evaluateJavascript(
                        "if(window.updateLocation){ window.updateLocation(${loc.latitude}, ${loc.longitude}, ${currentBearing}, true); }",
                        null
                    )
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {}

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Update Theme in Map when user toggles Day/Night
    LaunchedEffect(isDarkTheme) {
        webViewRef?.evaluateJavascript("if(window.setMapTheme){ window.setMapTheme($isDarkTheme); }", null)
    }

    // Search Location logic via Chromium WebView Fetch (Zero TLS issues, Zero API key)
    fun executeSearch(query: String) {
        if (query.isBlank()) return
        focusManager.clearFocus()
        keyboardController?.hide()
        isSearching = true

        val userLat = userLocation?.latitude ?: 0.0
        val userLng = userLocation?.longitude ?: 0.0
        val safeQuery = query.trim().replace("'", "\\'")

        webViewRef?.evaluateJavascript(
            "if(window.searchLocation){ window.searchLocation('$safeQuery', $userLat, $userLng); }",
            null
        )
    }

    // Start 100% In-App Turn-By-Turn Navigation Guidance
    fun startInAppNavigation(dest: WebMapDestination) {
        isNavigatingActive = true
        val safeTitle = dest.title.replace("'", "\\'")
        webViewRef?.evaluateJavascript(
            "if(window.startTurnByTurn){ window.startTurnByTurn(${dest.latitude}, ${dest.longitude}, '$safeTitle'); }",
            null
        )
    }

    // Stop In-App Navigation Guidance
    fun stopInAppNavigation() {
        isNavigatingActive = false
        activeNavRoute = null
        selectedDestination = null
        searchQuery = ""
        webViewRef?.evaluateJavascript("if(window.endNavigation){ window.endNavigation(); }", null)
    }

    // Javascript Interface Bridge for WebView Map
    val mapBridge = remember {
        object {
            @android.webkit.JavascriptInterface
            fun onDestinationFound(title: String, address: String, lat: Double, lng: Double) {
                coroutineScope.launch(Dispatchers.Main) {
                    isSearching = false
                    var distance = 0f
                    userLocation?.let { current ->
                        val distArray = FloatArray(1)
                        Location.distanceBetween(
                            current.latitude, current.longitude,
                            lat, lng,
                            distArray
                        )
                        distance = distArray[0]
                    }

                    selectedDestination = WebMapDestination(
                        title = title,
                        address = address,
                        latitude = lat,
                        longitude = lng,
                        distanceMeters = distance
                    )
                }
            }

            @android.webkit.JavascriptInterface
            fun onRouteCalculated(
                distance: Double,
                duration: Double,
                manType: String,
                manMod: String,
                streetName: String,
                stepDist: Double,
                startActiveNav: Boolean
            ) {
                coroutineScope.launch(Dispatchers.Main) {
                    activeNavRoute = InAppNavigationRoute(
                        totalDistanceMeters = distance,
                        totalDurationSeconds = duration,
                        nextManeuverType = manType,
                        nextManeuverModifier = manMod,
                        nextStreetName = streetName,
                        nextStepDistanceMeters = stepDist
                    )
                    if (startActiveNav) {
                        isNavigatingActive = true
                    }
                }
            }

            @android.webkit.JavascriptInterface
            fun onSearchError(error: String) {
                coroutineScope.launch(Dispatchers.Main) {
                    isSearching = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val mapCardShape = RoundedCornerShape(16.dp)
    val mapCardGrad = AppGradients.card(isActive = false)
    val mapCardBorder = AppGradients.border(isActive = false)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(mapCardShape)
            .background(brush = mapCardGrad, shape = mapCardShape)
            .border(border = BorderStroke(1.dp, mapCardBorder), shape = mapCardShape)
    ) {
        // =========================================================================
        // 1. FREE LOCAL LEAFLET WEBVIEW MAP (Zero API Key Needed!)
        // =========================================================================
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setGeolocationEnabled(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    addJavascriptInterface(mapBridge, "AndroidBridge")
                    webChromeClient = object : WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String,
                            callback: GeolocationPermissions.Callback
                        ) {
                            callback.invoke(origin, true, false)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            userLocation?.let { loc ->
                                view?.evaluateJavascript(
                                    "if(window.updateLocation){ window.updateLocation(${loc.latitude}, ${loc.longitude}, ${currentBearing}, true); }",
                                    null
                                )
                            }
                            view?.evaluateJavascript("if(window.setMapTheme){ window.setMapTheme($isDarkTheme); }", null)
                        }
                    }

                    val htmlContent = try {
                        ctx.assets.open("map/index.html").bufferedReader().use { it.readText() }
                    } catch (_: Exception) {
                        ""
                    }
                    loadDataWithBaseURL(
                        "https://tile.openstreetmap.org",
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )

        // =========================================================================
        // 2. TOP IN-APP TURN-BY-TURN MANEUVER HUD (Active Navigation Mode)
        // =========================================================================
        if (isNavigatingActive && activeNavRoute != null) {
            val route = activeNavRoute!!
            val maneuverShape = RoundedCornerShape(16.dp)
            val maneuverGrad = AppGradients.card(isActive = true)
            val maneuverBorder = AppGradients.border(isActive = true)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp)
                    .clip(maneuverShape)
                    .background(brush = maneuverGrad, shape = maneuverShape)
                    .border(border = BorderStroke(1.5.dp, maneuverBorder), shape = maneuverShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Turn Arrow Icon
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getManeuverIcon(route.nextManeuverType, route.nextManeuverModifier),
                            contentDescription = "Maneuver",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Turn Distance & Next Street
                    Column(modifier = Modifier.weight(1f)) {
                        val stepDistText = if (route.nextStepDistanceMeters >= 1000) {
                            String.format(Locale.getDefault(), "In %.1f km", route.nextStepDistanceMeters / 1000f)
                        } else {
                            "In ${route.nextStepDistanceMeters.roundToInt()} m"
                        }
                        Text(
                            text = stepDistText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = route.nextStreetName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // End Navigation Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFE53935), CircleShape)
                            .clickable { stopInAppNavigation() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "End Route",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // =====================================================================
            // 3. TOP INTEGRATED SEARCH PILL & MAP CONTROLS (Browsing Mode)
            // =====================================================================
            val searchPillShape = RoundedCornerShape(24.dp)
            val searchPillGrad = AppGradients.card(isActive = false)
            val searchPillBorder = AppGradients.border(isActive = false)

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(searchPillShape)
                        .background(brush = searchPillGrad, shape = searchPillShape)
                        .border(border = BorderStroke(1.dp, searchPillBorder), shape = searchPillShape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { executeSearch(searchQuery) }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search destination or address...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { executeSearch(searchQuery) }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedDestination = null
                                    activeNavRoute = null
                                    webViewRef?.evaluateJavascript("if(window.clearDestination){ window.clearDestination(); }", null)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Close Map Split Button
                val closeShape = CircleShape
                val closeGrad = AppGradients.card(isActive = false)
                val closeBorder = AppGradients.border(isActive = false)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(closeShape)
                        .background(brush = closeGrad, shape = closeShape)
                        .border(border = BorderStroke(1.dp, closeBorder), shape = closeShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Map",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Speedometer HUD Badge (Top-Left)
            val speedBadgeGrad = AppGradients.capsule(isActive = currentSpeedKmH > 0)
            val speedBorderGrad = AppGradients.border(isActive = currentSpeedKmH > 0)
            val speedShape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 72.dp)
                    .clip(speedShape)
                    .background(brush = speedBadgeGrad, shape = speedShape)
                    .border(border = BorderStroke(1.dp, speedBorderGrad), shape = speedShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "Speedometer",
                        tint = if (currentSpeedKmH > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$currentSpeedKmH",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // =========================================================================
        // 4. RECENTER MY LOCATION / FOLLOW VEHICLE FAB (Bottom-Right)
        // =========================================================================
        val fabShape = CircleShape
        val fabGrad = AppGradients.primaryButton()

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .size(48.dp)
                .clip(fabShape)
                .background(brush = fabGrad, shape = fabShape)
                .clickable {
                    webViewRef?.evaluateJavascript("if(window.centerUser){ window.centerUser(); }", null)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "Recenter My Location",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // =========================================================================
        // 5. BOTTOM IN-APP ACTIVE NAVIGATION STATUS COCKPIT (Active Nav Mode)
        // =========================================================================
        if (isNavigatingActive && activeNavRoute != null) {
            val route = activeNavRoute!!
            val activeCockpitShape = RoundedCornerShape(16.dp)
            val activeCockpitGrad = AppGradients.card(isActive = true)
            val activeCockpitBorder = AppGradients.border(isActive = true)

            val etaMinutes = (route.totalDurationSeconds / 60.0).roundToInt()
            val etaMillis = System.currentTimeMillis() + (route.totalDurationSeconds * 1000).toLong()
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val etaTimeString = timeFormat.format(Date(etaMillis))

            val distText = if (route.totalDistanceMeters >= 1000) {
                String.format(Locale.getDefault(), "%.1f km", route.totalDistanceMeters / 1000f)
            } else {
                "${route.totalDistanceMeters.roundToInt()} m"
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 70.dp, bottom = 14.dp)
                    .clip(activeCockpitShape)
                    .background(brush = activeCockpitGrad, shape = activeCockpitShape)
                    .border(border = BorderStroke(1.dp, activeCockpitBorder), shape = activeCockpitShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // ETA Time & Distance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$etaMinutes",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "min",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            Text(
                                text = "ETA $etaTimeString",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )

                        Column {
                            Text(
                                text = distText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$currentSpeedKmH km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Red Stop Route Button
                    Button(
                        onClick = { stopInAppNavigation() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            // =====================================================================
            // 6. DESTINATION DETAILS & "START IN-APP NAVIGATION" SLIDE-UP CARD
            // =====================================================================
            AnimatedVisibility(
                visible = selectedDestination != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 70.dp, bottom = 14.dp)
            ) {
                selectedDestination?.let { dest ->
                    val destShape = RoundedCornerShape(16.dp)
                    val destGrad = AppGradients.card(isActive = true)
                    val destBorder = AppGradients.border(isActive = true)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(destShape)
                            .background(brush = destGrad, shape = destShape)
                            .border(border = BorderStroke(1.dp, destBorder), shape = destShape)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = dest.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dest.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (dest.distanceMeters > 0) {
                                    val distanceText = if (dest.distanceMeters >= 1000) {
                                        String.format(Locale.getDefault(), "%.1f km", dest.distanceMeters / 1000f)
                                    } else {
                                        "${dest.distanceMeters.roundToInt()} m"
                                    }
                                    Text(
                                        text = distanceText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Navigation Action Buttons (100% In-App Navigation)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        startInAppNavigation(dest)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Navigation,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Navigation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedDestination = null
                                        activeNavRoute = null
                                        webViewRef?.evaluateJavascript("if(window.clearDestination){ window.clearDestination(); }", null)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns corresponding Compose Material icon for OSRM turn maneuvers
 */
private fun getManeuverIcon(type: String, modifier: String): ImageVector {
    val mod = modifier.lowercase()
    val t = type.lowercase()

    return when {
        t == "arrive" -> Icons.Rounded.Flag
        mod.contains("slight left") -> Icons.Rounded.TurnSlightLeft
        mod.contains("slight right") -> Icons.Rounded.TurnSlightRight
        mod.contains("left") -> Icons.Rounded.TurnLeft
        mod.contains("right") -> Icons.Rounded.TurnRight
        mod.contains("uturn") -> Icons.Rounded.AltRoute
        t == "roundabout" -> Icons.Rounded.Explore
        else -> Icons.Rounded.Straight
    }
}
