package com.aashik.music.service

import com.aashik.music.utils.b64u

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val apkDownloadUrl: String,
        val apkSize: Long
    ) : UpdateState
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState
    data class ReadyToInstall(
        val apkFile: File,
        val version: String
    ) : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private val GITHUB_REPO_API = b64u("aHR0cHM6Ly9hcGkuZ2l0aHViLmNvbS9yZXBvcy9hYXNoaWtoYW1lZWQvbXVzaWMtcGxheWVyLWFwcC9yZWxlYXNlcy9sYXRlc3Q=")

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var cachedUpdate: UpdateState.UpdateAvailable? = null

    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "0.2.4"
        } catch (_: Exception) {
            "0.2.4"
        }
    }

    fun getInstalledVersionCode(context: Context): Long {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            72L
        }
    }

    /**
     * Checks GitHub for the latest release.
     */
    suspend fun checkForUpdates(context: Context, silent: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        if (!silent) {
            _updateState.value = UpdateState.Checking
        }

        try {
            val url = URL(GITHUB_REPO_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "CarMusicPlayer-Android")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                val err = "GitHub API response: ${conn.responseCode}"
                Log.w(TAG, err)
                val state = UpdateState.Error(err)
                if (!silent) _updateState.value = state
                return@withContext state
            }

            val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(jsonString)
            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName).trim()
            val releaseNotes = json.optString("body", "").trim()

            // Find APK asset
            var apkUrl: String? = null
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        val downloadUrl = asset.optString("browser_download_url", "")
                        if (downloadUrl.isNotBlank()) {
                            apkUrl = downloadUrl
                            apkSize = asset.optLong("size", 0L)
                            // Prefer car-music.apk
                            if (assetName.equals("car-music.apk", ignoreCase = true)) {
                                break
                            }
                        }
                    }
                }
            }

            val remoteVersion = tagName.removePrefix("v").trim()
            val currentVersion = getInstalledVersionName(context).removePrefix("v").trim()

            val hasUpdate = isNewerVersion(remoteVersion, currentVersion) && !apkUrl.isNullOrBlank()

            val state = if (hasUpdate && apkUrl != null) {
                val update = UpdateState.UpdateAvailable(
                    latestVersion = remoteVersion,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    apkSize = apkSize
                )
                cachedUpdate = update
                update
            } else {
                UpdateState.UpToDate(currentVersion = currentVersion)
            }

            _updateState.value = state
            return@withContext state
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates: ${e.message}", e)
            val state = UpdateState.Error("Network error: ${e.localizedMessage ?: "Unknown"}")
            if (!silent) _updateState.value = state
            return@withContext state
        }
    }

    /**
     * Downloads the APK file with real-time percentage progress updates.
     */
    suspend fun downloadUpdate(context: Context, downloadUrl: String, version: String): File? = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "car-music-$version.apk")

        if (targetFile.exists() && targetFile.length() > 1024 * 1024) {
            // Already downloaded
            _updateState.value = UpdateState.ReadyToInstall(targetFile, version)
            return@withContext targetFile
        }

        try {
            _updateState.value = UpdateState.Downloading(progressPercent = 0, downloadedBytes = 0L, totalBytes = 0L)

            val conn = openConnectionWithRedirects(downloadUrl)
            val totalBytes = conn.contentLength.toLong().takeIf { it > 0 } ?: 4 * 1024 * 1024L
            var downloadedBytes = 0L

            val buffer = ByteArray(8192)
            val input: InputStream = conn.inputStream
            val output = FileOutputStream(targetFile)

            var lastReportPercent = -1

            input.use { inStream ->
                output.use { outStream ->
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99) else 0
                        if (percent != lastReportPercent) {
                            lastReportPercent = percent
                            _updateState.value = UpdateState.Downloading(
                                progressPercent = percent,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        }
                    }
                }
            }
            conn.disconnect()

            _updateState.value = UpdateState.ReadyToInstall(targetFile, version)
            return@withContext targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            _updateState.value = UpdateState.Error("Download failed: ${e.message}")
            if (targetFile.exists()) targetFile.delete()
            return@withContext null
        }
    }

    /**
     * Launches Android's Package Installer for the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Install intent error: ${e.message}", e)
            _updateState.value = UpdateState.Error("Install failed: ${e.message}")
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Follows HTTP 301/302/307/308 redirects (GitHub assets redirect to AWS S3).
     */
    private fun openConnectionWithRedirects(initialUrl: String, maxRedirects: Int = 6): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0

        while (redirects < maxRedirects) {
            val url = URL(currentUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("User-Agent", "CarMusicPlayer-Android")
            }

            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308
            ) {
                val newUrl = conn.getHeaderField("Location") ?: return conn
                conn.disconnect()
                currentUrl = newUrl
                redirects++
            } else {
                return conn
            }
        }
        return (URL(currentUrl).openConnection() as HttpURLConnection)
    }

    /**
     * Compares two semantic version strings (e.g. "0.2.4" vs "0.2.5").
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
