package com.aashik.music.model

import androidx.compose.runtime.Immutable

@Immutable
data class MusicFolder(
    val name: String,
    val path: String,
    val songCount: Int
)
