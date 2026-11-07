package com.aashik.music

import com.aashik.music.service.Mp4TagEditor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Mp4TagEditorTest {

    @Test
    fun testInvalidFileReturnsFalse() {
        val temp = File.createTempFile("test_invalid", ".m4a")
        temp.writeBytes(ByteArray(10))
        val result = Mp4TagEditor.embedMetadata(temp, "Title", "Artist", "Album", null)
        assertFalse(result)
        temp.delete()
    }

    @Test
    fun testSyntheticMp4Tagging() {
        val temp = File.createTempFile("test_synth", ".m4a")
        // Build minimal MP4 with ftyp and moov
        val ftyp = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(32)
            put("ftyp".toByteArray(Charsets.ISO_8859_1))
            put("M4A ".toByteArray(Charsets.ISO_8859_1))
            putInt(0)
            put("isom".toByteArray(Charsets.ISO_8859_1))
            put("mp42".toByteArray(Charsets.ISO_8859_1))
            put(ByteArray(8))
        }.array()

        // Minimal moov with mvhd
        val mvhd = ByteBuffer.allocate(108).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(108)
            put("mvhd".toByteArray(Charsets.ISO_8859_1))
            put(ByteArray(100))
        }.array()

        val moov = ByteBuffer.allocate(8 + mvhd.size).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(8 + mvhd.size)
            put("moov".toByteArray(Charsets.ISO_8859_1))
            put(mvhd)
        }.array()

        // Dummy mdat
        val mdat = ByteBuffer.allocate(40000).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(40000)
            put("mdat".toByteArray(Charsets.ISO_8859_1))
            put(ByteArray(40000 - 8))
        }.array()

        FileOutputStream(temp).use {
            it.write(ftyp)
            it.write(moov)
            it.write(mdat)
        }

        val dummyArt = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val success = Mp4TagEditor.embedMetadata(
            file = temp,
            title = "Synthetic Title",
            artist = "Synthetic Artist",
            album = "Synthetic Album",
            artBytes = dummyArt
        )
        assertTrue(success)
        assertTrue(temp.length() > ftyp.size + moov.size + mdat.size)

        // Idempotent second tagging
        val success2 = Mp4TagEditor.embedMetadata(
            file = temp,
            title = "Updated Title",
            artist = "Updated Artist",
            album = "Updated Album",
            artBytes = dummyArt
        )
        assertTrue(success2)

        temp.delete()
    }
}

