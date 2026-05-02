package ocd.phonetricks.data

/**
 * Project a raw surface value onto the [0, 1] range defined by the parameter's
 * [inputMin]/[inputMax]. If the input range is empty (min == max) the result
 * is 0.5 — neither extreme is meaningful so we centre the parameter.
 */
fun normalizeSurfaceValue(raw: Float, inputMin: Float, inputMax: Float): Float {
    val range = inputMax - inputMin
    if (range == 0f) return 0.5f
    return ((raw - inputMin) / range).coerceIn(0f, 1f)
}

/**
 * Compute the synth amplitude from a list of (normalized_t, Volume) pairs.
 *
 * Each mapping is scaled to its own [Volume.min], [Volume.max] range, then
 * the per-mapping outputs are averaged. The previous implementation took the
 * range from the first mapping only and averaged raw normalized values across
 * all of them, so additional mappings silently lost their configured range.
 *
 * Returns null when [mappings] is empty so the caller can fall back to a
 * default amplitude.
 */
fun computeVolumeAmplitude(mappings: List<Pair<Float, ControlParameter.Volume>>): Float? {
    if (mappings.isEmpty()) return null
    var sum = 0.0
    for ((t, p) in mappings) {
        val clamped = t.coerceIn(0f, 1f)
        sum += (p.min + clamped * (p.max - p.min)).toDouble()
    }
    return (sum / mappings.size).toFloat()
}
