package ocd.phonetricks.data

data class TrickEvent(
    val type: TrickType,
    val timestamp: Long,
    val confidence: Float // 0.0 to 1.0
)

enum class TrickType {
    SPIN,  // Rotation about Z axis (flat on table)
    FLIP   // Rotation about X or Y axis (like a spit roast)
}
