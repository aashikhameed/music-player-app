// AlbumArtCache.kt
package com.aashik.music.cache

import android.graphics.Bitmap

object AlbumArtCache {
    private val cache = object : LinkedHashMap<String, Bitmap>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > 100
        }
    }

    @Synchronized
    fun get(path: String): Bitmap? = cache[path]

    @Synchronized
    fun put(path: String, bitmap: Bitmap) {
        cache[path] = bitmap
    }

    @Synchronized
    fun contains(path: String): Boolean = cache.containsKey(path)
}

