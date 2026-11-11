package com.aashik.music.model.domain

import com.aashik.music.model.Song

data class DomainAlbum(
    val title: String,
    val artist: String,
    val year: String = "",
    val genre: String = "",
    val artworkUri: String? = null,
    val editorialReview: String = "",
    val isDolbyAtmos: Boolean = false,
    val isLossless: Boolean = false,
    val tracks: List<Song> = emptyList()
)

data class DomainArtist(
    val name: String,
    val photoUrl: String? = null,
    val origin: String = "",
    val born: String = "",
    val genre: String = "",
    val biography: String = "",
    val topSongs: List<Song> = emptyList(),
    val albums: List<DomainAlbum> = emptyList()
)

data class DomainPlaylist(
    val id: String,
    val title: String,
    val curator: String = "",
    val description: String = "",
    val lastUpdated: String = "",
    val songs: List<Song> = emptyList(),
    val featuredArtists: List<DomainArtist> = emptyList()
)

