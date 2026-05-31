package com.clock3.pet

import com.clock3.pet.utils.ExpCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpCalculatorTest {

    @Test
    fun calculatePomodoroExp_basicFocus() {
        val exp = ExpCalculator.calculatePomodoroExp(
            focusSeconds = 1500,
            completedFully = true,
            hasEvent = true,
            completedPomodoros = 0
        )
        assertEquals(1500 / 60 * 2 + 10 + 5, exp)
    }

    @Test
    fun calculatePomodoroExp_noEvent() {
        val exp = ExpCalculator.calculatePomodoroExp(
            focusSeconds = 1500,
            completedFully = true,
            hasEvent = false,
            completedPomodoros = 0
        )
        assertEquals(1500 / 60 * 2 + 10, exp)
    }

    @Test
    fun calculatePomodoroExp_notCompleted() {
        val exp = ExpCalculator.calculatePomodoroExp(
            focusSeconds = 1500,
            completedFully = false,
            hasEvent = false,
            completedPomodoros = 0
        )
        assertEquals(1500 / 60 * 2, exp)
    }

    @Test
    fun calculatePomodoroExp_withStreak() {
        val exp = ExpCalculator.calculatePomodoroExp(
            focusSeconds = 1500,
            completedFully = true,
            hasEvent = true,
            completedPomodoros = 3
        )
        assertEquals(1500 / 60 * 2 + 10 + 5 + (3 % 5) * 2, exp)
    }

    @Test
    fun calculatePomodoroExp_streakResetsAtCycle() {
        val exp = ExpCalculator.calculatePomodoroExp(
            focusSeconds = 1500,
            completedFully = true,
            hasEvent = true,
            completedPomodoros = 5
        )
        assertEquals(1500 / 60 * 2 + 10 + 5 + (5 % 5) * 2, exp)
    }

    @Test
    fun calculateInteractExp_level1() {
        assertEquals(5 + 1 * 2, ExpCalculator.calculateInteractExp(1))
    }

    @Test
    fun calculateInteractExp_level10() {
        assertEquals(5 + 10 * 2, ExpCalculator.calculateInteractExp(10))
    }

    @Test
    fun calculateFeedExp() {
        assertEquals(10, ExpCalculator.calculateFeedExp())
    }

    @Test
    fun calculatePlayExp() {
        assertEquals(15, ExpCalculator.calculatePlayExp())
    }
}
