package com.aashik.music.model

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
                imageUrl = "https://yt3.googleusercontent.com/CDt4RjHDGr0YiX6WxARTBFkdb9k9VsAIm88sJXZ7B3O1yMoS53kLS_dy8ZIKRrFRHHCB6OJePWU1GbY=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1JOQWZFeDByc2Jm"
            ),
            RadioCategory(
                id = "mood_commute",
                title = "Commute",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFFFC200,
                imageUrl = "https://yt3.googleusercontent.com/TALQkjUiJF2lNPNzYAFFAvJfJPiLHtSHF2YbtXKLA84h-ZaN7X8scDed7OTAiFgKx6CZ4K2ZAZyqUUw=w1200-h1200-l90-rj",
                params = "ggMPOg1uX044Z2o5WERLckpU"
            ),
            RadioCategory(
                id = "mood_diwali",
                title = "Diwali",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF606060,
                imageUrl = "https://yt3.googleusercontent.com/HFR703oTI5lyhzTdsn1Zo_Npm9QxtIMyVzz7JzzD1rxdgfESj97ckbb2cw7Ao-s5nmZ6AX56Xs3jLQ=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0lQVm1ablYxWml5"
            ),
            RadioCategory(
                id = "mood_energize",
                title = "Energize",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFFFE780,
                imageUrl = "https://yt3.googleusercontent.com/PiHeLTzGupMv0PkTI0dg-sstkzOx8yNkBOEfV67yP_sUkJtXLRk4JO8dwhz18rZAz9gHJSAGHeztsJ4=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2lRZUZiMnNrQnJW"
            ),
            RadioCategory(
                id = "mood_feel_good",
                title = "Feel good",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFA4FFA4,
                imageUrl = "https://yt3.googleusercontent.com/Am3iIGzF5Dz-8e2nKJMQfn5TE_1aAT2wUOKHJJ2xzfSoS0LT4XV6pOZax_mKLc2HarHbqnm_VQ0sY9vF=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzZQbDB5eThLRTQ3"
            ),
            RadioCategory(
                id = "mood_focus",
                title = "Focus",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFCCCCCC,
                imageUrl = "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0NvNGNhWThMYWRh"
            ),
            RadioCategory(
                id = "mood_gaming",
                title = "Gaming",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF606060,
                imageUrl = "https://yt3.googleusercontent.com/QrISu_Bk4sR6MN_TUD2bbWO4ZyfdSFJVE8JPpCD4P9uCRmyQtE1e3HjxtwsiomjiR0idTKD7Wl6w9A=w1200-h1200-l90-rj",
                params = "ggMPOg1uX3NmUVV4Vzl3WGQ0"
            ),
            RadioCategory(
                id = "mood_party",
                title = "Party",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFB47BFF,
                imageUrl = "https://yt3.googleusercontent.com/kHXGXRuL3_9v5rKBGrGX_-TY3HMrMvuTO1cHaD_dGZS04YFp1WxF8hXXmTwjXSv-FVxNlJE201hPoKI=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0pmQ0s2V0JRclZs"
            ),
            RadioCategory(
                id = "mood_romance",
                title = "Romance",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFCC0000,
                imageUrl = "https://yt3.googleusercontent.com/HEdSa36FoDLbUaGWynkGzG_QFFElSh2RpOF4UOXgPwWTUkbqynoWw8W5trrQAnQM76PgryWuXYTLMgw=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0FzQ2FhZWtUY211"
            ),
            RadioCategory(
                id = "mood_sad",
                title = "Sad",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF8C8C8C,
                imageUrl = "https://yt3.googleusercontent.com/q4iCQuoHBeumL9s0LMTIiCMeYR2EdIlNp_YAKiKUjlehuF8-lRO-_PvCplt2TCvKQV3ECPQGZU_pmrQ=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0JLQ0gySWZKZVY1"
            ),
            RadioCategory(
                id = "mood_sleep",
                title = "Sleep",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFF7B3EDB,
                imageUrl = "https://yt3.googleusercontent.com/SwyowKe-kDLokD2wEouPllJwjfKlG73podaFDHBzaEDeR0mu3mgp0x4iy7ouTDD0JuksjzG3ObZsfQ=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1MxaFQ3Z0JMZkN4"
            ),
            RadioCategory(
                id = "mood_workout",
                title = "Workout",
                section = CategorySection.MOODS_AND_MOMENTS,
                stripeColor = 0xFFFF8D40,
                imageUrl = "https://yt3.googleusercontent.com/ugsYfFh0MWr_EITP7kg1K3PMeDx2HC_EkEqQzD7-ww31VEwGHJPuCXIdcxBAqJGc8z4o9Q9shyxQsA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX09LWkhnTjRGRUJh"
            )        )

        val GENRE_CATEGORIES: List<RadioCategory> = listOf(
            RadioCategory(
                id = "genre_african",
                title = "African",
                section = CategorySection.GENRES,
                stripeColor = 0xFF00A513,
                imageUrl = "https://yt3.googleusercontent.com/kTxtxmzJ-4CKH_7InmwvuAMpJncOg2-HcZjSL2V-21OQo87TBSGqmHdKajnEhTyKQdELwV_FfIfyvJ4=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0UzWGxlRE5jMDVk"
            ),
            RadioCategory(
                id = "genre_arabic",
                title = "Arabic",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = "https://yt3.googleusercontent.com/rYJeFMLIXhFmrrK-VGHeWf9r7HXwDpS0n1wC8U_3pU09MsavuRR-h0vpl5y9SpMPY9dfsMuxOVhMFwA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX3VOQWxsblVZTFNE"
            ),
            RadioCategory(
                id = "genre_bengali",
                title = "Bengali",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = "https://yt3.googleusercontent.com/pK2GEiXP6Y39SIqt0yloi59loLlKJ9Quotgpb_wpt-DPFcNg5er3XME1oX_NCIXFYS_NQivfJipvn1gz=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1ZEZzBvYUp0TGNI"
            ),
            RadioCategory(
                id = "genre_bhojpuri",
                title = "Bhojpuri",
                section = CategorySection.GENRES,
                stripeColor = 0xFFAA09AA,
                imageUrl = "https://yt3.googleusercontent.com/jv803U2QeOu_0S_AGjKCDe9CmB1U-8PohJD7KvpPoLnsBnxUNJjCXWhvFTtwmuQygwugqWcUiA0tenDn=w1200-h1200-l90-rj",
                params = "ggMPOg1uX05iYWtoenBuUkZX"
            ),
            RadioCategory(
                id = "genre_carnatic_classical",
                title = "Carnatic classical",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF8D40,
                imageUrl = "https://yt3.googleusercontent.com/CI62SWBxOP3v62oUe_q-0et8fG5w4li3SrkkfS52vFRKc_DrWU0zOAS9dTU6YxzxfOanGHZKQLPF-HQqnQ=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2tuQ3JyczRkeEJK"
            ),
            RadioCategory(
                id = "genre_classical",
                title = "Classical",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCCCCCC,
                imageUrl = "https://yt3.googleusercontent.com/gt2VLko0VPeGpEwZwen50BQ7KLTCC_La1Z5o2bpRfe4mqQakRcdct1adWV2Qkn5HZUOob9iAL5AU6PM=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1N4VmduTmdUR3dm"
            ),
            RadioCategory(
                id = "genre_country_and_americana",
                title = "Country & Americana",
                section = CategorySection.GENRES,
                stripeColor = 0xFF337DFF,
                imageUrl = "https://yt3.googleusercontent.com/pXFQCa7a4g8yaLQOuE2y0zWK4X0A9Kdjx1PTC_KICZc9OEgahsFl-F6VBihBNOLAiYX7XnuPqaC4Yio=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1RXcFlyZEpRb1d3"
            ),
            RadioCategory(
                id = "genre_dance_and_electronic",
                title = "Dance & electronic",
                section = CategorySection.GENRES,
                stripeColor = 0xFF0092BF,
                imageUrl = "https://yt3.googleusercontent.com/vZoVcMsQI0ggXXTTLp0wITvlRFe2yK9dgwQr6xCljw_EbUp4rDBPUiAh-I-SpPZ6lHTZnzPE_05K_mA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1NPTld3SDN3WGs4"
            ),
            RadioCategory(
                id = "genre_decades",
                title = "Decades",
                section = CategorySection.GENRES,
                stripeColor = 0xFFA4FFA4,
                imageUrl = "https://yt3.googleusercontent.com/PzpjzCcvOOWN7fRIGzI5su0UEbtbF1va8Vb1ylLUwLnTxIjwOP2twr7TPfoR7ZBXMUfu09H5a16bc6ox=w1200-h1200-l90-rj",
                params = "ggMPOg1uX253QXk4VXN5NGdj"
            ),
            RadioCategory(
                id = "genre_desi_hip_hop",
                title = "Desi hip-hop",
                section = CategorySection.GENRES,
                stripeColor = 0xFF606060,
                imageUrl = "https://yt3.googleusercontent.com/S8j2pafsPquKvFPv1svpiFwhuNuiEWm7uzZswzcApOFr35GWxCIXgLu5CP9UPzqeoXUPD1uSD9Dd95A=w1200-h1200-l90-rj",
                params = "ggMPOg1uX3VtYUhGSmtKdlhr"
            ),
            RadioCategory(
                id = "genre_devotional",
                title = "Devotional",
                section = CategorySection.GENRES,
                stripeColor = 0xFF606060,
                imageUrl = "https://yt3.googleusercontent.com/Zx-E1XhOKa_9C5OmtilDDZ1uwuaFSZxj3PyKQOPNurRIyd4PB_AeKixWMFG7OXdShMF_VgHlsb-q5FQ=w1200-h1200-l90-rj",
                params = "ggMPOg1uX3g1dEo4cmZVY1Jm"
            ),
            RadioCategory(
                id = "genre_family",
                title = "Family",
                section = CategorySection.GENRES,
                stripeColor = 0xFF0092BF,
                imageUrl = "https://yt3.googleusercontent.com/oNUke-azZe_39Hn0AGoJs_pzHR0P_HWF6NsmwHzIYxYuY1Zev5xHTPKihGkBU5FJUHuY-WbKtqUfuCOnXg=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzMyY3J2SGM0bVh5"
            ),
            RadioCategory(
                id = "genre_folk_and_acoustic",
                title = "Folk & acoustic",
                section = CategorySection.GENRES,
                stripeColor = 0xFF00A513,
                imageUrl = "https://yt3.googleusercontent.com/GKC3nhmGLOznorgWsid0owU6nQ7AxUx_wX2lxDmfgs4E2BRj9-qTInkC5VQRohGfd882wyxRpZQ89rxm=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzBTRFBmQ3N4b0R6"
            ),
            RadioCategory(
                id = "genre_ghazal_sufi",
                title = "Ghazal/sufi",
                section = CategorySection.GENRES,
                stripeColor = 0xFF4DEEFF,
                imageUrl = "https://yt3.googleusercontent.com/CdHeMrQSKIquSi_OxDh4pe5wUIB8XKzWNVGDj5no5fQcC7g7wojSiwxzSCgfcBU-a14A4RUtOA89Pq_F=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1FyMnBXRmhFakpn"
            ),
            RadioCategory(
                id = "genre_gujarati",
                title = "Gujarati",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = "https://yt3.googleusercontent.com/MlOe5fnYeBLb12zb8XEBozl6a2fQCu6sn8ksWLY50-B_6pNHXt2ybCeF4sO1_tdYPB1jYk5nrlO8snqd=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0RXaUs0U3dTcWtD"
            ),
            RadioCategory(
                id = "genre_haryanvi",
                title = "Haryanvi",
                section = CategorySection.GENRES,
                stripeColor = 0xFF0092BF,
                imageUrl = "https://yt3.googleusercontent.com/Aq9aoAmgjOBga-nrwRxKMdrWaib7EDS78AlVB6ct084HrrOQVNhubVBzqp1iY3eAm6T--QnaqkagnrY=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzVTb3lITFZwQ2w1"
            ),
            RadioCategory(
                id = "genre_hindi",
                title = "Hindi",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = "https://yt3.googleusercontent.com/0lPp5SGRnGUn0NWQvBCIONNZVe7T4PTtgvFSg9Tqx3UHUjqu0-DpqC-kpSApkwa04gdi-7nhW5nSmj06cQ=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2ZvbzNJMzJwRkFT"
            ),
            RadioCategory(
                id = "genre_hindustani_classical",
                title = "Hindustani classical",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = "https://yt3.googleusercontent.com/KYw74XSQwtKPbZTrHMNEBAnEMg1P1gNGwymnZwBSjstbqSE-MpigGlTIy6IZvC-ERlRkeP0c7VTiZObS=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1FEVXVJSkdrdVgw"
            ),
            RadioCategory(
                id = "genre_hip_hop",
                title = "Hip-hop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = "https://yt3.googleusercontent.com/IBwK4LOs4sO-Cd-_5YO4XC2B2N1hvyBvKfr60tT_ljIuwmOuVWWW2NypbcvzKgScVAbRsWEYbmsjfcm2cw=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0M2dmRieXNxTW1s"
            ),
            RadioCategory(
                id = "genre_indian_indie",
                title = "Indian indie",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCCCCCC,
                imageUrl = "https://yt3.googleusercontent.com/naKgO_9vvIczuf7Vq1llQyRAQOOW898kBZN3pio-Bkbfcmdu3Gv14_ivEBZiHAow8VbPEq1bhO0j2DU=w1200-h1200-l90-rj",
                params = "ggMPOg1uX3FzMXBrNWlUMWNH"
            ),
            RadioCategory(
                id = "genre_indian_pop",
                title = "Indian pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFA4FFA4,
                imageUrl = "https://yt3.googleusercontent.com/0QRq0hY5hsJEQfYgLYiDlMC2RbYAAr5NCkr01xQtcTkPzxgOxSEx2S8fzqr1rPu7yhlMR68fOQ3sKbn9=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzNleFNpSmk2TTcy"
            ),
            RadioCategory(
                id = "genre_indie_and_alternative",
                title = "Indie & alternative",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCCCCCC,
                imageUrl = "https://yt3.googleusercontent.com/weYQWfEwWNPOuAm34geXN1LkSYPlsJay78NnQgHC3PKsyZcdvBHIsMtqoFh3rioA4XgMdHMQd3h6vH6mbA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX21NWWpBbU01SDgy"
            ),
            RadioCategory(
                id = "genre_j_pop",
                title = "J-Pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = "https://yt3.googleusercontent.com/R4k8Fj3T1Y-LvlSrMIY6ekpKg5OviQqzaNTq4WoL2QHwBjc6C0CHOMl7R8Kbdyj8atWWkpsYi1HPCbnU=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzAwSjVITDBZckJR"
            ),
            RadioCategory(
                id = "genre_jazz",
                title = "Jazz",
                section = CategorySection.GENRES,
                stripeColor = 0xFFA4C5FF,
                imageUrl = "https://yt3.googleusercontent.com/eJ3BhkvN3MmLZt95bBLG-aqGdUvoA1F_AWIyPOpUx69Rv_WsGlQMOHWSHMD48MppwWppHpQWwjIR2LTA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX3lPcDFRaE9wM1BS"
            ),
            RadioCategory(
                id = "genre_k_pop",
                title = "K-Pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFB47BFF,
                imageUrl = "https://yt3.googleusercontent.com/GriPb4nuy0ykhBVO0s1K0aYQBrbosi_ML1F0HYOCrHouOFTpUIm1zijUz7cZVNoVfd_XPWm_DEEhGVk=w1200-h1200-l90-rj",
                params = "ggMPOg1uX0JrbjBDOFFPSzJW"
            ),
            RadioCategory(
                id = "genre_kannada",
                title = "Kannada",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFC200,
                imageUrl = "https://yt3.googleusercontent.com/oKR034hvFE56fpXYSvfCb3ww5fr7ne3FjRArGrWBfspJ6h1egEnoeLAcCyO8fiq54FzO3KZk0U9RfwbY=w1200-h1200-l90-rj",
                params = "ggMPOg1uX29zOEp0MzY3djA5"
            ),
            RadioCategory(
                id = "genre_latin",
                title = "Latin",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFC200,
                imageUrl = "https://yt3.googleusercontent.com/QDpr0efHIuy71QwYWH5HxxvFi7rn_nMvJe-n4PXQb8WY3hNXkLbXPtgu1VCUKcnAdrfpFiSLJ68JkzpP=w1200-h1200-l90-rj",
                params = "ggMPOg1uX29wWTRjMHV1dWN5"
            ),
            RadioCategory(
                id = "genre_malayalam",
                title = "Malayalam",
                section = CategorySection.GENRES,
                stripeColor = 0xFF00A513,
                imageUrl = "https://yt3.googleusercontent.com/Y4Ud_A5wBH9QEMdIws233FywE8rYjstI_9HYlS8-aqWwcFzGPYPxIShnNgqQIJzO0VAaerBHSc9Q_jZS=w1200-h1200-l90-rj",
                params = "ggMPOg1uX050dDRiSWN3R1ZN"
            ),
            RadioCategory(
                id = "genre_marathi",
                title = "Marathi",
                section = CategorySection.GENRES,
                stripeColor = 0xFFB47BFF,
                imageUrl = "https://yt3.googleusercontent.com/BgAdCatlOJ9yf5BfVNZrRJbGU6S71dnJYWrxllUPL5oqj1eUFGSARQ0kOyOUKRs-Ac2_-9F89vwtl-MGfg=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2Y4V2dyOWVRUHZm"
            ),
            RadioCategory(
                id = "genre_metal",
                title = "Metal",
                section = CategorySection.GENRES,
                stripeColor = 0xFF8C8C8C,
                imageUrl = "https://yt3.googleusercontent.com/Rqh2HDAD-Fj1-oHYqVklPGklMHIV3r6SMPMZmFE3FlLiWeBBnP3SCESfgeNdzQ-DmpCIvOGE4J0hzVxa=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzdlSXhKZ0hMV1Z4"
            ),
            RadioCategory(
                id = "genre_monsoon",
                title = "Monsoon",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = "https://yt3.googleusercontent.com/z9D4wsUqTy0Qvbz71_kDNHVor-VJchsiaA8SYehP9QLT29LwQlrFbhqEM8vlufocn8cnklN6du9RWA=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2FGWmM5SHVqYlJX"
            ),
            RadioCategory(
                id = "genre_pop",
                title = "Pop",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = "https://yt3.googleusercontent.com/ynkpsIdsYe1fVEOpJeAO_SzQHJF3rsujhQ4EsmuIdVKavbNm5uDvDTBShK6bPj4I7Sj9Yd6Zvpni2Ktu7g=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1lLQkxHbHhWQUUy"
            ),
            RadioCategory(
                id = "genre_punjabi",
                title = "Punjabi",
                section = CategorySection.GENRES,
                stripeColor = 0xFFE24B00,
                imageUrl = "https://yt3.googleusercontent.com/O_Bm1zevAi82n4YdLFG6jc-ocB6RF3FQHFOtz3vc4MnIZ36gZXv5h1lYjQiRFjaLr5JMUN8xtCsasfjW=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1ZKNkRodjF2YWxv"
            ),
            RadioCategory(
                id = "genre_randb_and_soul",
                title = "R&B & soul",
                section = CategorySection.GENRES,
                stripeColor = 0xFF7B3EDB,
                imageUrl = "https://yt3.googleusercontent.com/KoChllWiDj_wg7KyRxmV23vBtID3OHM1YAsYxzVfCXprHl5xW6txUCJEjCZSu3RivNCGm78MXrQLlOU=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2JxQ2hxc2J5UFhR"
            ),
            RadioCategory(
                id = "genre_reggae_and_caribbean",
                title = "Reggae & caribbean",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFFE780,
                imageUrl = "https://yt3.googleusercontent.com/Fpzuam9iaeclowWeZFFOqzQoRBDQYPPwfvtiTXPyoW5lIv4E8Ct5Jedc43-TVEtG2YiQ_RhLHcjyX9yV=w1200-h1200-l90-rj",
                params = "ggMPOg1uX1JUc2lFcDFuUUth"
            ),
            RadioCategory(
                id = "genre_rock",
                title = "Rock",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCC0000,
                imageUrl = "https://yt3.googleusercontent.com/FOWVxykYND0FkmVvjkxwvP3t8EQtJi3dg1obE2c1MfhFLmbUwbfzGYOKF3VDSKXs-MZNiYUxPCskzucI=w1200-h1200-l90-rj",
                params = "ggMPOg1uXzJKTm5jUEZ5Uzlu"
            ),
            RadioCategory(
                id = "genre_tamil",
                title = "Tamil",
                section = CategorySection.GENRES,
                stripeColor = 0xFFCC0000,
                imageUrl = "https://yt3.googleusercontent.com/tETw7MM7-S7RmVrDalJmmp6r8GbJwObQzuADQmUZ6v1EMYZBOzVXpA8VkpT6mvFPksKm9k9Ck2xm_uk=w1200-h1200-l90-rj",
                params = "ggMPOg1uX2p2emtjU3J3ZVFB"
            ),
            RadioCategory(
                id = "genre_telugu",
                title = "Telugu",
                section = CategorySection.GENRES,
                stripeColor = 0xFFFF78FF,
                imageUrl = "https://yt3.googleusercontent.com/HyvOG8jPgXN2QkojP3fOpenC9bVLFK15xSkhzIw4G7QKyf1wNazOmuslDfbFVmvOV2TUAumNAq34iWA=w1200-h1200-l90-rj",
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
                streamUrl = "https://play.streamafrica.net/lofiradio",
                description = "24/7 relaxing lofi hip hop radio - beats to relax/study to",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/CDt4RjHDGr0YiX6WxARTBFkdb9k9VsAIm88sJXZ7B3O1yMoS53kLS_dy8ZIKRrFRHHCB6OJePWU1GbY=w500-h500-l90-rj"
            ),
            RadioStation(
                id = "stream_box_lofi",
                name = "Box Lofi Chill",
                genre = "Lo-Fi",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://streams.ilovemusic.de/iloveradio17.mp3",
                description = "Mellow instrumental hip-hop and chillhop vibes",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w500-h500-l90-rj"
            ),
            RadioStation(
                id = "stream_chillhop",
                name = "ChillHop Radio",
                genre = "Lo-Fi",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "http://stream.zeno.fm/f3wvbbqmdg8uv",
                description = "Smooth urban jazz beats and lo-fi coffeehop",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/Am3iIGzF5Dz-8e2nKJMQfn5TE_1aAT2wUOKHJJ2xzfSoS0LT4XV6pOZax_mKLc2HarHbqnm_VQ0sY9vF=w500-h500-l90-rj"
            ),

            // ── Synthwave & Retro ──
            RadioStation(
                id = "stream_synthwave",
                name = "Nightwave Synthwave",
                genre = "Synthwave",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://stream.zeno.fm/w74x0q2ddezuv",
                description = "Retrowave, Darksynth & 80s electronic driving frequencies",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/FOWVxykYND0FkmVvjkxwvP3t8EQtJi3dg1obE2c1MfhFLmbUwbfzGYOKF3VDSKXs-MZNiYUxPCskzucI=w500-h500-l90-rj"
            ),
            RadioStation(
                id = "stream_plaza",
                name = "Nightwave Plaza",
                genre = "Vaporwave",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://radio.plaza.one/mp3",
                description = "Nostalgic vaporwave, future funk and mallsoft aesthetics",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/KoChllWiDj_wg7KyRxmV23vBtID3OHM1YAsYxzVfCXprHl5xW6txUCJEjCZSu3RivNCGm78MXrQLlOU=w500-h500-l90-rj"
            ),
            RadioStation(
                id = "stream_retrowave",
                name = "Retro Synthwave 80s",
                genre = "Synthwave",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://stream.zeno.fm/7cvvuvqmdg8uv",
                description = "Pure 80s analog synthesizers and neon outrun soundtracks",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/HyvOG8jPgXN2QkojP3fOpenC9bVLFK15xSkhzIw4G7QKyf1wNazOmuslDfbFVmvOV2TUAumNAq34iWA=w500-h500-l90-rj"
            ),

            // ── Rock & Alternative ──
            RadioStation(
                id = "stream_rp_rock",
                name = "Radio Paradise Rock",
                genre = "Rock",
                categoryId = "genre_rock",
                streamUrl = "https://stream.radioparadise.com/rock-128",
                description = "Curated modern and classic rock mix with no commercials",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_rock_classic",
                name = "Rock Antenne Classic",
                genre = "Classic Rock",
                categoryId = "genre_rock",
                streamUrl = "https://s2-webradio.antenne.de/rock-antenne",
                description = "Legendary guitar anthems, stadium rock and metal hits",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_rp_main",
                name = "Radio Paradise Main",
                genre = "Eclectic Rock",
                categoryId = "genre_rock",
                streamUrl = "https://stream.radioparadise.com/mp3-128",
                description = "Unique blend of acoustic, indie rock and world rhythm",
                bitrate = "128 kbps"
            ),

            // ── Electronic & Dance ──
            RadioStation(
                id = "stream_defcon",
                name = "SomaFM DEF CON Radio",
                genre = "Electronic",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://ice2.somafm.com/defcon-128-mp3",
                description = "Music for hacking. The soundtrack for the DEF CON conference",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_dance_wave",
                name = "Dance Wave Hits",
                genre = "Dance / EDM",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://dancewave.online/dance.mp3",
                description = "Non-stop modern dance, house, and electronic club tracks",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_ibiza",
                name = "Ibiza Global Radio",
                genre = "House / Club",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://stream.ibizaglobalradio.com:8024/ibizaglobalradio.mp3",
                description = "Live underground deep house and electronic sets from Ibiza",
                bitrate = "128 kbps"
            ),

            // ── Ambient & Space ──
            RadioStation(
                id = "stream_groove_salad",
                name = "SomaFM Groove Salad",
                genre = "Ambient",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://ice2.somafm.com/groovesalad-128-mp3",
                description = "A nicely chilled plate of ambient/downtempo beats and grooves",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/SwyowKe-kDLokD2wEouPllJwjfKlG73podaFDHBzaEDeR0mu3mgp0x4iy7ouTDD0JuksjzG3ObZsfQ=w500-h500-l90-rj"
            ),
            RadioStation(
                id = "stream_deep_space",
                name = "SomaFM Deep Space One",
                genre = "Space Ambient",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://ice2.somafm.com/deepspaceone-128-mp3",
                description = "Deep ambient electronic and space music for exploration",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/SwyowKe-kDLokD2wEouPllJwjfKlG73podaFDHBzaEDeR0mu3mgp0x4iy7ouTDD0JuksjzG3ObZsfQ=w500-h500-l90-rj"
            ),
            RadioStation(
                id = "stream_drone_zone",
                name = "SomaFM Drone Zone",
                genre = "Drone Ambient",
                categoryId = "genre_dance_and_electronic",
                streamUrl = "https://ice2.somafm.com/dronezone-128-mp3",
                description = "Atmospheric, served best with headphones for highway drives",
                bitrate = "128 kbps",
                imageUrl = "https://yt3.googleusercontent.com/SwyowKe-kDLokD2wEouPllJwjfKlG73podaFDHBzaEDeR0mu3mgp0x4iy7ouTDD0JuksjzG3ObZsfQ=w500-h500-l90-rj"
            ),

            // ── Jazz & Soul ──
            RadioStation(
                id = "stream_swiss_jazz",
                name = "Radio Swiss Jazz",
                genre = "Jazz",
                categoryId = "genre_jazz",
                streamUrl = "http://stream.srg-ssr.ch/m/rsj/mp3_128",
                description = "Smooth jazz, swing, bebop, and soul classics 24/7",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_linn_jazz",
                name = "Linn Jazz UK",
                genre = "Audiophile Jazz",
                categoryId = "genre_jazz",
                streamUrl = "http://radio.linn.co.uk:8000/autodj",
                description = "Studio quality jazz masters and acoustic soul recordings",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_smooth_jazz",
                name = "Smooth Jazz Florida",
                genre = "Smooth Jazz",
                categoryId = "genre_jazz",
                streamUrl = "https://stream.zeno.fm/0545s0hmdg8uv",
                description = "Contemporary jazz, saxophone melodies, and relaxing grooves",
                bitrate = "128 kbps"
            ),

            // ── Classical & Focus ──
            RadioStation(
                id = "stream_swiss_classic",
                name = "Radio Swiss Classic",
                genre = "Classical",
                categoryId = "genre_classical",
                streamUrl = "http://stream.srg-ssr.ch/m/rsc_de/mp3_128",
                description = "Timeless classical music, symphonies, and chamber performances",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_classic_fm",
                name = "Classic FM UK",
                genre = "Classical",
                categoryId = "genre_classical",
                streamUrl = "https://media-ssl.musicradio.com/ClassicFMMP3",
                description = "The world's greatest music station with celebrated concertos",
                bitrate = "128 kbps"
            ),
            RadioStation(
                id = "stream_linn_classical",
                name = "Linn Classical",
                genre = "Symphonic",
                categoryId = "genre_classical",
                streamUrl = "http://radio.linn.co.uk:8004/autodj",
                description = "Uncompressed audiophile recordings of philharmonic orchestras",
                bitrate = "128 kbps"
            ),

            // ── News & Talk ──
            RadioStation(
                id = "stream_bbc_world",
                name = "BBC World Service",
                genre = "News",
                categoryId = "mood_commute",
                streamUrl = "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
                description = "International news, analysis and current affairs worldwide",
                bitrate = "96 kbps"
            ),
            RadioStation(
                id = "stream_npr_news",
                name = "NPR News Live",
                genre = "News / Talk",
                categoryId = "mood_commute",
                streamUrl = "https://npr-ice.streamguys1.com/live.mp3",
                description = "National Public Radio live reporting and documentary talk",
                bitrate = "96 kbps"
            )
        )
    }
}
