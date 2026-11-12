package com.aashik.music.model

/**
 * Which side of the car the driver sits on. The headunit UI puts navigation and the primary
 * controls on the driver's side (nearest the driver's hand) and the artwork / content on the far side.
 */
enum class DriveSide(val label: String, val summary: String) {
    LEFT("Left-hand drive", "Driver sits on the left. Navigation and controls on the left."),
    RIGHT("Right-hand drive", "Driver sits on the right. Navigation and controls on the right.")
}
