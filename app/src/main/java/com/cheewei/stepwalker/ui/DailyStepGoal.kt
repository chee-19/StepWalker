package com.cheewei.stepwalker.ui

/** A default only: StepsScreen can receive a user-selected goal later. */
const val DEFAULT_DAILY_STEP_GOAL = 10_000L

data class DailyStepGoal(val steps: Long, val goal: Long = DEFAULT_DAILY_STEP_GOAL) {
    init {
        require(goal > 0) { "The daily step goal must be positive." }
    }

    // Keep the numerical percentage uncapped, but keep the indicator in its valid range.
    val percentage: Long get() = (steps.coerceAtLeast(0).toDouble() / goal * 100).toLong()
    val progress: Float get() = (steps.toDouble() / goal).coerceIn(0.0, 1.0).toFloat()
}
