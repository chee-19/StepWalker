package com.cheewei.stepwalker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class GhostComparisonTest {
    @Test fun comparisonHandlesAheadBehindAndTied() {
        val ahead = GhostComparison(4250, 3810)
        assertEquals(440L, ahead.difference)
        assertEquals(GhostStatus.AHEAD, ahead.status)
        val behind = GhostComparison(3000, 3850)
        assertEquals(-850L, behind.difference)
        assertEquals(GhostStatus.BEHIND, behind.status)
        assertEquals(GhostStatus.TIED, GhostComparison(0, 0).status)
        assertEquals(GhostStatus.TIED, GhostComparison(4250, 4250).status)
    }

    @Test fun singaporeUsesYesterdayPartialDayAcrossYearBoundary() {
        val now = ZonedDateTime.of(2026, 1, 1, 13, 18, 0, 0, ZoneId.of("Asia/Singapore"))
        val ranges = GhostTimeRanges(now)
        assertEquals("2026-01-01T00:00+08:00[Asia/Singapore]", ranges.todayStart.toString())
        assertEquals("2025-12-31T00:00+08:00[Asia/Singapore]", ranges.yesterdayStart.toString())
        assertEquals(now.toLocalTime(), ranges.yesterdayEnd.toLocalTime())
        assertEquals(798L, Duration.between(ranges.yesterdayStart, ranges.yesterdayEnd).toMinutes())
    }

    @Test fun daylightSavingUsesSameWallTimeInsteadOf24HoursAgo() {
        for (date in listOf("2026-03-08T13:18:00", "2026-11-01T13:18:00")) {
            val now = java.time.LocalDateTime.parse(date).atZone(ZoneId.of("America/New_York"))
            val ranges = GhostTimeRanges(now)
            assertEquals(now.toLocalTime(), ranges.yesterdayEnd.toLocalTime())
            assertEquals(now.toLocalDate().minusDays(1), ranges.yesterdayEnd.toLocalDate())
            assertEquals(798L, Duration.between(ranges.yesterdayStart, ranges.yesterdayEnd).toMinutes())
        }
    }

    @Test fun nonexistentYesterdayTimeMovesForwardAndOverlapPrefersCurrentOffset() {
        val zone = ZoneId.of("America/New_York")
        val spring = GhostTimeRanges(ZonedDateTime.of(2026, 3, 9, 2, 30, 0, 0, zone))
        assertEquals(3, spring.yesterdayEnd.hour)
        assertEquals(30, spring.yesterdayEnd.minute)
        val fall = GhostTimeRanges(ZonedDateTime.of(2026, 11, 2, 1, 30, 0, 0, zone))
        assertEquals(fall.now.offset, fall.yesterdayEnd.offset)
    }

    @Test fun midnightHasEmptyRanges() {
        val ranges = GhostTimeRanges(ZonedDateTime.of(2026, 10, 7, 0, 0, 0, 0, ZoneId.of("Asia/Singapore")))
        assertEquals(ranges.todayStart, ranges.now)
        assertEquals(ranges.yesterdayStart, ranges.yesterdayEnd)
    }
}
