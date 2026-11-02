package com.aashik.music.provider

import com.aashik.music.model.Song

/**
 * Decoupled media source ingestion provider interface.
 * Abstracts local file system, network streams, and radio sources from the core audio player engine.
 */
interface MediaSourceProvider {
    val providerId: String
    val displayName: String

    suspend fun getStreamUri(track: Song): String?
    suspend fun search(query: String): List<Song>
    fun isOfflineAvailable(track: Song): Boolean
}

/**
 * Media provider for local on-device and USB storage audio files.
 */
class LocalFileSourceProvider : MediaSourceProvider {
    override val providerId: String = "local_storage"
    override val displayName: String = "Local Storage"

    override suspend fun getStreamUri(track: Song): String? {
        return if (!track.isStream() && track.path.isNotBlank()) track.path else null
    }

    override suspend fun search(query: String): List<Song> {
        return emptyList()
    }

    override fun isOfflineAvailable(track: Song): Boolean {
        return !track.isStream()
    }
}

/**
 * Media provider for live online radio stations and community streaming tracks.
 */
class OnlineStreamSourceProvider : MediaSourceProvider {
    override val providerId: String = "online_stream"
    override val displayName: String = "Live Radio & Online Streams"

    override suspend fun getStreamUri(track: Song): String? {
        return if (track.isStream()) track.path else null
    }

    override suspend fun search(query: String): List<Song> {
        return emptyList()
    }

    override fun isOfflineAvailable(track: Song): Boolean {
        return false
    }
}

