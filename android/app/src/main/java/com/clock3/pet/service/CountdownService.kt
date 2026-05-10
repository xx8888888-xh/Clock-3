package com.clock3.pet.service

import android.content.Context
import com.clock3.pet.data.model.Countdown
import com.clock3.pet.data.repository.Clock3Repository
import kotlinx.coroutines.*
import java.time.Duration
import java.time.LocalDateTime

class CountdownService(context: Context) {
    private val repository = Clock3Repository(context)
    private var isRunning = false
    private var checkJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var countdownCallbacks = mutableListOf<(Countdown) -> Unit>()

    fun startChecking() {
        if (isRunning) return
        isRunning = true
        checkJob = scope.launch {
            while (isActive) {
                checkCountdowns()
                delay(100)
            }
        }
    }

    fun stopChecking() {
        isRunning = false
        checkJob?.cancel()
    }

    private suspend fun checkCountdowns() {
        val countdowns = repository.getAllAlarmsSync()
            .map { alarm -> Countdown.create(alarm.label, 0) }

        for (countdown in countdowns) {
            if (countdown.status == Countdown.CountdownStatus.RUNNING) {
                val remaining = countdown.updateRemaining()
                if (remaining <= 0) {
                    onCountdownComplete(countdown)
                }
            }
        }
    }

    private fun onCountdownComplete(countdown: Countdown) {
        scope.launch {
            countdownCallbacks.forEach { callback ->
                try {
                    callback(countdown)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun createCountdown(label: String, seconds: Int): Countdown {
        val countdown = Countdown.create(label, seconds)
        val id = repository.addCountdown(countdown)
        return countdown.copy(id = id)
    }

    suspend fun pauseCountdown(countdownId: Long) {
        repository.pauseCountdown(countdownId)
    }

    suspend fun resumeCountdown(countdownId: Long) {
        repository.resumeCountdown(countdownId)
    }

    suspend fun deleteCountdown(countdownId: Long) {
        repository.deleteCountdown(countdownId)
    }

    fun onCountdownComplete(callback: (Countdown) -> Unit) {
        countdownCallbacks.add(callback)
    }

    companion object {
        @Volatile
        private var INSTANCE: CountdownService? = null

        fun getInstance(context: Context): CountdownService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CountdownService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
