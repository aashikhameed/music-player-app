package com.aashik.music.wear.storage

import android.os.Environment
import android.os.StatFs
import java.util.Locale

object WearStorageHelper {

    data class StorageInfo(
        val freeBytes: Long,
        val totalBytes: Long,
        val formattedFree: String,
        val formattedTotal: String,
        val isLowStorage: Boolean
    )

    fun getWatchStorageInfo(): StorageInfo {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong

            val freeBytes = availableBlocks * blockSize
            val totalBytes = totalBlocks * blockSize
            val isLow = freeBytes < (500L * 1024L * 1024L) // Warning threshold: < 500 MB

            StorageInfo(
                freeBytes = freeBytes,
                totalBytes = totalBytes,
                formattedFree = formatBytes(freeBytes),
                formattedTotal = formatBytes(totalBytes),
                isLowStorage = isLow
            )
        } catch (e: Exception) {
            StorageInfo(
                freeBytes = 0L,
                totalBytes = 0L,
                formattedFree = "--",
                formattedTotal = "--",
                isLowStorage = false
            )
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            val gb = mb / 1024.0
            String.format(Locale.US, "%.1f GB", gb)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
