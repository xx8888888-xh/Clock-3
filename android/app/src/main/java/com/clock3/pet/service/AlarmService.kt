package com.clock3.pet.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.receiver.AlarmReceiver
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class AlarmService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val repository = Clock3Repository(appContext)
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationService: NotificationService = NotificationService.getInstance(appContext)
    @Volatile
    private var isRunning = false
    private var checkJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val alarmCallbacks = CopyOnWriteArrayList<(Alarm) -> Unit>()
    private val activeAlarmRef = AtomicReference<Alarm>(null)
    private val lastTriggeredAlarmIds = Collections.synchronizedSet(mutableSetOf<Long>())

    @Volatile
    private var cachedMaxSnoozeCount = DEFAULT_MAX_SNOOZE_COUNT
    @Volatile
    private var cachedSnoozeDuration = DEFAULT_SNOOZE_DURATION
    @Volatile
    private var cachedVibrationEnabled = true
    @Volatile
    private var cachedSoundEnabled = true
    @Volatile
    private var cachedSleepModeEnabled = true
    @Volatile
    private var cachedSleepStartHour = DEFAULT_SLEEP_START_HOUR
    @Volatile
    private var cachedSleepEndHour = DEFAULT_SLEEP_END_HOUR
    private var configRefreshJob: Job? = null

    fun isRunning(): Boolean = isRunning

    fun startChecking() {
        if (isRunning) return
        isRunning = true
        lastTriggeredAlarmIds.clear()
        refreshConfig()
        configRefreshJob = scope.launch {
            while (isActive) {
                delay(CONFIG_REFRESH_INTERVAL_MS)
                refreshConfig()
            }
        }
        checkJob = scope.launch {
            while (isActive) {
                checkAlarms()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stopChecking() {
        isRunning = false
        checkJob?.cancel()
        configRefreshJob?.cancel()
    }

    private fun refreshConfig() {
        cachedMaxSnoozeCount = (repository.getConfig("max_snooze_count", DEFAULT_MAX_SNOOZE_COUNT) as? Int) ?: DEFAULT_MAX_SNOOZE_COUNT
        cachedSnoozeDuration = (repository.getConfig("snooze_duration", DEFAULT_SNOOZE_DURATION) as? Int) ?: DEFAULT_SNOOZE_DURATION
        cachedVibrationEnabled = (repository.getConfig("vibration_enabled", true) as? Boolean) ?: true
        cachedSoundEnabled = (repository.getConfig("sound_enabled", true) as? Boolean) ?: true
        cachedSleepModeEnabled = (repository.getConfig("sleep_mode_enabled", true) as? Boolean) ?: true
        cachedSleepStartHour = (repository.getConfig("sleep_start_hour", DEFAULT_SLEEP_START_HOUR) as? Int) ?: DEFAULT_SLEEP_START_HOUR
        cachedSleepEndHour = (repository.getConfig("sleep_end_hour", DEFAULT_SLEEP_END_HOUR) as? Int) ?: DEFAULT_SLEEP_END_HOUR
    }

    private suspend fun checkAlarms() {
        val alarms = repository.getEnabledAlarms()
        val now = LocalDateTime.now()

        for (alarm in alarms) {
            if (alarm.id in lastTriggeredAlarmIds) continue

            val maxSnoozeCount = cachedMaxSnoozeCount

            if (alarm.snoozeTime != null) {
                try {
                    val snoozeTime = LocalDateTime.parse(alarm.snoozeTime)
                    if ((now.isAfter(snoozeTime) || now.isEqual(snoozeTime)) && alarm.snoozeCount < maxSnoozeCount) {
                        lastTriggeredAlarmIds.add(alarm.id)
                        triggerAlarm(alarm)
                    }
                } catch (e: Exception) {
                    AppLog.e(TAG, "Failed to parse snooze time", e)
                }
                continue
            }

            val nextTime = alarm.getNextTriggerTime() ?: continue
            val diff = Duration.between(now, nextTime).seconds

            if (diff in 0..TRIGGER_WINDOW_SECONDS) {
                lastTriggeredAlarmIds.add(alarm.id)
                triggerAlarm(alarm)
            }
        }
    }

    private fun triggerAlarm(alarm: Alarm) {
        activeAlarmRef.set(alarm)

        scope.launch {
            alarmCallbacks.forEach { callback ->
                try {
                    callback(alarm)
                } catch (e: Exception) {
                    AppLog.e(TAG, "Alarm callback error", e)
                }
            }
        }
    }

    fun scheduleAlarm(alarm: Alarm) {
        cancelAlarmInternal(alarm.id)

        if (!alarm.enabled) return

        val intent = Intent(appContext, AlarmReceiver::class.java).apply {
            action = ALARM_TRIGGER_ACTION
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_content", alarm.content)
            putExtra("alarm_time", alarm.time)
            putExtra("alarm_repeat_type", alarm.repeatType.value)
            putExtra("alarm_enabled", alarm.enabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarm.getNextTriggerTime()?.let { triggerTime ->
            val triggerAt = java.time.ZonedDateTime.of(triggerTime, java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } else {
                    AppLog.w(TAG, "Exact alarms not available, using inexact alarm for id=${alarm.id}")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                AppLog.w(TAG, "SecurityException scheduling alarm id=${alarm.id}, falling back to inexact", e)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        cancelAlarmInternal(alarm.id)
    }

    private fun cancelAlarmInternal(alarmId: Long) {
        val intent = Intent(appContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun snoozeAlarm(minutes: Int) {
        val currentActiveAlarm = activeAlarmRef.getAndSet(null) ?: return
        scope.launch {
            val currentAlarm = repository.getAlarmById(currentActiveAlarm.id) ?: return@launch
            val maxSnoozeCount = cachedMaxSnoozeCount

            if (currentAlarm.snoozeCount >= maxSnoozeCount) {
                notificationService.stopSound()
                notificationService.cancelAlarmNotification(currentAlarm.id)
                dismissAlarmInternal(currentActiveAlarm)
                return@launch
            }

            val snoozeMinutes = if (minutes > 0) minutes else cachedSnoozeDuration

            repository.snoozeAlarm(currentAlarm.id, snoozeMinutes)

            val updatedAlarm = repository.getAlarmById(currentAlarm.id)
            updatedAlarm?.let {
                scheduleAlarm(it)
            }
        }
    }

    fun dismissAlarm() {
        val currentActiveAlarm = activeAlarmRef.getAndSet(null) ?: return
        notificationService.stopSound()
        notificationService.cancelAlarmNotification(currentActiveAlarm.id)
        scope.launch {
            dismissAlarmInternal(currentActiveAlarm)
        }
    }

    private suspend fun dismissAlarmInternal(alarm: Alarm) {
        if (alarm.repeatType == Alarm.RepeatType.ONCE) {
            repository.toggleAlarm(alarm.id, false)
        }
        repository.clearAlarmSnooze(alarm.id)

        val updatedAlarm = repository.getAlarmById(alarm.id)
        updatedAlarm?.let {
            if (it.enabled) {
                scheduleAlarm(it)
            }
        }
    }

    suspend fun getNextAlarm(): Alarm? {
        var nextAlarm: Alarm? = null
        var minDiff = Long.MAX_VALUE
        val maxDiffSeconds = 7 * 24 * 60 * 60L

        val alarms = repository.getEnabledAlarms()
        val now = LocalDateTime.now()

        for (alarm in alarms) {
            val nextTime = alarm.getNextTriggerTime() ?: continue
            val diff = Duration.between(now, nextTime).seconds

            if (diff in 0..maxDiffSeconds && diff < minDiff) {
                minDiff = diff
                nextAlarm = alarm
            }
        }

        return nextAlarm
    }

    fun onAlarmTrigger(callback: (Alarm) -> Unit) {
        alarmCallbacks.add(callback)
    }

    fun isVibrationEnabled(): Boolean = cachedVibrationEnabled

    fun isSoundEnabled(): Boolean = cachedSoundEnabled

    fun isSleepModeEnabled(): Boolean {
        if (!cachedSleepModeEnabled) return false
        if (cachedSleepStartHour == cachedSleepEndHour) return false
        val currentHour = LocalDateTime.now().hour
        return currentHour >= cachedSleepStartHour || currentHour < cachedSleepEndHour
    }

    fun release() {
        stopChecking()
        scope.cancel()
        alarmCallbacks.clear()
        activeAlarmRef.set(null)
        lastTriggeredAlarmIds.clear()
    }

    companion object {
        private const val TAG = "AlarmService"
        const val ALARM_TRIGGER_ACTION = "com.clock3.pet.ALARM_TRIGGER"
        const val CHECK_INTERVAL_MS = 1000L
        const val CONFIG_REFRESH_INTERVAL_MS = 30000L
        const val TRIGGER_WINDOW_SECONDS = 5
        const val DEFAULT_MAX_SNOOZE_COUNT = 3
        const val DEFAULT_SNOOZE_DURATION = 5
        const val DEFAULT_SLEEP_START_HOUR = 22
        const val DEFAULT_SLEEP_END_HOUR = 7

        @Volatile
        private var INSTANCE: AlarmService? = null

        fun getInstance(context: Context): AlarmService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmService(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun releaseInstance() {
            INSTANCE?.release()
            INSTANCE = null
        }
    }
}
