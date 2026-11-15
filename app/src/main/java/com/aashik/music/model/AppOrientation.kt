package com.aashik.music.model

enum class AppOrientation(val label: String, val summary: String) {
    AUTO("Auto", "Follows device rotation"),
    LANDSCAPE("Landscape", "Forces car, TV & landscape layout"),
    PORTRAIT("Portrait", "Forces phone portrait layout")
}
