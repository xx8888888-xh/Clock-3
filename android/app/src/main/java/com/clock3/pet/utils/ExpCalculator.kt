package com.clock3.pet.utils

object ExpCalculator {
    const val BASE_EXP_PER_MINUTE = 2
    const val COMPLETION_BONUS = 10
    const val EVENT_BONUS = 5
    const val INTERACT_BASE_EXP = 5
    const val INTERACT_LEVEL_MULTIPLIER = 2
    const val FEED_EXP = 10
    const val PLAY_EXP = 15
    const val STREAK_BONUS_PER_POMODORO = 2
    const val STREAK_CYCLE = 5
    const val EXP_PER_LEVEL = 100

    fun calculatePomodoroExp(
        focusSeconds: Int,
        completedFully: Boolean,
        hasEvent: Boolean,
        completedPomodoros: Int
    ): Int {
        var exp = focusSeconds * BASE_EXP_PER_MINUTE / 60
        if (completedFully) {
            exp += COMPLETION_BONUS
        }
        if (hasEvent) {
            exp += EVENT_BONUS
        }
        val streakBonus = minOf(completedPomodoros % STREAK_CYCLE, STREAK_CYCLE - 1) * STREAK_BONUS_PER_POMODORO
        exp += streakBonus
        return exp
    }

    fun calculateInteractExp(level: Int): Int {
        return INTERACT_BASE_EXP + (level * INTERACT_LEVEL_MULTIPLIER)
    }

    fun calculateFeedExp(): Int = FEED_EXP

    fun calculatePlayExp(): Int = PLAY_EXP
}
