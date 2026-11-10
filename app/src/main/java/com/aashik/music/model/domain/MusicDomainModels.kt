package com.aashik.music.model.domain

import com.aashik.music.model.Song

data class DomainAlbum(
    val title: String,
    val artist: String,
    val year: String = "2025",
    val genre: String = "Pop",
    val artworkUri: String? = null,
    val editorialReview: String = "An expansive listening experience crafted with rich spatial immersion.",
    val isDolbyAtmos: Boolean = true,
    val isLossless: Boolean = true,
    val tracks: List<Song> = emptyList()
)

data class DomainArtist(
    val name: String,
    val photoUrl: String? = null,
    val origin: String = "United States",
    val born: String = "",
    val genre: String = "Pop",
    val biography: String = "Celebrated singer-songwriter crafting intimate, sonically rich pop and acoustic narratives.",
    val topSongs: List<Song> = emptyList(),
    val albums: List<DomainAlbum> = emptyList()
)

data class DomainPlaylist(
    val id: String,
    val title: String,
    val curator: String = "Apple Music Pop",
    val description: String = "Curated collection of the most captivating songs and spatial audio mixes.",
    val lastUpdated: String = "Updated recently",
    val songs: List<Song> = emptyList(),
    val featuredArtists: List<DomainArtist> = emptyList()
)

