package com.clock3.pet.service

import android.content.Context
import com.clock3.pet.data.model.Countdown
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class CountdownService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val repository = Clock3Repository(appContext)
    @Volatile
    private var isRunning = false
    private var checkJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val countdownCallbacks = CopyOnWriteArrayList<(Countdown) -> Unit>()
    private val activeCountdowns = Collections.synchronizedMap(mutableMapOf<Long, Countdown>())
    private var dbWriteCounter = 0

    private val notificationService: NotificationService = NotificationService.getInstance(appContext)

    fun isRunning(): Boolean = isRunning

    fun startChecking() {
        if (isRunning) return
        isRunning = true
        dbWriteCounter = 0
        checkJob = scope.launch {
            while (isActive) {
                checkCountdowns()
                dbWriteCounter++
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stopChecking() {
        isRunning = false
        checkJob?.cancel()
    }

    private suspend fun checkCountdowns() {
        val writeToDb = dbWriteCounter >= DB_WRITE_INTERVAL
        if (writeToDb) dbWriteCounter = 0
        val countdowns = repository.getCountdownsByStatus("running")

        for (countdown in countdowns) {
            try {
                if (countdown.status == Countdown.CountdownStatus.RUNNING) {
                    countdown.updateRemaining()

                    synchronized(activeCountdowns) {
                        activeCountdowns[countdown.id] = countdown
                    }

                    if (countdown.remainingSeconds <= 0) {
                        val completedCountdown = countdown.copy(
                            status = Countdown.CountdownStatus.COMPLETED,
                            remainingSeconds = 0
                        )
                        repository.updateCountdown(completedCountdown)
                        synchronized(activeCountdowns) {
                            activeCountdowns.remove(countdown.id)
                        }
                        notificationService.showCountdownNotification(completedCountdown)
                        countdownCallbacks.forEach { callback ->
                            try {
                                callback(completedCountdown)
                            } catch (e: Exception) {
                                AppLog.e(TAG, "Countdown callback error", e)
                            }
                        }
                    } else if (writeToDb) {
                        repository.updateCountdown(countdown)
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error checking countdown ${countdown.id}", e)
            }
        }
    }

    suspend fun createCountdown(label: String, seconds: Int): Countdown {
        val countdown = Countdown.create(label, seconds)
        val id = repository.addCountdown(countdown)
        val savedCountdown = countdown.copy(id = id)
        synchronized(activeCountdowns) {
            activeCountdowns[id] = savedCountdown
        }
        return savedCountdown
    }

    suspend fun pauseCountdown(countdownId: Long) {
        val countdown = repository.getCountdownById(countdownId)
        countdown?.let {
            val paused = it.pause()
            repository.updateCountdown(paused)
            synchronized(activeCountdowns) {
                activeCountdowns[countdownId] = paused
            }
        }
    }

    suspend fun resumeCountdown(countdownId: Long) {
        val countdown = repository.getCountdownById(countdownId)
        countdown?.let {
            val resumed = it.resume()
            repository.updateCountdown(resumed)
            synchronized(activeCountdowns) {
                activeCountdowns[countdownId] = resumed
            }
        }
    }

    suspend fun deleteCountdown(countdownId: Long) {
        repository.deleteCountdown(countdownId)
        synchronized(activeCountdowns) {
            activeCountdowns.remove(countdownId)
        }
    }

    suspend fun completeCountdown(countdownId: Long) {
        val countdown = repository.getCountdownById(countdownId)
        countdown?.let {
            val completed = it.copy(
                status = Countdown.CountdownStatus.COMPLETED,
                remainingSeconds = 0
            )
            repository.updateCountdown(completed)
            synchronized(activeCountdowns) {
                activeCountdowns.remove(countdownId)
            }
        }
    }

    fun getActiveCountdown(id: Long): Countdown? {
        return synchronized(activeCountdowns) {
            activeCountdowns[id]
        }
    }

    fun onCountdownComplete(callback: (Countdown) -> Unit) {
        countdownCallbacks.add(callback)
    }

    fun release() {
        stopChecking()
        scope.cancel()
        countdownCallbacks.clear()
        synchronized(activeCountdowns) {
            activeCountdowns.clear()
        }
    }

    companion object {
        private const val TAG = "CountdownService"
        const val CHECK_INTERVAL_MS = 1000L
        private const val DB_WRITE_INTERVAL = 10

        @Volatile
        private var INSTANCE: CountdownService? = null

        fun getInstance(context: Context): CountdownService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CountdownService(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun releaseInstance() {
            INSTANCE?.release()
            INSTANCE = null
        }
    }
}
