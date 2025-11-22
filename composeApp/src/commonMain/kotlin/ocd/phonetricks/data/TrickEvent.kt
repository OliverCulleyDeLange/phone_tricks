package ocd.phonetricks.data

data class TrickEvent(
    val type: TrickType,
    val timestamp: Long,
    val confidence: Float // 0.0 to 1.0
)

enum class TrickType {
    SPIN,  // Rotation about Z axis (flat on table)
    FLIP,  // Rotation about X or Y axis (like a spit roast)
    TAP_FRONT,  // Tap on the front face (screen)
    TAP_BACK,   // Tap on the back face
    TAP_TOP,    // Tap on the top edge
    TAP_BOTTOM, // Tap on the bottom edge
    TAP_LEFT,   // Tap on the left edge
    TAP_RIGHT   // Tap on the right edge
}

fun TrickType.isTap(): Boolean {
    return this == TrickType.TAP_FRONT ||
        this == TrickType.TAP_BACK ||
        this == TrickType.TAP_TOP ||
        this == TrickType.TAP_BOTTOM ||
        this == TrickType.TAP_LEFT ||
        this == TrickType.TAP_RIGHT
}

fun TrickType.getLabel(): String {
    return when (this) {
        TrickType.SPIN -> "Spin"
        TrickType.FLIP -> "Flip"
        TrickType.TAP_FRONT -> "Tap Front"
        TrickType.TAP_BACK -> "Tap Back"
        TrickType.TAP_TOP -> "Tap Top"
        TrickType.TAP_BOTTOM -> "Tap Bottom"
        TrickType.TAP_LEFT -> "Tap Left"
        TrickType.TAP_RIGHT -> "Tap Right"
    }
}

fun TrickType.getShortLabel(): String {
    return when (this) {
        TrickType.SPIN -> "S"
        TrickType.FLIP -> "F"
        TrickType.TAP_FRONT -> "Front"
        TrickType.TAP_BACK -> "Back"
        TrickType.TAP_TOP -> "Top"
        TrickType.TAP_BOTTOM -> "Bottom"
        TrickType.TAP_LEFT -> "Left"
        TrickType.TAP_RIGHT -> "Right"
    }
}
