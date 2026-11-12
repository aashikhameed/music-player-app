package com.aashik.music.desktop.model

import com.aashik.music.desktop.utils.b64u

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
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0NEdDRSakhER3IwWWlYNld4QVJUQkZrZGI5azlWc0FJbTg4c0pYWjdCM08xeU1vUzUza0xTX2R5OFpJS1JyRlJISENCNk9KZVBXVTFHYlk9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "mood_commute",
            title = "Commute",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFFFC200,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1RBTFFralVpSkYybE5QTnpZQUZGQXZKZkpQaUxIdFNIRjJZYnRYS0xBODRoLVphTjdYOHNjRGVkN09UQWlGZ0t4NkNaNEsyWkFaeXFVVXc9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "mood_energize",
            title = "Energize",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFFFE780,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1BpSGVMVHpHdXBNdjBQa1RJMGRnLXNzdGt6T3g4eU5rQk9FZlY2N3lQX3NVa0p0WExSazRKTzhkd2h6MThyWkF6OWdISlNBR0hlenRzSjQ9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "mood_feel_good",
            title = "Feel good",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFA4FFA4,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0FtM2lJR3pGNUR6LThlMm5LSk1RZm41VEVfMWFBVDJ3VU9LSEpKMnh6ZlNvUzBMVDRYVjZwT1pheF9tS0xjMkhhckhicW5tX1ZRMHNZOXZGPXc1MDAtaDUwMC1sOTAtcmo=")
        ),
        DesktopRadioCategory(
            id = "mood_focus",
            title = "Focus",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFCCCCCC,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzlJNDVTWVpBZlRlUjE3QnlWOWkyeURwV2hRNURUV01DclczTGVpOHZNSWlaemV5VW5mMnpSRlVEQm9iNFRHd2dVa1hBQVJfeDFYYThWQT13NTAwLWg1MDAtbDkwLXJq")
        ),
        DesktopRadioCategory(
            id = "mood_gaming",
            title = "Gaming",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFF606060,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1FySVN1X0JrNHNSNk1OX1RVRDJiYldPNFp5ZmRTRkpWRThKUHBDRDRQOXVDUm15UXRFMWUzSGp4dHdzaW9tamlSMGlkVEtEN1dsNnc5QT13NTAwLWg1MDAtbDkwLXJq")
        ),
        DesktopRadioCategory(
            id = "mood_party",
            title = "Party",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFB47BFF,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2tIWEdYUnVMM185djVyS0JHckdYXy1UWTNITXJNdnVUTzFjSGFEX2RHWlMwNFlGcDFXeEY4aFhYbVR3alhTdi1GVnhObEpFMjAxaFBvS0k9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "mood_romance",
            title = "Romance",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFCC0000,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0hFZFNhMzZGb0RMYlVhR1d5bmtHekdfUUZGRWxTaDJScE9GNFVPWGdQd1dUVWticXlub1d3OFc1dHJyUUFuUU03NlBncnlXdVhZVExNZ3c9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "mood_sleep",
            title = "Sleep",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFF7B3EDB,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1N3eW93S2Uta0RMb2tEMndFb3VQbGxKd2pmS2xHNzNwb2RhRkRIQnphRURlUjBtdTNtZ3AweDRpeTdvdVRERDBKdWtzanpHM09iWnNmUT13NTAwLWg1MDAtbDkwLXJq")
        ),
        DesktopRadioCategory(
            id = "mood_workout",
            title = "Workout",
            section = DesktopCategorySection.MOODS_AND_MOMENTS,
            stripeColor = 0xFFFF8D40,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3Vnc1lmRmgwTVdyX0VJVFA3a2cxSzNQTWVEeDJIQ19Fa0VxUXpENy13dzMxVkV3R0hKUHVDWElkY3hCQXFKR2M4ejRvOVE5c2h5eFFzQT13NTAwLWg1MDAtbDkwLXJq")
        )
    )

    val GENRE_CATEGORIES: List<DesktopRadioCategory> = listOf(
        DesktopRadioCategory(
            id = "genre_classical",
            title = "Classical",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFCCCCCC,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2d0MlZMa28wVlBlR3BFd1p3ZW41MEJRN0tMVENDX0xhMVo1bzJicFJmZTRtcVFha1JjZGN0MWFkV1YyUWtuNUhaVU9vYjlpQUw1QVU2UE09dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "genre_dance_and_electronic",
            title = "Dance & Electronic",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF0092BF,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3Zab1ZjTXNRSTBnZ1hYVFRMcDB3SVR2bFJGZTJ5SzlkZ3dRcjZ4Q2xqd19FYlVwNHJEQlBVaUFoLUktU3BQWjZsSFRabnpQRV8wNUtfbUE9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "genre_hip_hop",
            title = "Hip-Hop",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFE24B00,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1M4ajJwYWZzUHF1S3ZGUHYxc3ZwaUZ3aHVOdWlFV203dXpac3d6Y0FwT0ZyMzVHV3hDSVhnTHU1Q1A5VVB6cWVvWFVQRDF1U0Q5RGQ5NUE9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "genre_pop",
            title = "Pop",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFFF78FF,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3lua3BzSWRzWWUxZlZFT3BKZUFPX1N6UUhKRjNyc3VqaFE0RXNtdUlkVkthdmJObTV1RHZEVEJTaEs2YlBqNEk3U2o5WWQ2WnZwbmkyS3R1N2c9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "genre_rock",
            title = "Rock",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFCC0000,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZPV1Z4eWtZTkQwRmttVnZqa3h3dlAzdDhFUXRKaTNkZzFvYkUyYzFNZmhGTG1iVXdiZnpHWU9LRjNWRFNLWHMtTVpOaVlVeFBDc2t6dWNJPXc1MDAtaDUwMC1sOTAtcmo=")
        ),
        DesktopRadioCategory(
            id = "genre_folk_and_acoustic",
            title = "Folk & Acoustic",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF00A513,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0dLQzNuaG1HTE96bm9yZ1dzaWQwb3dVNm5RN0F4VXhfd1gybHhEbWZnczRFMkJSajktcVRJbmtDNVZRUm9oR2ZkODgyd3l4UnBaUTg5cnhtPXc1MDAtaDUwMC1sOTAtcmo=")
        ),
        DesktopRadioCategory(
            id = "genre_randb_and_soul",
            title = "R&B & Soul",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF7B3EDB,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0tvQ2hsbFdpRGpfd2c3S3lSeG1WMjN2QnRJRDNPSE0xWUFzWXh6VmZDWHBySGw1eFc2dHhVQ0pFakNaU3UzUml2TkNHbTc4TVhyUUxsT1U9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "genre_hindi",
            title = "Hindi Hits",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFFFFE780,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzBsUHA1U0dSbkdVbjBOV1F2QkNJT05OWlZlN1Q0UFR0Z3ZGU2c5VHF4M1VIVWpxdTAtRHBxQy1rcFNBcGt3YTA0Z2RpLTduaFc1blNtajA2Y1E9dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioCategory(
            id = "genre_tamil",
            title = "Tamil",
            section = DesktopCategorySection.GENRES,
            stripeColor = 0xFF00A513,
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL21FODEydy11NVA5RTZmTTM1eFk1bUUwbU8yc0I4NDhCMHhYcTlwMHIyPXc1MDAtaDUwMC1sOTAtcmo=")
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
            streamUrl = b64u("aHR0cHM6Ly9zdHJlYW1zLmlsb3ZlbXVzaWMuZGUvaWxvdmVyYWRpbzE3Lm1wMw=="),
            description = "Mellow instrumental hip-hop and chillhop vibes",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzlJNDVTWVpBZlRlUjE3QnlWOWkyeURwV2hRNURUV01DclczTGVpOHZNSWlaemV5VW5mMnpSRlVEQm9iNFRHd2dVa1hBQVJfeDFYYThWQT13NTAwLWg1MDAtbDkwLXJq")
        ),
        DesktopRadioStation(
            id = "stream_chillhop",
            name = "ChillHop Radio",
            genre = "Lo-Fi",
            categoryId = "genre_dance_and_electronic",
            streamUrl = b64u("aHR0cDovL3N0cmVhbS56ZW5vLmZtL2Yzd3ZiYnFtZGc4dXY="),
            description = "Smooth urban jazz beats and lo-fi coffeehop",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0FtM2lJR3pGNUR6LThlMm5LSk1RZm41VEVfMWFBVDJ3VU9LSEpKMnh6ZlNvUzBMVDRYVjZwT1pheF9tS0xjMkhhckhicW5tX1ZRMHNZOXZGPXc1MDAtaDUwMC1sOTAtcmo=")
        ),

        // Synthwave & Retro
        DesktopRadioStation(
            id = "stream_synthwave",
            name = "Nightwave Synthwave",
            genre = "Synthwave",
            categoryId = "genre_dance_and_electronic",
            streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0uemVuby5mbS93NzR4MHEyZGRlenV2"),
            description = "Retrowave, Darksynth & 80s electronic driving frequencies",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZPV1Z4eWtZTkQwRmttVnZqa3h3dlAzdDhFUXRKaTNkZzFvYkUyYzFNZmhGTG1iVXdiZnpHWU9LRjNWRFNLWHMtTVpOaVlVeFBDc2t6dWNJPXc1MDAtaDUwMC1sOTAtcmo=")
        ),
        DesktopRadioStation(
            id = "stream_plaza",
            name = "Nightwave Plaza",
            genre = "Vaporwave",
            categoryId = "genre_dance_and_electronic",
            streamUrl = b64u("aHR0cHM6Ly9yYWRpby5wbGF6YS5vbmUvbXAz"),
            description = "Nostalgic vaporwave, future funk and mallsoft aesthetics",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0tvQ2hsbFdpRGpfd2c3S3lSeG1WMjN2QnRJRDNPSE0xWUFzWXh6VmZDWHBySGw1eFc2dHhVQ0pFakNaU3UzUml2TkNHbTc4TVhyUUxsT1U9dzUwMC1oNTAwLWw5MC1yag==")
        ),

        // Rock & Alternative
        DesktopRadioStation(
            id = "stream_rp_rock",
            name = "Radio Paradise Rock",
            genre = "Rock",
            categoryId = "genre_rock",
            streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0ucmFkaW9wYXJhZGlzZS5jb20vcm9jay0xMjg="),
            description = "Curated modern and classic rock mix with no commercials",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZPV1Z4eWtZTkQwRmttVnZqa3h3dlAzdDhFUXRKaTNkZzFvYkUyYzFNZmhGTG1iVXdiZnpHWU9LRjNWRFNLWHMtTVpOaVlVeFBDc2t6dWNJPXc1MDAtaDUwMC1sOTAtcmo=")
        ),
        DesktopRadioStation(
            id = "stream_rock_classic",
            name = "Rock Antenne Classic",
            genre = "Classic Rock",
            categoryId = "genre_rock",
            streamUrl = b64u("aHR0cHM6Ly9zMi13ZWJyYWRpby5hbnRlbm5lLmRlL3JvY2stYW50ZW5uZQ=="),
            description = "Legendary guitar anthems, stadium rock and metal hits",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZPV1Z4eWtZTkQwRmttVnZqa3h3dlAzdDhFUXRKaTNkZzFvYkUyYzFNZmhGTG1iVXdiZnpHWU9LRjNWRFNLWHMtTVpOaVlVeFBDc2t6dWNJPXc1MDAtaDUwMC1sOTAtcmo=")
        ),

        // Ambient & Acoustic
        DesktopRadioStation(
            id = "stream_rp_mellow",
            name = "Radio Paradise Mellow",
            genre = "Ambient",
            categoryId = "genre_folk_and_acoustic",
            streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0ucmFkaW9wYXJhZGlzZS5jb20vbWVsbG93LTEyOA=="),
            description = "Relaxed acoustic, singer-songwriter, and ambient soundscapes",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1N3eW93S2Uta0RMb2tEMndFb3VQbGxKd2pmS2xHNzNwb2RhRkRIQnphRURlUjBtdTNtZ3AweDRpeTdvdVRERDBKdWtzanpHM09iWnNmUT13NTAwLWg1MDAtbDkwLXJq")
        ),
        DesktopRadioStation(
            id = "stream_rp_main",
            name = "Radio Paradise Eclectic",
            genre = "Eclectic",
            categoryId = "genre_folk_and_acoustic",
            streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0ucmFkaW9wYXJhZGlzZS5jb20vbXAzLTEyOA=="),
            description = "Unique blend of acoustic, indie rock and world rhythm",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0dLQzNuaG1HTE96bm9yZ1dzaWQwb3dVNm5RN0F4VXhfd1gybHhEbWZnczRFMkJSajktcVRJbmtDNVZRUm9oR2ZkODgyd3l4UnBaUTg5cnhtPXc1MDAtaDUwMC1sOTAtcmo=")
        ),

        // Classical
        DesktopRadioStation(
            id = "stream_swiss_classic",
            name = "Radio Swiss Classic",
            genre = "Classical",
            categoryId = "genre_classical",
            streamUrl = b64u("aHR0cDovL3N0cmVhbS5zcmctc3NyLmNoL20vcnNjX2RlL21wM18xMjg="),
            description = "Premium orchestral masterpieces, concertos, and symphonies",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2d0MlZMa28wVlBlR3BFd1p3ZW41MEJRN0tMVENDX0xhMVo1bzJicFJmZTRtcVFha1JjZGN0MWFkV1YyUWtuNUhaVU9vYjlpQUw1QVU2UE09dzUwMC1oNTAwLWw5MC1yag==")
        ),
        DesktopRadioStation(
            id = "stream_classic_fm",
            name = "Classic FM UK",
            genre = "Classical",
            categoryId = "genre_classical",
            streamUrl = b64u("aHR0cHM6Ly9tZWRpYS1zc2wubXVzaWNyYWRpby5jb20vQ2xhc3NpY0ZN"),
            description = "World famous classical broadcaster featuring timeless works",
            bitrate = "128 kbps",
            imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2d0MlZMa28wVlBlR3BFd1p3ZW41MEJRN0tMVENDX0xhMVo1bzJicFJmZTRtcVFha1JjZGN0MWFkV1YyUWtuNUhaVU9vYjlpQUw1QVU2UE09dzUwMC1oNTAwLWw5MC1yag==")
        )
    )
}

