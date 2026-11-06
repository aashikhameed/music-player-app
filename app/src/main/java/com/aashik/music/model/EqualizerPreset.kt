package com.aashik.music.model

data class EqualizerPresetDefinition(
    val id: Int,
    val name: String,
    val hpfMode: Int,
    val bandGains: List<Float>
)

object EqualizerPresetRegistry {
    const val PRESET_FLAT = 0
    const val PRESET_ACOUSTIC = 1
    const val PRESET_BASS_BOOSTER = 2
    const val PRESET_BASS_REDUCER = 3
    const val PRESET_CLASSICAL = 4
    const val PRESET_DANCE = 5
    const val PRESET_DEEP = 6
    const val PRESET_ELECTRONIC = 7
    const val PRESET_HIP_HOP = 8
    const val PRESET_JAZZ = 9
    const val PRESET_LATIN = 10
    const val PRESET_LOUDNESS = 11
    const val PRESET_LOUNGE = 12
    const val PRESET_PIANO = 13
    const val PRESET_POP = 14
    const val PRESET_R_AND_B = 15
    const val PRESET_ROCK = 16
    const val PRESET_SMALL_SPEAKERS = 17
    const val PRESET_SPOKEN_WORD = 18
    const val PRESET_TREBLE_BOOSTER = 19
    const val PRESET_TREBLE_REDUCER = 20
    const val PRESET_VOCAL_BOOSTER = 21
    const val PRESET_LATE_NIGHT = 22
    const val PRESET_CUSTOM = 23

    val PRESET_NAMES: List<String> = listOf(
        "Flat",             // 0
        "Acoustic",         // 1
        "Bass Booster",     // 2
        "Bass Reducer",     // 3
        "Classical",        // 4
        "Dance",            // 5
        "Deep",             // 6
        "Electronic",       // 7
        "Hip-Hop",          // 8
        "Jazz",             // 9
        "Latin",            // 10
        "Loudness",         // 11
        "Lounge",           // 12
        "Piano",            // 13
        "Pop",              // 14
        "R&B",              // 15
        "Rock",             // 16
        "Small Speakers",   // 17
        "Spoken Word",      // 18
        "Treble Booster",   // 19
        "Treble Reducer",   // 20
        "Vocal Booster",    // 21
        "Late Night",       // 22
        "Custom"            // 23
    )

    val PRESET_DEFINITIONS: List<EqualizerPresetDefinition> = listOf(
        EqualizerPresetDefinition(
            id = PRESET_FLAT,
            name = "Flat",
            hpfMode = 0,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_ACOUSTIC,
            name = "Acoustic",
            hpfMode = 2,
            bandGains = listOf(0f, 1f, -1f, -4f, -1f, 1.5f, 2f, 2.5f, 3f, 3.5f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_BASS_BOOSTER,
            name = "Bass Booster",
            hpfMode = 1,
            bandGains = listOf(6f, 6f, 4.5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_BASS_REDUCER,
            name = "Bass Reducer",
            hpfMode = 2,
            bandGains = listOf(-6f, -6f, -4.5f, -3f, -1f, 0f, 0f, 0f, 0f, 0f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_CLASSICAL,
            name = "Classical",
            hpfMode = 1,
            bandGains = listOf(4f, 3f, 2f, 0f, 0f, 0f, 0f, 2f, 3f, 3.5f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_DANCE,
            name = "Dance",
            hpfMode = 1,
            bandGains = listOf(5f, 5.5f, 3.5f, 0f, 1f, 2f, 3f, 4f, 4.5f, 3.5f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_DEEP,
            name = "Deep",
            hpfMode = 0,
            bandGains = listOf(5f, 4.5f, 3f, 1f, 0f, -1f, -2f, -2f, -3f, -4f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_ELECTRONIC,
            name = "Electronic",
            hpfMode = 1,
            bandGains = listOf(4.5f, 4f, 2f, 0f, -1.5f, 1.5f, 2.5f, 3.5f, 4.5f, 4f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_HIP_HOP,
            name = "Hip-Hop",
            hpfMode = 1,
            bandGains = listOf(6f, 5.5f, 3.5f, 1f, -1f, 1f, 1.5f, 2f, 2.5f, 2f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_JAZZ,
            name = "Jazz",
            hpfMode = 1,
            bandGains = listOf(3f, 2.5f, 1.5f, 1f, 1.5f, 2f, 1.5f, 2.5f, 3f, 2.5f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_LATIN,
            name = "Latin",
            hpfMode = 1,
            bandGains = listOf(3f, 2.5f, 0f, -1.5f, 0f, 2f, 2.5f, 3.5f, 3f, 2f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_LOUDNESS,
            name = "Loudness",
            hpfMode = 1,
            bandGains = listOf(5.5f, 4.5f, 2f, 0f, -1.5f, 0f, 1f, 2.5f, 4f, 4.5f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_LOUNGE,
            name = "Lounge",
            hpfMode = 1,
            bandGains = listOf(-2f, 1f, 3f, 2f, 0f, 1f, 1.5f, 2f, -1f, -2f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_PIANO,
            name = "Piano",
            hpfMode = 1,
            bandGains = listOf(2f, 2.5f, 2f, 1f, 2f, 3f, 3f, 2.5f, 2f, 2f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_POP,
            name = "Pop",
            hpfMode = 1,
            bandGains = listOf(-1f, 1.5f, 3f, 3.5f, 2f, 1f, 2f, 3f, 3.5f, 2.5f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_R_AND_B,
            name = "R&B",
            hpfMode = 1,
            bandGains = listOf(5f, 6f, 3f, -1f, 1f, 2f, 2.5f, 3f, 3.5f, 3f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_ROCK,
            name = "Rock",
            hpfMode = 1,
            bandGains = listOf(4f, 3.5f, 1.5f, -2f, -1f, 1f, 2f, 3f, 4f, 4f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_SMALL_SPEAKERS,
            name = "Small Speakers",
            hpfMode = 2,
            bandGains = listOf(-6f, -5f, -2f, 1f, 2.5f, 3f, 3.5f, 3f, 2f, 1f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_SPOKEN_WORD,
            name = "Spoken Word",
            hpfMode = 2,
            bandGains = listOf(-6f, -5f, -3f, -1f, 3f, 4f, 4f, 2f, 0f, -2f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_TREBLE_BOOSTER,
            name = "Treble Booster",
            hpfMode = 1,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f, 1f, 2.5f, 4f, 5.5f, 6f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_TREBLE_REDUCER,
            name = "Treble Reducer",
            hpfMode = 1,
            bandGains = listOf(0f, 0f, 0f, 0f, 0f, -1f, -2.5f, -4f, -5.5f, -6f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_VOCAL_BOOSTER,
            name = "Vocal Booster",
            hpfMode = 2,
            bandGains = listOf(-3f, -2f, -3f, -5f, 1f, 3f, 3.5f, 2f, 1f, 1f)
        ),
        EqualizerPresetDefinition(
            id = PRESET_LATE_NIGHT,
            name = "Late Night",
            hpfMode = 1,
            bandGains = listOf(3f, 2f, 1f, 0f, 1f, 1.5f, 1.5f, 1f, 0f, -1f)
        )
    )

    fun getPresetName(index: Int): String {
        return PRESET_NAMES.getOrElse(index) { "Custom" }
    }
}

