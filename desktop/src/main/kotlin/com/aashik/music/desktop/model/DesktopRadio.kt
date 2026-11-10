package com.aashik.music.desktop.model

enum class DesktopCategorySection {
    MOODS_AND_MOMENTS,
    GENRES
}

data class DesktopRadioCategory(
    val id: String,
    val title: String,
    val section: DesktopCategorySection = DesktopCategorySection.MOODS_AND_MOMENTS,
    val stripeColor: Long,
    val imageUrl: String = "",
    val params: String = "",
    val description: String = ""
)

data class DesktopRadioStation(
    val id: String,
    val name: String,
    val genre: String,
    val streamUrl: String,
    val categoryId: String = "genre_classical",
    val description: String = "",
    val country: String = "Global",
    val bitrate: String = "128 kbps",
    val imageUrl: String = ""
) {
    fun toSong(): DesktopSong {
        val art = imageUrl.ifBlank {
            DesktopRadioData.CATEGORIES.firstOrNull { it.id == categoryId }?.imageUrl ?: ""
        }
        return DesktopSong(
            id = id,
            title = name,
            artist = "$genre • $bitrate",
            album = "Live Radio Stream",
            duration = 0L,
            path = streamUrl,
            artworkUrl = art.ifBlank { null }
        )
    }
}

object DesktopRadioData {
    val MOOD_CATEGORIES: List<DesktopRadioCategory> = listOf(
        DesktopRadioCategory(
            id = "mood_chill",
            title = "Chill",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFA4C5FF,
            imageUrl = "https://yt3.googleusercontent.com/CDt4RjHDGr0YiX6WxARTBFkdb9k9VsAIm88sJXZ7B3O1yMoS53kLS_dy8ZIKRrFRHHCB6OJePWU1GbY=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_commute",
            title = "Commute",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFFFC200,
            imageUrl = "https://yt3.googleusercontent.com/TALQkjUiJF2lNPNzYAFFAvJfJPiLHtSHF2YbtXKLA84h-ZaN7X8scDed7OTAiFgKx6CZ4K2ZAZyqUUw=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_energize",
            title = "Energize",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFFFE780,
            imageUrl = "https://yt3.googleusercontent.com/PiHeLTzGupMv0PkTI0dg-sstkzOx8yNkBOEfV67yP_sUkJtXLRk4JO8dwhz18rZAz9gHJSAGHeztsJ4=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_feel_good",
            title = "Feel good",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFA4FFA4,
            imageUrl = "https://yt3.googleusercontent.com/Am3iIGzF5Dz-8e2nKJMQfn5TE_1aAT2wUOKHJJ2xzfSoS0LT4XV6pOZax_mKLc2HarHbqnm_VQ0sY9vF=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_focus",
            title = "Focus",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFCCCCCC,
            imageUrl = "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_gaming",
            title = "Gaming",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFF606060,
            imageUrl = "https://yt3.googleusercontent.com/QrISu_Bk4sR6MN_TUD2bbWO4ZyfdSFJVE8JPpCD4P9uCRmyQtE1e3HjxtwsiomjiR0idTKD7Wl6w9A=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_party",
            title = "Party",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFB47BFF,
            imageUrl = "https://yt3.googleusercontent.com/kHXGXRuL3_9v5rKBGrGX_-TY3HMrMvuTO1cHaD_dGZS04YFp1WxF8hXXmTwjXSv-FVxNlJE201hPoKI=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_romance",
            title = "Romance",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFCC0000,
            imageUrl = "https://yt3.googleusercontent.com/HEdSa36FoDLbUaGWynkGzG_QFFElSh2RpOF4UOXgPwWTUkbqynoWw8W5trrQAnQM76PgryWuXYTLMgw=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_sleep",
            title = "Sleep",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFF7B3EDB,
            imageUrl = "https://yt3.googleusercontent.com/SwyowKe-kDLokD2wEouPllJwjfKlG73podaFDHBzaEDeR0mu3mgp0x4iy7ouTDD0JuksjzG3ObZsfQ=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "mood_workout",
            title = "Workout",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFFF8D40,
            imageUrl = "https://yt3.googleusercontent.com/ugsYfFh0MWr_EITP7kg1K3PMeDx2HC_EkEqQzD7-ww31VEwGHJPuCXIdcxBAqJGc8z4o9Q9shyxQsA=w500-h500-l90-rj"
        )
    )

    val GENRE_CATEGORIES: List<DesktopRadioCategory> = listOf(
        DesktopRadioCategory(
            id = "genre_classical",
            title = "Classical",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFCCCCCC,
            imageUrl = "https://yt3.googleusercontent.com/gt2VLko0VPeGpEwZwen50BQ7KLTCC_La1Z5o2bpRfe4mqQakRcdct1adWV2Qkn5HZUOob9iAL5AU6PM=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_dance_and_electronic",
            title = "Dance & Electronic",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF0092BF,
            imageUrl = "https://yt3.googleusercontent.com/vZoVcMsQI0ggXXTTLp0wITvlRFe2yK9dgwQr6xCljw_EbUp4rDBPUiAh-I-SpPZ6lHTZnzPE_05K_mA=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_hip_hop",
            title = "Hip-Hop",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFE24B00,
            imageUrl = "https://yt3.googleusercontent.com/S8j2pafsPquKvFPv1svpiFwhuNuiEWm7uzZswzcApOFr35GWxCIXgLu5CP9UPzqeoXUPD1uSD9Dd95A=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_pop",
            title = "Pop",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFFF78FF,
            imageUrl = "https://yt3.googleusercontent.com/ynkpsIdsYe1fVEOpJeAO_SzQHJF3rsujhQ4EsmuIdVKavbNm5uDvDTBShK6bPj4I7Sj9Yd6Zvpni2Ktu7g=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_rock",
            title = "Rock",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFCC0000,
            imageUrl = "https://yt3.googleusercontent.com/FOWVxykYND0FkmVvjkxwvP3t8EQtJi3dg1obE2c1MfhFLmbUwbfzGYOKF3VDSKXs-MZNiYUxPCskzucI=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_folk_and_acoustic",
            title = "Folk & Acoustic",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF00A513,
            imageUrl = "https://yt3.googleusercontent.com/GKC3nhmGLOznorgWsid0owU6nQ7AxUx_wX2lxDmfgs4E2BRj9-qTInkC5VQRohGfd882wyxRpZQ89rxm=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_randb_and_soul",
            title = "R&B & Soul",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF7B3EDB,
            imageUrl = "https://yt3.googleusercontent.com/KoChllWiDj_wg7KyRxmV23vBtID3OHM1YAsYxzVfCXprHl5xW6txUCJEjCZSu3RivNCGm78MXrQLlOU=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_hindi",
            title = "Hindi Hits",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFFFE780,
            imageUrl = "https://yt3.googleusercontent.com/0lPp5SGRnGUn0NWQvBCIONNZVe7T4PTtgvFSg9Tqx3UHUjqu0-DpqC-kpSApkwa04gdi-7nhW5nSmj06cQ=w500-h500-l90-rj"
        ),
        DesktopRadioCategory(
            id = "genre_tamil",
            title = "Tamil Top 50",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF00A513,
            imageUrl = "https://yt3.googleusercontent.com/mE812w-u5P9E6fM35xY5mE0mO2sB848B0xXq9p0r2=w500-h500-l90-rj"
        )
    )

    val CATEGORIES: List<DesktopRadioCategory> = MOOD_CATEGORIES + GENRE_CATEGORIES

    val CURATED_STATIONS: List<DesktopRadioStation> = listOf(
        // Lo-Fi & Chill
        DesktopRadioStation(
            id = "stream_box_lofi",
            name = "Box Lofi Chill",
            genre = "Lo-Fi",
            categoryId = "genre_dance_and_electronic",
            streamUrl = "https://streams.ilovemusic.de/iloveradio17.mp3",
            description = "Mellow instrumental hip-hop and chillhop vibes",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w500-h500-l90-rj"
        ),
        DesktopRadioStation(
            id = "stream_chillhop",
            name = "ChillHop Radio",
            genre = "Lo-Fi",
            categoryId = "genre_dance_and_electronic",
            streamUrl = "http://stream.zeno.fm/f3wvbbqmdg8uv",
            description = "Smooth urban jazz beats and lo-fi coffeehop",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/Am3iIGzF5Dz-8e2nKJMQfn5TE_1aAT2wUOKHJJ2xzfSoS0LT4XV6pOZax_mKLc2HarHbqnm_VQ0sY9vF=w500-h500-l90-rj"
        ),

        // Synthwave & Retro
        DesktopRadioStation(
            id = "stream_synthwave",
            name = "Nightwave Synthwave",
            genre = "Synthwave",
            categoryId = "genre_dance_and_electronic",
            streamUrl = "https://stream.zeno.fm/w74x0q2ddezuv",
            description = "Retrowave, Darksynth & 80s electronic driving frequencies",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/FOWVxykYND0FkmVvjkxwvP3t8EQtJi3dg1obE2c1MfhFLmbUwbfzGYOKF3VDSKXs-MZNiYUxPCskzucI=w500-h500-l90-rj"
        ),
        DesktopRadioStation(
            id = "stream_plaza",
            name = "Nightwave Plaza",
            genre = "Vaporwave",
            categoryId = "genre_dance_and_electronic",
            streamUrl = "https://radio.plaza.one/mp3",
            description = "Nostalgic vaporwave, future funk and mallsoft aesthetics",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/KoChllWiDj_wg7KyRxmV23vBtID3OHM1YAsYxzVfCXprHl5xW6txUCJEjCZSu3RivNCGm78MXrQLlOU=w500-h500-l90-rj"
        ),

        // Rock & Alternative
        DesktopRadioStation(
            id = "stream_rp_rock",
            name = "Radio Paradise Rock",
            genre = "Rock",
            categoryId = "genre_rock",
            streamUrl = "https://stream.radioparadise.com/rock-128",
            description = "Curated modern and classic rock mix with no commercials",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/FOWVxykYND0FkmVvjkxwvP3t8EQtJi3dg1obE2c1MfhFLmbUwbfzGYOKF3VDSKXs-MZNiYUxPCskzucI=w500-h500-l90-rj"
        ),
        DesktopRadioStation(
            id = "stream_rock_classic",
            name = "Rock Antenne Classic",
            genre = "Classic Rock",
            categoryId = "genre_rock",
            streamUrl = "https://s2-webradio.antenne.de/rock-antenne",
            description = "Legendary guitar anthems, stadium rock and metal hits",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/FOWVxykYND0FkmVvjkxwvP3t8EQtJi3dg1obE2c1MfhFLmbUwbfzGYOKF3VDSKXs-MZNiYUxPCskzucI=w500-h500-l90-rj"
        ),

        // Ambient & Acoustic
        DesktopRadioStation(
            id = "stream_rp_mellow",
            name = "Radio Paradise Mellow",
            genre = "Ambient",
            categoryId = "genre_folk_and_acoustic",
            streamUrl = "https://stream.radioparadise.com/mellow-128",
            description = "Relaxed acoustic, singer-songwriter, and ambient soundscapes",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/SwyowKe-kDLokD2wEouPllJwjfKlG73podaFDHBzaEDeR0mu3mgp0x4iy7ouTDD0JuksjzG3ObZsfQ=w500-h500-l90-rj"
        ),
        DesktopRadioStation(
            id = "stream_rp_main",
            name = "Radio Paradise Eclectic",
            genre = "Eclectic",
            categoryId = "genre_folk_and_acoustic",
            streamUrl = "https://stream.radioparadise.com/mp3-128",
            description = "Unique blend of acoustic, indie rock and world rhythm",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/GKC3nhmGLOznorgWsid0owU6nQ7AxUx_wX2lxDmfgs4E2BRj9-qTInkC5VQRohGfd882wyxRpZQ89rxm=w500-h500-l90-rj"
        ),

        // Classical
        DesktopRadioStation(
            id = "stream_swiss_classic",
            name = "Radio Swiss Classic",
            genre = "Classical",
            categoryId = "genre_classical",
            streamUrl = "http://stream.srg-ssr.ch/m/rsc_de/mp3_128",
            description = "Premium orchestral masterpieces, concertos, and symphonies",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/gt2VLko0VPeGpEwZwen50BQ7KLTCC_La1Z5o2bpRfe4mqQakRcdct1adWV2Qkn5HZUOob9iAL5AU6PM=w500-h500-l90-rj"
        ),
        DesktopRadioStation(
            id = "stream_classic_fm",
            name = "Classic FM UK",
            genre = "Classical",
            categoryId = "genre_classical",
            streamUrl = "https://media-ssl.musicradio.com/ClassicFM",
            description = "World famous classical broadcaster featuring timeless works",
            bitrate = "128 kbps",
            imageUrl = "https://yt3.googleusercontent.com/gt2VLko0VPeGpEwZwen50BQ7KLTCC_La1Z5o2bpRfe4mqQakRcdct1adWV2Qkn5HZUOob9iAL5AU6PM=w500-h500-l90-rj"
        )
    )
}
