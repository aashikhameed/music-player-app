package com.aashik.music.ui.components

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aashik.music.service.StreamDownloadManager
import com.aashik.music.theme.AppGradients
import com.aashik.music.viewmodel.MusicViewModel
import kotlin.math.roundToInt

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

@Composable
fun DownloadLocationDialog(
    currentPreset: String,
    currentPath: String,
    onSelectPreset: (String, String?) -> Unit,
    onTestPath: (String) -> Pair<Boolean, String>,
    onDismiss: () -> Unit
) {
    var isCustomSelected by remember { mutableStateOf(currentPreset == StreamDownloadManager.PRESET_CUSTOM) }
    var customPathInput by remember { mutableStateOf(if (currentPreset == StreamDownloadManager.PRESET_CUSTOM) currentPath else "") }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val context = LocalContext.current
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            val path = uri.path ?: uri.toString()
            customPathInput = path
            isCustomSelected = true
            onSelectPreset(StreamDownloadManager.PRESET_CUSTOM, path)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = DialogContainerShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Download Storage Location",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose where offline audio files are stored. MediaStore is used on Android 10+ for public folders.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: Public Music
                StorageOptionItem(
                    title = "Public Music (Recommended)",
                    subtitle = "Music/MusicPlayer (Visible in all media players and PC transfers)",
                    badge = "System Library",
                    isSelected = !isCustomSelected && currentPreset == StreamDownloadManager.PRESET_PUBLIC_MUSIC,
                    onClick = {
                        isCustomSelected = false
                        onSelectPreset(StreamDownloadManager.PRESET_PUBLIC_MUSIC, null)
                    }
                )

                // Option 2: Public Downloads
                StorageOptionItem(
                    title = "Public Downloads",
                    subtitle = "Download/MusicPlayer (Standard system downloads folder)",
                    badge = "Downloads",
                    isSelected = !isCustomSelected && currentPreset == StreamDownloadManager.PRESET_PUBLIC_DOWNLOADS,
                    onClick = {
                        isCustomSelected = false
                        onSelectPreset(StreamDownloadManager.PRESET_PUBLIC_DOWNLOADS, null)
                    }
                )

                // Option 3: App Storage (Safe Mode)
                StorageOptionItem(
                    title = "App Storage (Safe Mode)",
                    subtitle = "Android/data/.../files/Music (100% reliable on all Android versions)",
                    badge = "Isolated",
                    isSelected = !isCustomSelected && currentPreset == StreamDownloadManager.PRESET_APP_INTERNAL,
                    onClick = {
                        isCustomSelected = false
                        onSelectPreset(StreamDownloadManager.PRESET_APP_INTERNAL, null)
                    }
                )

                // Option 4: Custom Directory
                StorageOptionItem(
                    title = "Custom Location...",
                    subtitle = "Specify a custom path on internal or SD card storage",
                    badge = "Custom",
                    isSelected = isCustomSelected,
                    onClick = {
                        isCustomSelected = true
                        if (customPathInput.isBlank()) {
                            customPathInput = currentPath
                        }
                    }
                )

                if (isCustomSelected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = DownloadOptionShape)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Enter Custom Directory Path:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = customPathInput,
                            onValueChange = {
                                customPathInput = it
                                testResult = null
                            },
                            singleLine = true,
                            shape = CustomPathFieldShape,
                            placeholder = { Text("/storage/emulated/0/Music", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        testResult?.let { (success, message) ->
                            Text(
                                text = message,
                                fontSize = 10.sp,
                                color = if (success) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { safLauncher.launch(null) },
                                shape = ActionButtonShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    contentColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = "Browse Storage Access Framework",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Browse (SAF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val trimmed = customPathInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        val res = onTestPath(trimmed)
                                        testResult = res
                                        if (res.first) {
                                            onSelectPreset(StreamDownloadManager.PRESET_CUSTOM, trimmed)
                                        }
                                    }
                                },
                                shape = ActionButtonShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Test & Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}


