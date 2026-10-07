package com.cheewei.stepwalker.data

import java.time.ZonedDateTime

enum class GhostStatus { AHEAD, BEHIND, TIED }

data class GhostComparison(val todaySteps: Long, val yesterdaySameTimeSteps: Long) {
    val difference: Long get() = todaySteps - yesterdaySameTimeSteps
    val status: GhostStatus get() = when {
        difference > 0 -> GhostStatus.AHEAD
        difference < 0 -> GhostStatus.BEHIND
        else -> GhostStatus.TIED
    }
}

/** Calendar days, not a fixed 24-hour subtraction, preserve local time across DST. */
internal data class GhostTimeRanges(val now: ZonedDateTime) {
    val todayStart: ZonedDateTime = now.toLocalDate().atStartOfDay(now.zone)
    val yesterdayStart: ZonedDateTime = now.toLocalDate().minusDays(1).atStartOfDay(now.zone)
    // Java resolves nonexistent local times forward and prefers today's offset in overlaps.
    val yesterdayEnd: ZonedDateTime = now.minusDays(1)
}

data class StepsSnapshot(val history: List<DailySteps>, val ghost: GhostComparison)
