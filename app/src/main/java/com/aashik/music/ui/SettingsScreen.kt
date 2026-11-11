package com.aashik.music.ui

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.VolumeUp
import com.aashik.music.ui.designsystem.Button
import com.aashik.music.ui.designsystem.ButtonDefaults
import com.aashik.music.ui.designsystem.CircularProgressIndicator
import com.aashik.music.ui.designsystem.FilterChip
import com.aashik.music.ui.designsystem.FilterChipDefaults
import com.aashik.music.ui.designsystem.HorizontalDivider
import com.aashik.music.ui.designsystem.Icon
import com.aashik.music.ui.designsystem.IconButton
import com.aashik.music.ui.designsystem.MaterialTheme
import com.aashik.music.ui.designsystem.OutlinedTextField
import com.aashik.music.ui.designsystem.OutlinedTextFieldDefaults
import com.aashik.music.ui.designsystem.Slider
import com.aashik.music.ui.designsystem.SliderDefaults
import com.aashik.music.ui.designsystem.Surface
import com.aashik.music.ui.designsystem.Switch
import com.aashik.music.ui.designsystem.SwitchDefaults
import com.aashik.music.ui.designsystem.Text
import com.aashik.music.ui.designsystem.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.aashik.music.service.StreamDownloadManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel

import com.aashik.music.ui.components.AppUpdateSection
import com.aashik.music.ui.components.CacheManagementSection
import com.aashik.music.ui.components.DownloadStorageSection
import com.aashik.music.ui.components.EqualizerNavigationCard
import com.aashik.music.ui.components.LiquidGlassSettingsSection
import com.aashik.music.ui.components.PlaybackPreferencesSection
import com.aashik.music.ui.components.RefetchSection
import com.aashik.music.ui.components.SpeedCompensatedVolumeSection
import com.aashik.music.ui.components.SystemInfoSection
import com.aashik.music.ui.components.ThemeDisplaySection


// Precomputed shapes hoisted to top-level constants for Mali GPU fill rate & zero GC allocation
private val CardSectionShape = RoundedCornerShape(12.dp)
private val ChipPillShape = RoundedCornerShape(8.dp)
private val InfoBadgeShape = RoundedCornerShape(8.dp)
private val ActionButtonShape = RoundedCornerShape(8.dp)
private val DownloadOptionShape = RoundedCornerShape(12.dp)
private val PresetBadgeShape = RoundedCornerShape(4.dp)
private val CustomPathFieldShape = RoundedCornerShape(8.dp)
private val DialogContainerShape = RoundedCornerShape(28.dp)

private val ThemeSunColor = Color(0xFFFFD54F)
private val ThemeMoonColor = Color(0xFF7986CB)
private val RefetchGreenColor = Color(0xFF00E676)

private val CROSSFADE_OPTIONS = listOf(0, 1, 2, 3, 5, 8)

private val SLEEP_TIMER_OPTIONS = listOf(0, 15, 30, 45, 60)

private fun formatSleepTimer(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}

/**
 * Unified Automotive Settings Screen.
 * Consolidates Song Refetching, Lite/Dark Theme, Equalizer & DSP tuning, Cache Cleaning,
 * Playback Preferences, Grid Layout, and Hardware Diagnostics.
 * Optimized for Automotive Headunit (1280x720 @ 160 dpi).
 */
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isLandscape) {
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    } else {
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    }
                )
        ) {
            // ── Top Header Navigation Bar (Material 3 Standardized) ──
            AppTopBar(
                title = "Settings",
                onBack = onClose
            )

                Text(
                    text = "Automotive Audio & Headunit Preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )

                // ── Unified Single Scrollable Column (No Split Screen) ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EqualizerNavigationCard(
                        viewModel = viewModel,
                        onClick = onOpenEqualizer
                    )
                    SpeedCompensatedVolumeSection(viewModel = viewModel)
                    RefetchSection(viewModel = viewModel)
                    ThemeDisplaySection(viewModel = viewModel)
                    LiquidGlassSettingsSection(viewModel = viewModel)
                    PlaybackPreferencesSection(viewModel = viewModel)
                    DownloadStorageSection(viewModel = viewModel)
                    CacheManagementSection(viewModel = viewModel)
                    SystemInfoSection()
                    AppUpdateSection(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
