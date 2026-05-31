package com.clock3.pet.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clock3.pet.R
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.NotificationService
import com.clock3.pet.ui.AlarmActivity
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmService.ALARM_TRIGGER_ACTION) return

        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) return

        val alarmLabel = intent.getStringExtra("alarm_label") ?: context.getString(R.string.alarm_default_label)
        val alarmContent = intent.getStringExtra("alarm_content") ?: ""
        val alarmTime = intent.getStringExtra("alarm_time") ?: ""
        val alarmRepeatType = intent.getStringExtra("alarm_repeat_type") ?: "once"
        val alarmEnabled = intent.getBooleanExtra("alarm_enabled", true)

        val notificationService = NotificationService.getInstance(context)
        val alarmService = AlarmService.getInstance(context)

        val fallbackAlarm = com.clock3.pet.data.model.Alarm(
            id = alarmId,
            label = alarmLabel,
            content = alarmContent,
            time = alarmTime,
            repeatType = com.clock3.pet.data.model.Alarm.RepeatType.fromValue(alarmRepeatType),
            enabled = alarmEnabled
        )

        var alarm = fallbackAlarm
        val pendingResult = goAsync()
        val supervisorJob = SupervisorJob()
        CoroutineScope(supervisorJob + Dispatchers.IO).launch {
            try {
                withTimeout(9000) {
                    val dbAlarm = Clock3Repository(context.applicationContext).getAlarmById(alarmId)
                    if (dbAlarm != null) {
                        alarm = dbAlarm
                    } else {
                        AppLog.w(TAG, "Alarm id=$alarmId not found in DB, using Intent extras as fallback")
                    }
                }
            } catch (e: Exception) {
                AppLog.w(TAG, "Failed to load alarm from DB, using Intent extras", e)
            } finally {
                supervisorJob.cancel()
                handleAlarm(context, alarm, notificationService, alarmService)
                pendingResult.finish()
            }
        }
    }

    private fun handleAlarm(
        context: Context,
        alarm: com.clock3.pet.data.model.Alarm,
        notificationService: NotificationService,
        alarmService: AlarmService
    ) {
        if (alarmService.isSleepModeEnabled()) {
            notificationService.showAlarmNotification(alarm, fullScreenPendingIntent = null, silent = true)
            return
        }

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
            putExtra("alarm_content", alarm.content)
            putExtra("alarm_time", alarm.time)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt() + FULL_SCREEN_PENDING_OFFSET,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationService.showAlarmNotification(alarm, fullScreenPendingIntent)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "alarm"
        const val FULL_SCREEN_PENDING_OFFSET = 500000
    }
}
