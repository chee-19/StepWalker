package com.cheewei.stepwalker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RacePositionsTest {
    @Test fun coordinatesUseFixedScale() {
        assertEquals(0f, stepPosition(0), .001f)
        assertEquals(150f, stepPosition(1000), .001f)
        assertEquals(300f, stepPosition(2000), .001f)
        assertEquals(750f, stepPosition(5000), .001f)
        assertEquals(1500f, stepPosition(10000), .001f)
        val close = racePositions(4250, 3810)
        assertEquals(637.5f, close.you, .001f)
        assertEquals(571.5f, close.ghost, .001f)
    }

    @Test fun gapsAreProportionalAndIndependentOfLeader() {
        val small = racePositions(2000, 2500)
        val large = racePositions(2000, 7000)
        assertEquals(small.you, large.you, .001f)
        assertEquals(75f, small.ghost - small.you, .001f)
        assertEquals(750f, large.ghost - large.you, .001f)
    }

    @Test fun trackExtendsBeyondLeaderAndTiesKeepExactCoordinate() {
        for ((today, ghost, end) in listOf(Triple(0L, 0L, 1000L), Triple(4250L, 3810L, 5000L),
            Triple(2100L, 7800L, 8000L), Triple(4000L, 4000L, 5000L))) {
            val positions = racePositions(today, ghost)
            assertEquals(end, positions.endSteps)
            assertTrue(positions.trackLengthDp > maxOf(positions.you, positions.ghost))
        }
        val tied = racePositions(4000, 4000)
        assertEquals(tied.you, tied.ghost, 0f)
        assertEquals(0f, stepPosition(-1), 0f)
    }
}
