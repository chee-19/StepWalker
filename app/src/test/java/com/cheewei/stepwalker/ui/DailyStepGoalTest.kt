package com.cheewei.stepwalker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStepGoalTest {
    @Test fun emptyDay() {
        val goal = DailyStepGoal(0)
        assertEquals(0L, goal.percentage)
        assertEquals(0f, goal.progress, 0f)
    }

    @Test fun partialGoal() {
        val goal = DailyStepGoal(3_420)
        assertEquals(34L, goal.percentage)
        assertEquals(0.342f, goal.progress, 0.0001f)
    }

    @Test fun exceededGoal() {
        val goal = DailyStepGoal(12_500)
        assertEquals(125L, goal.percentage)
        assertEquals(1f, goal.progress, 0f)
    }

    @Test fun customGoal() {
        val goal = DailyStepGoal(3_000, 6_000)
        assertEquals(50L, goal.percentage)
        assertEquals(0.5f, goal.progress, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroGoal() {
        DailyStepGoal(100, 0)
    }
}
