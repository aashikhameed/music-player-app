package com.aashik.music.service

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure Kotlin, zero-dependency MP4/M4A metadata and artwork tagger.
 * Follows ISO Base Media File Format (ISO/IEC 14496-12) & Apple QuickTime specs.
 *
 * Designed for low-memory automotive hardware (<250 KB heap allocation):
 * - Parses and modifies only the 'moov' atom in RAM.
 * - Streams unchanged 'mdat' audio data directly to disk.
 * - Idempotently updates or inserts ilst tags (©nam, ©ART, ©alb, covr).
 * - Correctly shifts 32-bit (stco) and 64-bit (co64) chunk offsets.
 */
object Mp4TagEditor {

    private const val TAG = "Mp4TagEditor"
    private val TAGS_TO_REPLACE = setOf("\u00A9nam", "\u00A9ART", "\u00A9alb", "covr")

    /**
     * Embeds metadata (title, artist, album) and album artwork into an MP4/M4A audio file.
     * Returns true on success, false on failure or if file is not a valid MP4 container.
     */
    fun embedMetadata(
        file: File,
        title: String?,
        artist: String?,
        album: String?,
        artBytes: ByteArray?
    ): Boolean {
        if (!file.exists() || file.length() < 32L) return false

        val tempFile = File(file.parentFile, "${file.name}.tag_${System.currentTimeMillis()}.tmp")
        try {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length()

                // 1. Verify ftyp atom
                raf.seek(0)
                val ftypHeader = ByteArray(8)
                raf.readFully(ftypHeader)
                val ftypType = String(ftypHeader, 4, 4, Charsets.US_ASCII)
                if (ftypType != "ftyp") {
                    Log.w(TAG, "Not an MP4/M4A file (ftyp missing): ${file.name}")
                    return false
                }

                // 2. Scan top-level boxes to locate 'moov'
                var moovOffset = -1L
                var moovSize = -1L
                var pos = 0L

                while (pos < fileSize) {
                    raf.seek(pos)
                    val boxHead = ByteArray(8)
                    val read = raf.read(boxHead)
                    if (read < 8) break
                    val buf = ByteBuffer.wrap(boxHead).order(ByteOrder.BIG_ENDIAN)
                    var bSize = buf.int.toLong() and 0xFFFFFFFFL
                    val bType = String(boxHead, 4, 4, Charsets.US_ASCII)

                    if (bSize == 1L) {
                        val extHead = ByteArray(8)
                        raf.readFully(extHead)
                        bSize = ByteBuffer.wrap(extHead).order(ByteOrder.BIG_ENDIAN).long
                    } else if (bSize == 0L) {
                        bSize = fileSize - pos
                    }

                    if (bType == "moov") {
                        moovOffset = pos
                        moovSize = bSize
                        break
                    }
                    if (bSize <= 0) break
                    pos += bSize
                }

                if (moovOffset < 0 || moovSize <= 8 || moovSize > 30_000_000L) {
                    Log.w(TAG, "moov box not found or invalid in ${file.name}")
                    return false
                }

                // 3. Read moov atom into memory
                raf.seek(moovOffset)
                val moovBytes = ByteArray(moovSize.toInt())
                raf.readFully(moovBytes)

                // 4. Build new ilst items (tags + art)
                val newItems = buildIlstItems(title, artist, album, artBytes)
                if (newItems.isEmpty()) return false

                // 5. Update moov atom with new ilst tags and shifted chunk offsets
                val (updatedMoovBytes, _, _) = updateMoovAtom(
                    moovBytes = moovBytes,
                    moovOffset = moovOffset,
                    newItems = newItems
                ) ?: return false

                // 6. Stream write updated file to temp file
                FileOutputStream(tempFile).use { fos ->
                    // A. Stream bytes before moov
                    if (moovOffset > 0) {
                        raf.seek(0)
                        streamCopy(raf, fos, moovOffset)
                    }
                    // B. Write updated moov
                    fos.write(updatedMoovBytes)
                    // C. Stream bytes after moov
                    val afterMoovPos = moovOffset + moovSize
                    if (afterMoovPos < fileSize) {
                        raf.seek(afterMoovPos)
                        streamCopy(raf, fos, fileSize - afterMoovPos)
                    }
                    fos.flush()
                }
            }

            // 7. Atomically replace original file with tempFile
            if (tempFile.exists() && tempFile.length() > 20_000L) {
                val origName = file.name
                val destBackup = File(file.parentFile, "$origName.bak_${System.currentTimeMillis()}")
                if (file.renameTo(destBackup)) {
                    if (tempFile.renameTo(file)) {
                        destBackup.delete()
                        return true
                    } else {
                        destBackup.renameTo(file)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed embedding MP4 metadata for ${file.name}: ${e.message}", e)
        } finally {
            try { if (tempFile.exists()) tempFile.delete() } catch (_: Exception) {}
        }
        return false
    }

    private data class MoovUpdateResult(
        val updatedMoovBytes: ByteArray,
        val delta: Int,
        val absInsertPos: Long
    )

    private fun updateMoovAtom(
        moovBytes: ByteArray,
        moovOffset: Long,
        newItems: ByteArray
    ): MoovUpdateResult? {
        val moovBuf = ByteBuffer.wrap(moovBytes).order(ByteOrder.BIG_ENDIAN)
        val origMoovLen = moovBuf.int
        if (origMoovLen != moovBytes.size) return null

        // Find udta inside moov
        var udtaPos = -1
        var pos = 8
        while (pos + 8 <= moovBytes.size) {
            val sz = ByteBuffer.wrap(moovBytes, pos, 4).order(ByteOrder.BIG_ENDIAN).int
            val tag = String(moovBytes, pos + 4, 4, Charsets.US_ASCII)
            if (tag == "udta") {
                udtaPos = pos
                break
            }
            if (sz <= 0) break
            pos += sz
        }

        var delta: Int
        var absInsertPos: Long
        var updatedMoov: ByteArray

        if (udtaPos != -1) {
            val udtaLen = ByteBuffer.wrap(moovBytes, udtaPos, 4).order(ByteOrder.BIG_ENDIAN).int
            val udtaEnd = udtaPos + udtaLen

            // Find meta inside udta
            var metaPos = -1
            pos = udtaPos + 8
            while (pos + 8 <= udtaEnd) {
                val sz = ByteBuffer.wrap(moovBytes, pos, 4).order(ByteOrder.BIG_ENDIAN).int
                val tag = String(moovBytes, pos + 4, 4, Charsets.US_ASCII)
                if (tag == "meta") {
                    metaPos = pos
                    break
                }
                if (sz <= 0) break
                pos += sz
            }

            if (metaPos != -1) {
                val metaLen = ByteBuffer.wrap(moovBytes, metaPos, 4).order(ByteOrder.BIG_ENDIAN).int
                val metaEnd = metaPos + metaLen

                // Find ilst inside meta (meta is FullBox: 4 size + 4 type + 4 version/flags = 12 bytes header)
                var ilstPos = -1
                pos = metaPos + 12
                while (pos + 8 <= metaEnd) {
                    val sz = ByteBuffer.wrap(moovBytes, pos, 4).order(ByteOrder.BIG_ENDIAN).int
                    val tag = String(moovBytes, pos + 4, 4, Charsets.ISO_8859_1)
                    if (tag == "ilst") {
                        ilstPos = pos
                        break
                    }
                    if (sz <= 0) break
                    pos += sz
                }

                if (ilstPos != -1) {
                    val ilstLen = ByteBuffer.wrap(moovBytes, ilstPos, 4).order(ByteOrder.BIG_ENDIAN).int
                    val oldPayloadStart = ilstPos + 8

                    // Filter existing tags to replace
                    val keptPayload = java.io.ByteArrayOutputStream()
                    var p = oldPayloadStart
                    while (p + 8 <= ilstPos + ilstLen) {
                        val itemSz = ByteBuffer.wrap(moovBytes, p, 4).order(ByteOrder.BIG_ENDIAN).int
                        val itemTag = String(moovBytes, p + 4, 4, Charsets.ISO_8859_1)
                        if (itemSz <= 0) break
                        if (itemTag !in TAGS_TO_REPLACE) {
                            keptPayload.write(moovBytes, p, itemSz)
                        }
                        p += itemSz
                    }

                    val newPayload = keptPayload.toByteArray() + newItems
                    val newIlstBox = ByteBuffer.allocate(8 + newPayload.size).order(ByteOrder.BIG_ENDIAN).apply {
                        putInt(8 + newPayload.size)
                        put("ilst".toByteArray(Charsets.ISO_8859_1))
                        put(newPayload)
                    }.array()

                    delta = newIlstBox.size - ilstLen
                    absInsertPos = moovOffset + ilstPos + 8

                    val out = java.io.ByteArrayOutputStream(moovBytes.size + delta)
                    out.write(moovBytes, 0, ilstPos)
                    out.write(newIlstBox)
                    out.write(moovBytes, ilstPos + ilstLen, moovBytes.size - (ilstPos + ilstLen))
                    updatedMoov = out.toByteArray()

                    // Update meta and udta lengths
                    updateBoxLength(updatedMoov, metaPos, metaLen + delta)
                    updateBoxLength(updatedMoov, udtaPos, udtaLen + delta)
                } else {
                    // ilst missing: create ilst inside meta
                    val newIlstBox = ByteBuffer.allocate(8 + newItems.size).order(ByteOrder.BIG_ENDIAN).apply {
                        putInt(8 + newItems.size)
                        put("ilst".toByteArray(Charsets.ISO_8859_1))
                        put(newItems)
                    }.array()

                    delta = newIlstBox.size
                    absInsertPos = moovOffset + metaEnd

                    val out = java.io.ByteArrayOutputStream(moovBytes.size + delta)
                    out.write(moovBytes, 0, metaEnd)
                    out.write(newIlstBox)
                    out.write(moovBytes, metaEnd, moovBytes.size - metaEnd)
                    updatedMoov = out.toByteArray()

                    updateBoxLength(updatedMoov, metaPos, metaLen + delta)
                    updateBoxLength(updatedMoov, udtaPos, udtaLen + delta)
                }
            } else {
                // meta missing: create meta + ilst inside udta
                val ilstBox = ByteBuffer.allocate(8 + newItems.size).order(ByteOrder.BIG_ENDIAN).apply {
                    putInt(8 + newItems.size)
                    put("ilst".toByteArray(Charsets.ISO_8859_1))
                    put(newItems)
                }.array()

                val metaBox = ByteBuffer.allocate(12 + ilstBox.size).order(ByteOrder.BIG_ENDIAN).apply {
                    putInt(12 + ilstBox.size)
                    put("meta".toByteArray(Charsets.ISO_8859_1))
                    putInt(0) // FullBox version + flags
                    put(ilstBox)
                }.array()

                delta = metaBox.size
                absInsertPos = moovOffset + udtaEnd

                val out = java.io.ByteArrayOutputStream(moovBytes.size + delta)
                out.write(moovBytes, 0, udtaEnd)
                out.write(metaBox)
                out.write(moovBytes, udtaEnd, moovBytes.size - udtaEnd)
                updatedMoov = out.toByteArray()

                updateBoxLength(updatedMoov, udtaPos, udtaLen + delta)
            }
        } else {
            // udta missing: create udta + meta + ilst inside moov
            val ilstBox = ByteBuffer.allocate(8 + newItems.size).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(8 + newItems.size)
                put("ilst".toByteArray(Charsets.ISO_8859_1))
                put(newItems)
            }.array()

            val metaBox = ByteBuffer.allocate(12 + ilstBox.size).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(12 + ilstBox.size)
                put("meta".toByteArray(Charsets.ISO_8859_1))
                putInt(0)
                put(ilstBox)
            }.array()

            val udtaBox = ByteBuffer.allocate(8 + metaBox.size).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(8 + metaBox.size)
                put("udta".toByteArray(Charsets.ISO_8859_1))
                put(metaBox)
            }.array()

            delta = udtaBox.size
            absInsertPos = moovOffset + moovBytes.size

            val out = java.io.ByteArrayOutputStream(moovBytes.size + delta)
            out.write(moovBytes)
            out.write(udtaBox)
            updatedMoov = out.toByteArray()
        }

        // Update overall moov length
        updateBoxLength(updatedMoov, 0, origMoovLen + delta)

        // Adjust all stco (32-bit) chunk offsets in updatedMoov
        adjustStcoOffsets(updatedMoov, absInsertPos, delta)

        // Adjust all co64 (64-bit) chunk offsets in updatedMoov
        adjustCo64Offsets(updatedMoov, absInsertPos, delta)

        return MoovUpdateResult(updatedMoov, delta, absInsertPos)
    }

    private fun updateBoxLength(bytes: ByteArray, offset: Int, newLength: Int) {
        val buf = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(newLength)
    }

    private fun adjustStcoOffsets(bytes: ByteArray, absInsertPos: Long, delta: Int) {
        var idx = 0
        val target = "stco".toByteArray(Charsets.ISO_8859_1)
        while (true) {
            val found = indexOf(bytes, target, idx)
            if (found == -1) break
            val stcoBoxPos = found - 4
            if (stcoBoxPos >= 0 && stcoBoxPos + 16 <= bytes.size) {
                val entryCount = ByteBuffer.wrap(bytes, stcoBoxPos + 12, 4).order(ByteOrder.BIG_ENDIAN).int
                var entryPos = stcoBoxPos + 16
                var i = 0
                while (i < entryCount && entryPos + 4 <= bytes.size) {
                    val off = ByteBuffer.wrap(bytes, entryPos, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL
                    if (off > absInsertPos) {
                        val newOff = (off + delta).toInt()
                        ByteBuffer.wrap(bytes, entryPos, 4).order(ByteOrder.BIG_ENDIAN).putInt(newOff)
                    }
                    entryPos += 4
                    i++
                }
            }
            idx = found + 4
        }
    }

    private fun adjustCo64Offsets(bytes: ByteArray, absInsertPos: Long, delta: Int) {
        var idx = 0
        val target = "co64".toByteArray(Charsets.ISO_8859_1)
        while (true) {
            val found = indexOf(bytes, target, idx)
            if (found == -1) break
            val co64BoxPos = found - 4
            if (co64BoxPos >= 0 && co64BoxPos + 16 <= bytes.size) {
                val entryCount = ByteBuffer.wrap(bytes, co64BoxPos + 12, 4).order(ByteOrder.BIG_ENDIAN).int
                var entryPos = co64BoxPos + 16
                var i = 0
                while (i < entryCount && entryPos + 8 <= bytes.size) {
                    val off = ByteBuffer.wrap(bytes, entryPos, 8).order(ByteOrder.BIG_ENDIAN).long
                    if (off > absInsertPos) {
                        val newOff = off + delta
                        ByteBuffer.wrap(bytes, entryPos, 8).order(ByteOrder.BIG_ENDIAN).putLong(newOff)
                    }
                    entryPos += 8
                    i++
                }
            }
            idx = found + 4
        }
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (target.isEmpty() || fromIndex >= source.size) return -1
        val max = source.size - target.size
        for (i in fromIndex..max) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun buildIlstItems(
        title: String?,
        artist: String?,
        album: String?,
        artBytes: ByteArray?
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        if (!title.isNullOrBlank()) {
            writeIlstItem(out, "\u00A9nam", 1, title.toByteArray(Charsets.UTF_8))
        }
        if (!artist.isNullOrBlank() && artist != "<unknown>") {
            writeIlstItem(out, "\u00A9ART", 1, artist.toByteArray(Charsets.UTF_8))
        }
        if (!album.isNullOrBlank() && album != "<unknown>") {
            writeIlstItem(out, "\u00A9alb", 1, album.toByteArray(Charsets.UTF_8))
        }
        if (artBytes != null && artBytes.isNotEmpty()) {
            val isPng = artBytes.size > 8 &&
                    artBytes[0] == 0x89.toByte() && artBytes[1] == 0x50.toByte() &&
                    artBytes[2] == 0x4E.toByte() && artBytes[3] == 0x47.toByte()
            val typeInd = if (isPng) 14 else 13
            writeIlstItem(out, "covr", typeInd, artBytes)
        }
        return out.toByteArray()
    }

    private fun writeIlstItem(
        out: java.io.ByteArrayOutputStream,
        fourcc: String,
        typeIndicator: Int,
        payload: ByteArray
    ) {
        val dataBoxSize = 16 + payload.size
        val itemBoxSize = 8 + dataBoxSize

        val header = ByteBuffer.allocate(8 + 16).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(itemBoxSize)
            put(fourcc.toByteArray(Charsets.ISO_8859_1))
            putInt(dataBoxSize)
            put("data".toByteArray(Charsets.ISO_8859_1))
            putInt(typeIndicator)
            putInt(0) // locale
        }

        out.write(header.array())
        out.write(payload)
    }

    private fun streamCopy(raf: RandomAccessFile, fos: FileOutputStream, totalBytes: Long) {
        val buffer = ByteArray(65536)
        var remaining = totalBytes
        while (remaining > 0) {
            val toRead = minOf(remaining, buffer.size.toLong()).toInt()
            val read = raf.read(buffer, 0, toRead)
            if (read <= 0) break
            fos.write(buffer, 0, read)
            remaining -= read
        }
    }
}
