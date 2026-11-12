package com.aashik.music.model

import com.aashik.music.utils.b64u

import androidx.compose.runtime.Immutable

enum class CategorySection {
    MOODS_AND_MOMENTS,
    GENRES
}

@Immutable
data class RadioCategory(
    val id: String,
    val title: String,
    val section: CategorySection = CategorySection.MOODS_AND_MOMENTS,
    val stripeColor: Long, // 0xFFA4C5FF, etc.
    val imageUrl: String = "",
    val params: String = "",
    val description: String = ""
)

@Immutable
data class RadioStation(
    val id: String,
    val name: String,
    val genre: String,
    val streamUrl: String,
    val categoryId: String = "genre_classical",
    val description: String = "",
    val country: String = "Global",
    val bitrate: String = "128 kbps",
    val isCommunityPlaylist: Boolean = true,
    val imageUrl: String = ""
) {
    fun toSong(): Song {
        val art = imageUrl.ifBlank {
            CATEGORIES.firstOrNull { it.id == categoryId }?.imageUrl ?: ""
        }
        if (art.isNotBlank()) {
            com.aashik.music.cache.AlbumArtCache.registerStreamArtwork(streamUrl, art)
        }
        return Song(
            id = id,
            title = name,
            artist = "$genre • $bitrate",
            album = "Online Radio & Streams",
            duration = 0L,
            path = streamUrl,
            artworkUrl = art
        )
    }

    companion object {
        val MOOD_CATEGORIES: List<RadioCategory> = listOf(
            RadioCategory(
                id = "mood_chill",
                title = "Chill",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFA4C5FF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0NEdDRSakhER3IwWWlYNld4QVJUQkZrZGI5azlWc0FJbTg4c0pYWjdCM08xeU1vUzUza0xTX2R5OFpJS1JyRlJISENCNk9KZVBXVTFHYlk9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX1JOQWZFeDByc2Jm"
            ),
            RadioCategory(
                id = "mood_commute",
                title = "Commute",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFFFC200,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1RBTFFralVpSkYybE5QTnpZQUZGQXZKZkpQaUxIdFNIRjJZYnRYS0xBODRoLVphTjdYOHNjRGVkN09UQWlGZ0t4NkNaNEsyWkFaeXFVVXc9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX044Z2o5WERLckpU"
            ),
            RadioCategory(
                id = "mood_diwali",
                title = "Diwali",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF606060,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0hGUjcwM29USTVseWh6VGRzbjFab19OcG05UXh0SU15Vnp6N0p6ekQxcnhkZ2ZFU2o5N2NrYmIyY3c3QW8tczVubVo2QVg1NlhzM2pMUT13MTIwMC1oMTIwMC1sOTAtcmo="),
                params = "ggMPOg1uX0lQVm1ablYxWml5"
            ),
            RadioCategory(
                id = "mood_energize",
                title = "Energize",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFFFE780,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1BpSGVMVHpHdXBNdjBQa1RJMGRnLXNzdGt6T3g4eU5rQk9FZlY2N3lQX3NVa0p0WExSazRKTzhkd2h6MThyWkF6OWdISlNBR0hlenRzSjQ9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX2lRZUZiMnNrQnJW"
            ),
            RadioCategory(
                id = "mood_feel_good",
                title = "Feel good",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFA4FFA4,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0FtM2lJR3pGNUR6LThlMm5LSk1RZm41VEVfMWFBVDJ3VU9LSEpKMnh6ZlNvUzBMVDRYVjZwT1pheF9tS0xjMkhhckhicW5tX1ZRMHNZOXZGPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uXzZQbDB5eThLRTQ3"
            ),
            RadioCategory(
                id = "mood_focus",
                title = "Focus",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFCCCCCC,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzlJNDVTWVpBZlRlUjE3QnlWOWkyeURwV2hRNURUV01DclczTGVpOHZNSWlaemV5VW5mMnpSRlVEQm9iNFRHd2dVa1hBQVJfeDFYYThWQT13MTIwMC1oMTIwMC1sOTAtcmo="),
                params = "ggMPOg1uX0NvNGNhWThMYWRh"
            ),
            RadioCategory(
                id = "mood_gaming",
                title = "Gaming",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF606060,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1FySVN1X0JrNHNSNk1OX1RVRDJiYldPNFp5ZmRTRkpWRThKUHBDRDRQOXVDUm15UXRFMWUzSGp4dHdzaW9tamlSMGlkVEtEN1dsNnc5QT13MTIwMC1oMTIwMC1sOTAtcmo="),
                params = "ggMPOg1uX3NmUVV4Vzl3WGQ0"
            ),
            RadioCategory(
                id = "mood_party",
                title = "Party",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFB47BFF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2tIWEdYUnVMM185djVyS0JHckdYXy1UWTNITXJNdnVUTzFjSGFEX2RHWlMwNFlGcDFXeEY4aFhYbVR3alhTdi1GVnhObEpFMjAxaFBvS0k9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0pmQ0s2V0JRclZs"
            ),
            RadioCategory(
                id = "mood_romance",
                title = "Romance",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFCC0000,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0hFZFNhMzZGb0RMYlVhR1d5bmtHekdfUUZGRWxTaDJScE9GNFVPWGdQd1dUVWticXlub1d3OFc1dHJyUUFuUU03NlBncnlXdVhZVExNZ3c9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0FzQ2FhZWtUY211"
            ),
            RadioCategory(
                id = "mood_sad",
                title = "Sad",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF8C8C8C,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3E0aUNRdW9IQmV1bUw5czBMTVRJaUNNZVlSMkVkSWxOcF9ZQUtpS1VqbGVodUY4LWxSTy1fUHZDcGx0MlRDdktRVjNFQ1BRR1pVX3BtclE9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0JLQ0gySWZKZVY1"
            ),
            RadioCategory(
                id = "mood_sleep",
                title = "Sleep",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF7B3EDB,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1N3eW93S2Uta0RMb2tEMndFb3VQbGxKd2pmS2xHNzNwb2RhRkRIQnphRURlUjBtdTNtZ3AweDRpeTdvdVRERDBKdWtzanpHM09iWnNmUT13MTIwMC1oMTIwMC1sOTAtcmo="),
                params = "ggMPOg1uX1MxaFQ3Z0JMZkN4"
            ),
            RadioCategory(
                id = "mood_workout",
                title = "Workout",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFFF8D40,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3Vnc1lmRmgwTVdyX0VJVFA3a2cxSzNQTWVEeDJIQ19Fa0VxUXpENy13dzMxVkV3R0hKUHVDWElkY3hCQXFKR2M4ejRvOVE5c2h5eFFzQT13MTIwMC1oMTIwMC1sOTAtcmo="),
                params = "ggMPOg1uX09LWkhnTjRGRUJh"
            )        )

        val GENRE_CATEGORIES: List<RadioCategory> = listOf(
            RadioCategory(
                id = "genre_african",
                title = "African",
                section = CategorySection.GENRES,
                stripeColor = 0xFF00A513,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2tUeHR4bXpKLTRDS0hfN0lubXd2dUFNcEpuY09nMi1IY1pqU0wyVi0yMU9Rbzg3VEJTR3FtSGRLYWpuRWhUeUtRZEVMd1ZfRmZJZnl2SjQ9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0UzWGxlRE5jMDVk"
            ),
            RadioCategory(
                id = "genre_arabic",
                title = "Arabic",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3JZSmVGTUxJWGhGbXJySy1WR0hlV2Y5cjdIWHdEcFMwbjF3QzhVXzNwVTA5TXNhdnVSUi1oMHZwbDV5OVNwTVBZOWRmc011eE9WaE1Gd0E9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX3VOQWxsblVZTFNE"
            ),
            RadioCategory(
                id = "genre_bengali",
                title = "Bengali",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3BLMkdFaVhQNlkzOVNJcXQweWxvaTU5bG9MbEtKOVF1b3RncGJfd3B0LURQRmNOZzVlcjNYTUUxb1hfTkNJWEZZU19OUWl2ZkppcHZuMWd6PXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX1ZEZzBvYUp0TGNI"
            ),
            RadioCategory(
                id = "genre_bhojpuri",
                title = "Bhojpuri",
                section = CategorySection.GENRES,
                stripeColor = 0xFFAA09AA,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2p2ODAzVTJRZU91XzBTX0FHaktDRGU5Q21CMVUtOFBvaEpEN0t2cFBvTG5zQm54VU5KakNYV2h2RlR0d211UXlnd3VncVdjVWlBMHRlbkRuPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX05iYWtoenBuUkZX"
            ),
            RadioCategory(
                id = "genre_carnatic_classical",
                title = "Carnatic classical",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF8D40,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0NJNjJTV0J4T1AzdjYyb1VlX3EtMGV0OGZHNXc0bGkzU3Jra2ZTNTJ2RlJLY19EcldVMHpPQVM5ZFRVNll4enhmT2FuR0haS1FMUEYtSFFxblE9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX2tuQ3JyczRkeEJK"
            ),
            RadioCategory(
                id = "genre_classical",
                title = "Classical",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCCCCCC,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2d0MlZMa28wVlBlR3BFd1p3ZW41MEJRN0tMVENDX0xhMVo1bzJicFJmZTRtcVFha1JjZGN0MWFkV1YyUWtuNUhaVU9vYjlpQUw1QVU2UE09dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX1N4VmduTmdUR3dm"
            ),
            RadioCategory(
                id = "genre_country_and_americana",
                title = "Country & Americana",
                section = CategorySection.GENRES,
                stripeColor = 0xFF337DFF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3BYRlFDYTdhNGc4eWFMUU91RTJ5MHpXSzRYMEE5S2RqeDFQVENfS0lDWmM5T0VnYWhzRmwtRjZWQmloQk5PTEFpWVg3WG51UHFhQzRZaW89dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX1RXcFlyZEpRb1d3"
            ),
            RadioCategory(
                id = "genre_dance_and_electronic",
                title = "Dance & electronic",
                section = CategorySection.GENRES,
                stripeColor = 0xFF0092BF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3Zab1ZjTXNRSTBnZ1hYVFRMcDB3SVR2bFJGZTJ5SzlkZ3dRcjZ4Q2xqd19FYlVwNHJEQlBVaUFoLUktU3BQWjZsSFRabnpQRV8wNUtfbUE9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX1NPTld3SDN3WGs4"
            ),
            RadioCategory(
                id = "genre_decades",
                title = "Decades",
                section = CategorySection.GENRES,
                stripeColor = 0xFFA4FFA4,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1B6cGp6Q2N2T09XTjdmUklHekk1c3UwVUVidGJGMXZhOFZiMXlsTFV3TG5UeElqd09QMnR3cjdUUGZvUjdaQlhNVWZ1MDlINWExNmJjNm94PXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX253QXk4VXN5NGdj"
            ),
            RadioCategory(
                id = "genre_desi_hip_hop",
                title = "Desi hip-hop",
                section = CategorySection.GENRES,
                stripeColor = 0xFF606060,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1M4ajJwYWZzUHF1S3ZGUHYxc3ZwaUZ3aHVOdWlFV203dXpac3d6Y0FwT0ZyMzVHV3hDSVhnTHU1Q1A5VVB6cWVvWFVQRDF1U0Q5RGQ5NUE9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX3VtYUhGSmtKdlhr"
            ),
            RadioCategory(
                id = "genre_devotional",
                title = "Devotional",
                section = CategorySection.GENRES,
                stripeColor = 0xFF606060,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1p4LUUxWGhPS2FfOUM1T210aWxERFoxdXd1YUZTWnhqM1B5S1FPUE51clJJeWQ0UEJfQWVLaXhXTUZHN09YZFNoTUZfVmdIbHNiLXE1RlE9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX3g1dEo4cmZVY1Jm"
            ),
            RadioCategory(
                id = "genre_family",
                title = "Family",
                section = CategorySection.GENRES,
                stripeColor = 0xFF0092BF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL29OVWtlLWF6WmVfMzlIbjBBR29Kc19wekhSMFBfSFdGNk5zbXdIeklZeFl1WTFaZXY1eEhUUEtpaEdrQlU1RkpVSHVZLVdiS3RxVWZ1Q09uWGc9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uXzMyY3J2SGM0bVh5"
            ),
            RadioCategory(
                id = "genre_folk_and_acoustic",
                title = "Folk & acoustic",
                section = CategorySection.GENRES,
                stripeColor = 0xFF00A513,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0dLQzNuaG1HTE96bm9yZ1dzaWQwb3dVNm5RN0F4VXhfd1gybHhEbWZnczRFMkJSajktcVRJbmtDNVZRUm9oR2ZkODgyd3l4UnBaUTg5cnhtPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uXzBTRFBmQ3N4b0R6"
            ),
            RadioCategory(
                id = "genre_ghazal_sufi",
                title = "Ghazal/sufi",
                section = CategorySection.GENRES,
                stripeColor = 0xFF4DEEFF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0NkSGVNclFTS0lxdVNpX094RGg0cGU1d1VJQjhYS3pXTlZHRGo1bm81ZlFjQzdnN3dvalNpd3h6U0NnZmNCVS1hMTRBNFJVdE9BODlQcV9GPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX1FyMnBXRmhFakpn"
            ),
            RadioCategory(
                id = "genre_gujarati",
                title = "Gujarati",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL01sT2U1Zm5ZZUJMYjEyemI4WEVCb3psNmEyZlFDdTZzbjhrc1dMWTUwLUJfNnBOSFh0MnliQ2VGNHNPMV90ZFlQQjFqWWs1bnJsTzhzbnFkPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX0RXaUs0U3dTcWtD"
            ),
            RadioCategory(
                id = "genre_haryanvi",
                title = "Haryanvi",
                section = CategorySection.GENRES,
                stripeColor = 0xFF0092BF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0FxOWFvQW1nak9CZ2EtbnJ3UnhLTWRyV2FpYjdFRFM3OEFsVkI2Y3QwODRIcnJPUVZOaHViVkJ6cXAxaVkzZUFtNlQtLVFuYXFrYWduclk9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uXzVTb3lITFZwQ2w1"
            ),
            RadioCategory(
                id = "genre_hindi",
                title = "Hindi",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzBsUHA1U0dSbkdVbjBOV1F2QkNJT05OWlZlN1Q0UFR0Z3ZGU2c5VHF4M1VIVWpxdTAtRHBxQy1rcFNBcGt3YTA0Z2RpLTduaFc1blNtajA2Y1E9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX2ZvbzNJMzJwRkFT"
            ),
            RadioCategory(
                id = "genre_hindustani_classical",
                title = "Hindustani classical",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0tZdzc0WFNRd3RLUGJaVHJITU5FQkFuRU1nMVAxZ05Hd3ltblp3QlNqc3RicVNFLU1waWdHbFRJeTZJWnZDLUVSbFJrZVAwYzdWVGlaT2JTPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX1FEVXVJSkdrdVgw"
            ),
            RadioCategory(
                id = "genre_hip_hop",
                title = "Hip-hop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0lCd0s0TE9zNHNPLUNkLV81WU80WEMyQjJOMWh2eUJ2S2ZyNjB0VF9sakl1d21PdVZXV1cyTnlwYmN2ektnU2NWQWJSc1dFWWJtc2pmY20yY3c9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0M2dmRieXNxTW1s"
            ),
            RadioCategory(
                id = "genre_indian_indie",
                title = "Indian indie",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCCCCCC,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL25hS2dPXzl2dkljenVmN1ZxMWxsUXlSQVFPT1c4OThrQlpOM3Bpby1Ca2JmY21kdTNHdjE0X2l2RUJaaUhBb3c4VmJQRXExYmhPMGoyRFU9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX3FzMXBrNWlUMWNH"
            ),
            RadioCategory(
                id = "genre_indian_pop",
                title = "Indian pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFA4FFA4,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzBRUnEwaFk1aHNKRVFmWWdMWWlEbE1DMlJiWUFBcjVOQ2tyMDF4UXRjVGtQenhnT3hTRXgyUzhmenFyMXJQdTd5aGxNUjY4Zk9RM3NLYm45PXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uXzNleFNpSmk2TTcy"
            ),
            RadioCategory(
                id = "genre_indie_and_alternative",
                title = "Indie & alternative",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCCCCCC,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3dlWVFXZkV3V05QT3VBbTM0Z2VYTjFMa1NZUGxzSmF5NzhOblFnSEMzUEtzeVpjZHZCSElzTXRxb0ZoM3Jpb0E0WGdNZEhNUWQzaDZ2SDZtYkE9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX21NWWpBbU01SDgy"
            ),
            RadioCategory(
                id = "genre_j_pop",
                title = "J-Pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1I0azhGajNUMVktTHZsU3JNSVk2ZWtwS2c1T3ZpUXF6YU5UcTRXb0wyUUh3QmpjNkMwQ0hPTWw3UjhLYmR5ajhhdFdXa3BzWWkxSFBDYm5VPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uXzAwSjVITDBZckJR"
            ),
            RadioCategory(
                id = "genre_jazz",
                title = "Jazz",
                section = CategorySection.GENRES,
                stripeColor = 0xFFA4C5FF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2VKM0Joa3ZOM01tTFp0OTViQkxHLWFxR2RVdm9BMUZfQVdJeVBPcFV4NjlSdl9Xc0dsUU1PSFdTSE1ENDhNcHB3V3BwSHBRV3dqSVIyTFRBPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX3lPcDFRaE9wM1BS"
            ),
            RadioCategory(
                id = "genre_k_pop",
                title = "K-Pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFB47BFF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0dyaVBiNG51eTB5a2hCVk8wczFLMGFZUUJyYm9zaV9NTDFGMEhZT0NySG91T0ZUcFVJbTF6aWpVejdjWlZOb1ZmZF9YUFdtX0RFRWhHVms9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0JrbjBDOFFPSzJW"
            ),
            RadioCategory(
                id = "genre_kannada",
                title = "Kannada",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFC200,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL29LUjAzNGh2RkU1NmZwWFlTdmZDYjN3dzVmcjduZTNGalJBckdyV0Jmc3BKNmgxZWdFbm9lTEFjQ3lPOGZpcTU0RnpPM0taazBVOVJmd2JZPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX29zOEp0MzY3djA5"
            ),
            RadioCategory(
                id = "genre_latin",
                title = "Latin",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFC200,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1FEcHIwZWZISXV5NzFRd1lXSDVIeHh2Rmk3cm5fbk12SmUtbjRQWFFiOFdZM2hOWGtMYlhQdGd1MVZDVUtjbkFkcmZwRmlTTEo2OEprenBQPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX29wWTRjMHV1dWN5"
            ),
            RadioCategory(
                id = "genre_malayalam",
                title = "Malayalam",
                section = CategorySection.GENRES,
                stripeColor = 0xFF00A513,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1k0VWRfQTV3Qkg5UUVNZEl3czIzM0Z5d0U4cllqc3RJXzlIWWxTOC1hcVd3Y0Z6R1BZUHhJU2huTmdxUUlKek8wVkFhZXJCSFNjOVFfalpTPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX050dDRiSWN3R1ZN"
            ),
            RadioCategory(
                id = "genre_marathi",
                title = "Marathi",
                section = CategorySection.GENRES,
                stripeColor = 0xFFB47BFF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0JnQWRDYXRsT0o5eWY1QmZWTlpyUkpiR1U2UzcxZG5KWVdyeGxsVVBMNW9xajFlVUZHU0FSUTBrT3lPVUtScy1BYzJfLTlGODl2d3RsLU1HZmc9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX2Y4V2dyOWVRUHZm"
            ),
            RadioCategory(
                id = "genre_metal",
                title = "Metal",
                section = CategorySection.GENRES,
                stripeColor = 0xFF8C8C8C,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1JxaDJIREFELUZqMS1vSFlxVmtsUEdrbE1ISVYzcjZTTVBNWm1GRTNGbExpV2VCQm5QM1NDRVNmZ2VOZHpRLURtcENJdk9HRTRKMGh6VnhhPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uXzdlSXhKZ0hMV1Z4"
            ),
            RadioCategory(
                id = "genre_monsoon",
                title = "Monsoon",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3o5RDR3c1VxVHkwUXZiejcxX2tETkhWb3ItVkpjaHNpYUE4U1llaFA5UUxUMjlMd1FsckZiaHFFTTh2bHVmb2NuOGNua2xONmR1OVJXQT13MTIwMC1oMTIwMC1sOTAtcmo="),
                params = "ggMPOg1uX2FGWmM5SHVqYlJX"
            ),
            RadioCategory(
                id = "genre_pop",
                title = "Pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3lua3BzSWRzWWUxZlZFT3BKZUFPX1N6UUhKRjNyc3VqaFE0RXNtdUlkVkthdmJObTV1RHZEVEJTaEs2YlBqNEk3U2o5WWQ2WnZwbmkyS3R1N2c9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX1lLQkxHbHhWQUUy"
            ),
            RadioCategory(
                id = "genre_punjabi",
                title = "Punjabi",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL09fQm0xemV2QWk4Mm40WWRMRkc2amMtb2NCNlJGM0ZRSEZPdHozdmM0TW5JWjM2Z1pYdjVoMWxZalFpUkZqYUxyNUpNVU44eHRDc2FzZmpXPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX1ZKNkRodjF2YWxv"
            ),
            RadioCategory(
                id = "genre_randb_and_soul",
                title = "R&B & soul",
                section = CategorySection.GENRES,
                stripeColor = 0xFF7B3EDB,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0tvQ2hsbFdpRGpfd2c3S3lSeG1WMjN2QnRJRDNPSE0xWUFzWXh6VmZDWHBySGw1eFc2dHhVQ0pFakNaU3UzUml2TkNHbTc4TVhyUUxsT1U9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX2JxQ2hxc2J5UFhR"
            ),
            RadioCategory(
                id = "genre_reggae_and_caribbean",
                title = "Reggae & caribbean",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZwenVhbTlpYWVjbG93V2VaRkZPcXpRb1JCRFFZUFB3ZnZ0aVRYUHlvVzVsSXY0RThDdDVKZWRjNDMtVFZFdEcyWWlRX1JoTEhjanlYOXlWPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uX1JUc2lFcDFuUUth"
            ),
            RadioCategory(
                id = "genre_rock",
                title = "Rock",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCC0000,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZPV1Z4eWtZTkQwRmttVnZqa3h3dlAzdDhFUXRKaTNkZzFvYkUyYzFNZmhGTG1iVXdiZnpHWU9LRjNWRFNLWHMtTVpOaVlVeFBDc2t6dWNJPXcxMjAwLWgxMjAwLWw5MC1yag=="),
                params = "ggMPOg1uXzJKTm5jUEZ5Uzlu"
            ),
            RadioCategory(
                id = "genre_tamil",
                title = "Tamil",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCC0000,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL3RFVHc3TU03LVM3Um1WckRhbEptbXA2cjhHYkp3T2JRenVBRFFtVVo2djFFTVlaQk96VlhwQThWa3BUNm12RlBrc0ttOWs5Q2syeG1fdWs9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX2p2emtjU3J3ZVFB"
            ),
            RadioCategory(
                id = "genre_telugu",
                title = "Telugu",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0h5dk9HOGpQZ1hOMlFrb2pQM2ZPcGVuQzliVkxGSzE1eFNraHpJdzRHN1FLeWYxd05hek9tdXNsRGZiRlZtdk9WMlRVQXVtTkFxMzRpV0E9dzEyMDAtaDEyMDAtbDkwLXJq"),
                params = "ggMPOg1uX0syaEVmTXhSOVl6"
            )
        )

        val CATEGORIES: List<RadioCategory> = MOOD_CATEGORIES + GENRE_CATEGORIES

        val CURATED_STATIONS: List<RadioStation> = listOf(
            // ── Chill & Lo-Fi ──
            RadioStation(
                id = "stream_lofi_girl",
                name = "Lofi Girl Beats",
                genre = "Lo-Fi",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9wbGF5LnN0cmVhbWFmcmljYS5uZXQvbG9maXJhZGlv"),
                description = "24/7 relaxing lofi hip hop radio - beats to relax/study to",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0NEdDRSakhER3IwWWlYNld4QVJUQkZrZGI5azlWc0FJbTg4c0pYWjdCM08xeU1vUzUza0xTX2R5OFpJS1JyRlJISENCNk9KZVBXVTFHYlk9dzUwMC1oNTAwLWw5MC1yag==")
            ),
            RadioStation(
                id = "stream_box_lofi",
                name = "Box Lofi Chill",
                genre = "Lo-Fi",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW1zLmlsb3ZlbXVzaWMuZGUvaWxvdmVyYWRpbzE3Lm1wMw=="),
                description = "Mellow instrumental hip-hop and chillhop vibes",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLzlJNDVTWVpBZlRlUjE3QnlWOWkyeURwV2hRNURUV01DclczTGVpOHZNSWlaemV5VW5mMnpSRlVEQm9iNFRHd2dVa1hBQVJfeDFYYThWQT13NTAwLWg1MDAtbDkwLXJq")
            ),
            RadioStation(
                id = "stream_chillhop",
                name = "ChillHop Radio",
                genre = "Lo-Fi",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cDovL3N0cmVhbS56ZW5vLmZtL2Yzd3ZiYnFtZGc4dXY="),
                description = "Smooth urban jazz beats and lo-fi coffeehop",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0FtM2lJR3pGNUR6LThlMm5LSk1RZm41VEVfMWFBVDJ3VU9LSEpKMnh6ZlNvUzBMVDRYVjZwT1pheF9tS0xjMkhhckhicW5tX1ZRMHNZOXZGPXc1MDAtaDUwMC1sOTAtcmo=")
            ),

            // ── Synthwave & Retro ──
            RadioStation(
                id = "stream_synthwave",
                name = "Nightwave Synthwave",
                genre = "Synthwave",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0uemVuby5mbS93NzR4MHEyZGRlenV2"),
                description = "Retrowave, Darksynth & 80s electronic driving frequencies",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0ZPV1Z4eWtZTkQwRmttVnZqa3h3dlAzdDhFUXRKaTNkZzFvYkUyYzFNZmhGTG1iVXdiZnpHWU9LRjNWRFNLWHMtTVpOaVlVeFBDc2t6dWNJPXc1MDAtaDUwMC1sOTAtcmo=")
            ),
            RadioStation(
                id = "stream_plaza",
                name = "Nightwave Plaza",
                genre = "Vaporwave",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9yYWRpby5wbGF6YS5vbmUvbXAz"),
                description = "Nostalgic vaporwave, future funk and mallsoft aesthetics",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0tvQ2hsbFdpRGpfd2c3S3lSeG1WMjN2QnRJRDNPSE0xWUFzWXh6VmZDWHBySGw1eFc2dHhVQ0pFakNaU3UzUml2TkNHbTc4TVhyUUxsT1U9dzUwMC1oNTAwLWw5MC1yag==")
            ),
            RadioStation(
                id = "stream_retrowave",
                name = "Retro Synthwave 80s",
                genre = "Synthwave",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0uemVuby5mbS83Y3Z2dXZxbWRnOHV2"),
                description = "Pure 80s analog synthesizers and neon outrun soundtracks",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL0h5dk9HOGpQZ1hOMlFrb2pQM2ZPcGVuQzliVkxGSzE1eFNraHpJdzRHN1FLeWYxd05hek9tdXNsRGZiRlZtdk9WMlRVQXVtTkFxMzRpV0E9dzUwMC1oNTAwLWw5MC1yag==")
            ),

            // ── Rock & Alternative ──
            RadioStation(
                id = "stream_rp_rock",
                name = "Radio Paradise Rock",
                genre = "Rock",
                categoryId = "genre_rock",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0ucmFkaW9wYXJhZGlzZS5jb20vcm9jay0xMjg="),
                description = "Curated modern and classic rock mix with no commercials",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_rock_classic",
                name = "Rock Antenne Classic",
                genre = "Classic Rock",
                categoryId = "genre_rock",
                streamUrl = b64u("aHR0cHM6Ly9zMi13ZWJyYWRpby5hbnRlbm5lLmRlL3JvY2stYW50ZW5uZQ=="),
                description = "Legendary guitar anthems, stadium rock and metal hits",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_rp_main",
                name = "Radio Paradise Main",
                genre = "Eclectic Rock",
                categoryId = "genre_rock",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0ucmFkaW9wYXJhZGlzZS5jb20vbXAzLTEyOA=="),
                description = "Unique blend of acoustic, indie rock and world rhythm",
                bitrate = "128 kbps"
            ),

            // ── Electronic & Dance ──
            RadioStation(
                id = "stream_defcon",
                name = "SomaFM DEF CON Radio",
                genre = "Electronic",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9pY2UyLnNvbWFmbS5jb20vZGVmY29uLTEyOC1tcDM="),
                description = "Music for hacking. The soundtrack for the DEF CON conference",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_dance_wave",
                name = "Dance Wave Hits",
                genre = "Dance / EDM",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9kYW5jZXdhdmUub25saW5lL2RhbmNlLm1wMw=="),
                description = "Non-stop modern dance, house, and electronic club tracks",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_ibiza",
                name = "Ibiza Global Radio",
                genre = "House / Club",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0uaWJpemFnbG9iYWxyYWRpby5jb206ODAyNC9pYml6YWdsb2JhbHJhZGlvLm1wMw=="),
                description = "Live underground deep house and electronic sets from Ibiza",
                bitrate = "128 kbps"
            ),

            // ── Ambient & Space ──
            RadioStation(
                id = "stream_groove_salad",
                name = "SomaFM Groove Salad",
                genre = "Ambient",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9pY2UyLnNvbWFmbS5jb20vZ3Jvb3Zlc2FsYWQtMTI4LW1wMw=="),
                description = "A nicely chilled plate of ambient/downtempo beats and grooves",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1N3eW93S2Uta0RMb2tEMndFb3VQbGxKd2pmS2xHNzNwb2RhRkRIQnphRURlUjBtdTNtZ3AweDRpeTdvdVRERDBKdWtzanpHM09iWnNmUT13NTAwLWg1MDAtbDkwLXJq")
            ),
            RadioStation(
                id = "stream_deep_space",
                name = "SomaFM Deep Space One",
                genre = "Space Ambient",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9pY2UyLnNvbWFmbS5jb20vZGVlcHNwYWNlb25lLTEyOC1tcDM="),
                description = "Deep ambient electronic and space music for exploration",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1N3eW93S2Uta0RMb2tEMndFb3VQbGxKd2pmS2xHNzNwb2RhRkRIQnphRURlUjBtdTNtZ3AweDRpeTdvdVRERDBKdWtzanpHM09iWnNmUT13NTAwLWg1MDAtbDkwLXJq")
            ),
            RadioStation(
                id = "stream_drone_zone",
                name = "SomaFM Drone Zone",
                genre = "Drone Ambient",
                categoryId = "genre_dance_and_electronic",
                streamUrl = b64u("aHR0cHM6Ly9pY2UyLnNvbWFmbS5jb20vZHJvbmV6b25lLTEyOC1tcDM="),
                description = "Atmospheric, served best with headphones for highway drives",
                bitrate = "128 kbps",
                imageUrl = b64u("aHR0cHM6Ly95dDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL1N3eW93S2Uta0RMb2tEMndFb3VQbGxKd2pmS2xHNzNwb2RhRkRIQnphRURlUjBtdTNtZ3AweDRpeTdvdVRERDBKdWtzanpHM09iWnNmUT13NTAwLWg1MDAtbDkwLXJq")
            ),

            // ── Jazz & Soul ──
            RadioStation(
                id = "stream_swiss_jazz",
                name = "Radio Swiss Jazz",
                genre = "Jazz",
                categoryId = "genre_jazz",
                streamUrl = b64u("aHR0cDovL3N0cmVhbS5zcmctc3NyLmNoL20vcnNqL21wM18xMjg="),
                description = "Smooth jazz, swing, bebop, and soul classics 24/7",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_linn_jazz",
                name = "Linn Jazz UK",
                genre = "Audiophile Jazz",
                categoryId = "genre_jazz",
                streamUrl = b64u("aHR0cDovL3JhZGlvLmxpbm4uY28udWs6ODAwMC9hdXRvZGo="),
                description = "Studio quality jazz masters and acoustic soul recordings",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_smooth_jazz",
                name = "Smooth Jazz Florida",
                genre = "Smooth Jazz",
                categoryId = "genre_jazz",
                streamUrl = b64u("aHR0cHM6Ly9zdHJlYW0uemVuby5mbS8wNTQ1czBobWRnOHV2"),
                description = "Contemporary jazz, saxophone melodies, and relaxing grooves",
                bitrate = "128 kbps"
            ),

            // ── Classical & Focus ──
            RadioStation(
                id = "stream_swiss_classic",
                name = "Radio Swiss Classic",
                genre = "Classical",
                categoryId = "genre_classical",
                streamUrl = b64u("aHR0cDovL3N0cmVhbS5zcmctc3NyLmNoL20vcnNjX2RlL21wM18xMjg="),
                description = "Timeless classical music, symphonies, and chamber performances",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_classic_fm",
                name = "Classic FM UK",
                genre = "Classical",
                categoryId = "genre_classical",
                streamUrl = b64u("aHR0cHM6Ly9tZWRpYS1zc2wubXVzaWNyYWRpby5jb20vQ2xhc3NpY0ZNTVAz"),
                description = "The world's greatest music station with celebrated concertos",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_linn_classical",
                name = "Linn Classical",
                genre = "Symphonic",
                categoryId = "genre_classical",
                streamUrl = b64u("aHR0cDovL3JhZGlvLmxpbm4uY28udWs6ODAwNC9hdXRvZGo="),
                description = "Uncompressed audiophile recordings of philharmonic orchestras",
                bitrate = "128 kbps"
            ),

            // ── News & Talk ──
            RadioStation(
                id = "stream_bbc_world",
                name = "BBC World Service",
                genre = "News",
                categoryId = "mood_commute",
                streamUrl = b64u("aHR0cDovL3N0cmVhbS5saXZlLnZjLmJiY21lZGlhLmNvLnVrL2JiY193b3JsZF9zZXJ2aWNl"),
                description = "International news, analysis and current affairs worldwide",
                bitrate = "96 kbps"
            ),
            RadioStation(
                id = "stream_npr_news",
                name = "NPR News Live",
                genre = "News / Talk",
                categoryId = "mood_commute",
                streamUrl = b64u("aHR0cHM6Ly9ucHItaWNlLnN0cmVhbWd1eXMxLmNvbS9saXZlLm1wMw=="),
                description = "National Public Radio live reporting and documentary talk",
                bitrate = "96 kbps"
            )
        )
    }
}
