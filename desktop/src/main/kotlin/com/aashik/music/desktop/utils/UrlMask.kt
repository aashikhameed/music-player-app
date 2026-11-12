package com.aashik.music.desktop.utils

/** Decodes a Base64-masked URL (URLs are not stored in plain text in the source). */
fun b64u(encoded: String): String = String(java.util.Base64.getDecoder().decode(encoded), Charsets.UTF_8)
