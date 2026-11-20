package com.aashik.music.wear.online

import com.aashik.music.wear.model.WearSong

object WearRadioRepository {

    data class RadioCategory(
        val id: String,
        val title: String,
        val subtitle: String,
        val isMood: Boolean
    )

    data class LiveRadioStation(
        val id: String,
        val name: String,
        val description: String,
        val streamUrl: String
    )

    private fun decodeB64(b64: String): String =
        String(java.util.Base64.getDecoder().decode(b64), Charsets.UTF_8)

    val LIVE_RADIO_STATIONS = listOf(
        LiveRadioStation(
            id = "stream_lofi_girl",
            name = "Lofi Girl Beats",
            description = "24/7 relaxing lofi hip hop radio",
            streamUrl = decodeB64("aHR0cHM6Ly9wbGF5LnN0cmVhbWFmcmljYS5uZXQvbG9maXJhZGlv")
        ),
        LiveRadioStation(
            id = "stream_box_lofi",
            name = "Box Lofi Chill",
            description = "Mellow instrumental chillhop beats",
            streamUrl = decodeB64("aHR0cHM6Ly9zdHJlYW1zLmlsb3ZlbXVzaWMuZGUvaWxvdmVyYWRpbzE3Lm1wMw==")
        ),
        LiveRadioStation(
            id = "stream_synthwave",
            name = "Nightwave Synthwave",
            description = "Retrowave & 80s electronic synth",
            streamUrl = decodeB64("aHR0cHM6Ly9zdHJlYW0uemVuby5mbS93NzR4MHEyZGRlenV2")
        ),
        LiveRadioStation(
            id = "stream_rp_rock",
            name = "Radio Paradise Rock",
            description = "Modern & classic rock mix",
            streamUrl = decodeB64("aHR0cHM6Ly9zdHJlYW0ucmFkaW9wYXJhZGlzZS5jb20vcm9jay0xMjg=")
        ),
        LiveRadioStation(
            id = "stream_dance_wave",
            name = "Dance Wave Hits",
            description = "Non-stop dance & electronic club",
            streamUrl = decodeB64("aHR0cHM6Ly9kYW5jZXdhdmUub25saW5lL2RhbmNlLm1wMw==")
        ),
        LiveRadioStation(
            id = "stream_swiss_jazz",
            name = "Radio Swiss Jazz",
            description = "Smooth jazz, swing & soul classics",
            streamUrl = decodeB64("aHR0cDovL3N0cmVhbS5zcmctc3NyLmNoL20vcnNqL21wM18xMjg=")
        ),
        LiveRadioStation(
            id = "stream_swiss_classic",
            name = "Radio Swiss Classic",
            description = "Timeless classical music symphonies",
            streamUrl = decodeB64("aHR0cDovL3N0cmVhbS5zcmctc3NyLmNoL20vcnNjX2RlL21wM18xMjg=")
        ),
        LiveRadioStation(
            id = "stream_bbc_world",
            name = "BBC World Service",
            description = "International news & current affairs",
            streamUrl = decodeB64("aHR0cDovL3N0cmVhbS5saXZlLnZjLmJiY21lZGlhLmNvLnVrL2JiY193b3JsZF9zZXJ2aWNl")
        )
    )

    fun LiveRadioStation.toWearSong(): WearSong = WearSong(
        id = id,
        title = name,
        artist = description,
        durationMs = 0L,
        path = streamUrl,
        album = "Live Radio"
    )

    val MOOD_CATEGORIES = listOf(
        RadioCategory("mood_chill", "Chill", "Relaxing vibes & beats", true),
        RadioCategory("mood_workout", "Workout", "High-energy training beats", true),
        RadioCategory("mood_energize", "Energize", "Uplifting & motivational", true),
        RadioCategory("mood_focus", "Focus", "Instrumental & deep focus", true),
        RadioCategory("mood_feel_good", "Feel Good", "Positive & cheerful tunes", true),
        RadioCategory("mood_commute", "Commute", "Songs for the road", true),
        RadioCategory("mood_sleep", "Sleep", "Calming ambient soundscapes", true),
        RadioCategory("mood_party", "Party", "Upbeat dancefloor anthems", true),
        RadioCategory("mood_romance", "Romance", "Love songs & acoustic ballads", true)
    )

    val GENRE_CATEGORIES = listOf(
        // Regional Indian & Global Languages (matching Mobile)
        RadioCategory("genre_tamil", "Tamil", "Tamil hits, melodies & kuthu", false),
        RadioCategory("genre_telugu", "Telugu", "Telugu chartbusters & melodies", false),
        RadioCategory("genre_hindi", "Hindi", "Bollywood & indie Hindi hits", false),
        RadioCategory("genre_malayalam", "Malayalam", "Mollywood melodies & indie", false),
        RadioCategory("genre_kannada", "Kannada", "Sandalwood hits & classics", false),
        RadioCategory("genre_punjabi", "Punjabi", "Punjabi pop, bhangra & beats", false),
        RadioCategory("genre_english", "English", "Global English pop & top hits", false),
        RadioCategory("genre_pop", "Pop Hits", "Global charts & trending pop", false),
        RadioCategory("genre_hiphop", "Hip-Hop", "Beats, bars & flow", false),
        RadioCategory("genre_dance", "Dance & Electronic", "EDM, house & festival anthems", false),
        RadioCategory("genre_rock", "Rock Classics", "Riffs, anthems & alt-rock", false),
        RadioCategory("genre_indie", "Indie & Alternative", "Fresh indie soundscapes", false),
        RadioCategory("genre_lofi", "Lo-Fi Beats", "Mellow beats to relax/study to", false),
        RadioCategory("genre_classical", "Classical", "Orchestral masterpieces", false),
        RadioCategory("genre_acoustic", "Acoustic & Folk", "Raw unplugged sessions", false),
        RadioCategory("genre_jazz", "Jazz & Soul", "Smooth brass & soulful grooves", false),
        RadioCategory("genre_kpop", "K-Pop", "High-energy global K-Pop", false)
    )

    suspend fun getCategoryTracks(category: RadioCategory, page: Int = 1): List<WearSong> {
        val baseQuery = when (category.id) {
            "mood_chill" -> "Chillout relaxing vibes"
            "mood_workout" -> "Workout gym motivational music"
            "mood_energize" -> "Energize uplifting beats"
            "mood_focus" -> "Deep focus study instrumental"
            "mood_feel_good" -> "Feel good happy songs"
            "mood_commute" -> "Road trip travel songs"
            "mood_sleep" -> "Calm sleep ambient soothing"
            "mood_party" -> "Party dance electronic hits"
            "mood_romance" -> "Romantic acoustic love songs"
            "genre_tamil" -> "Tamil hits songs"
            "genre_telugu" -> "Telugu hits songs"
            "genre_hindi" -> "Hindi bollywood hits"
            "genre_malayalam" -> "Malayalam hits songs"
            "genre_kannada" -> "Kannada hits songs"
            "genre_punjabi" -> "Punjabi hits songs"
            "genre_english" -> "Top english hits"
            "genre_pop" -> "Top global pop hits"
            "genre_hiphop" -> "Hip-hop rap trending hits"
            "genre_dance" -> "EDM electronic dance festival"
            "genre_rock" -> "Classic rock anthems alternative"
            "genre_indie" -> "Indie alternative acoustic"
            "genre_lofi" -> "Lofi hip hop beats study relax"
            "genre_classical" -> "Classical orchestra piano masterpieces"
            "genre_acoustic" -> "Acoustic unplugged guitar vocal"
            "genre_jazz" -> "Smooth jazz soul blues"
            "genre_kpop" -> "K-Pop trending hits"
            else -> category.title
        }
        return WearOnlineRepository.search(baseQuery, limit = 15, page = page)
    }
}
