package com.cheewei.stepwalker.ui

const val TRACK_DP_PER_1000_STEPS = 150f
const val TRACK_MARKER_INTERVAL_STEPS = 1000L

/** Absolute dp coordinates measured from the zero-step marker, independent of viewport. */
data class RacePositions(val you: Float, val ghost: Float, val endSteps: Long) {
    val trackLengthDp: Float get() = stepPosition(endSteps)
}

fun stepPosition(steps: Long): Float =
    (steps.coerceAtLeast(0).toDouble() / TRACK_MARKER_INTERVAL_STEPS * TRACK_DP_PER_1000_STEPS).toFloat()

fun racePositions(todaySteps: Long, yesterdaySteps: Long): RacePositions {
    val today = todaySteps.coerceAtLeast(0)
    val yesterday = yesterdaySteps.coerceAtLeast(0)
    val leader = maxOf(today, yesterday)
    val end = ((leader / TRACK_MARKER_INTERVAL_STEPS) + 1) * TRACK_MARKER_INTERVAL_STEPS
    return RacePositions(stepPosition(today), stepPosition(yesterday), end)
}
