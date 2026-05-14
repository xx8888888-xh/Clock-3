package com.clock3.pet.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.receiver.AlarmReceiver
import kotlinx.coroutines.*
import java.time.Duration
import java.time.LocalDateTime

class AlarmService(private val context: Context) {
    private val repository = Clock3Repository(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private var isRunning = false
    private var checkJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var alarmCallbacks = mutableListOf<(Alarm) -> Unit>()
    private var activeAlarm: Alarm? = null

    fun startChecking() {
        if (isRunning) return
        isRunning = true
        checkJob = scope.launch {
            while (isActive) {
                checkAlarms()
                delay(1000)
            }
        }
    }

    fun stopChecking() {
        isRunning = false
        checkJob?.cancel()
    }

    private suspend fun checkAlarms() {
        val alarms = repository.getEnabledAlarms()
        val now = LocalDateTime.now()

        for (alarm in alarms) {
            alarm.snoozeTime?.let { snoozeTimeStr ->
                try {
                    val snoozeTime = LocalDateTime.parse(snoozeTimeStr)
                    if (now.isAfter(snoozeTime) || now.isEqual(snoozeTime)) {
                        triggerAlarm(alarm)
                        return
                    }
                } catch (e: Exception) {
                }
                continue
            }

            val nextTime = alarm.getNextTriggerTime() ?: continue
            val diff = Duration.between(now, nextTime).seconds

            if (diff in 0..1) {
                triggerAlarm(alarm)
                return
            }
        }
    }

    private fun triggerAlarm(alarm: Alarm) {
        activeAlarm = alarm
        scheduleAlarm(alarm)

        scope.launch {
            alarmCallbacks.forEach { callback ->
                try {
                    callback(alarm)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun scheduleAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.clock3.pet.ALARM_TRIGGER"
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_content", alarm.content)
            putExtra("alarm_time", alarm.time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarm.getNextTriggerTime()?.let { triggerTime ->
            val triggerAtMillis = java.time.ZoneId.systemDefault()
                .rules.getOffset(java.time.Instant.now())
                .getOffset(java.time.Instant.now())
            val triggerAt = java.time.ZonedDateTime.of(triggerTime, java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            try {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun snoozeAlarm(minutes: Int) {
        activeAlarm?.let { alarm ->
            scope.launch {
                val snoozeMinutes = repository.getConfig("snooze_duration", 5) as Int
                repository.snoozeAlarm(alarm.id, snoozeMinutes)
                activeAlarm = null
            }
        }
    }

    fun dismissAlarm() {
        activeAlarm?.let { alarm ->
            scope.launch {
                if (alarm.repeatType == Alarm.RepeatType.ONCE) {
                    repository.toggleAlarm(alarm.id, false)
                } else {
                    repository.clearAlarmSnooze(alarm.id)
                }
                activeAlarm = null
            }
        }
    }

    fun getNextAlarm(): Alarm? {
        var nextAlarm: Alarm? = null
        var minDiff = Long.MAX_VALUE

        runBlocking {
            val alarms = repository.getEnabledAlarms()
            val now = LocalDateTime.now()

            for (alarm in alarms) {
                val nextTime = alarm.getNextTriggerTime() ?: continue
                val diff = Duration.between(now, nextTime).seconds

                if (diff in 0 until minDiff) {
                    minDiff = diff
                    nextAlarm = alarm
                }
            }
        }

        return nextAlarm
    }

    fun onAlarmTrigger(callback: (Alarm) -> Unit) {
        alarmCallbacks.add(callback)
    }

    companion object {
        @Volatile
        private var INSTANCE: AlarmService? = null

        fun getInstance(context: Context): AlarmService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
