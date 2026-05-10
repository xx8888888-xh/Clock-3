package com.clock3.pet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.NotificationService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.clock3.pet.ALARM_TRIGGER") {
            val alarmId = intent.getLongExtra("alarm_id", -1)
            val alarmLabel = intent.getStringExtra("alarm_label") ?: "闹钟"
            val alarmContent = intent.getStringExtra("alarm_content") ?: ""
            val alarmTime = intent.getStringExtra("alarm_time") ?: ""

            val repository = Clock3Repository(context)
            val notificationService = NotificationService.getInstance(context)

            val alarm = com.clock3.pet.data.model.Alarm(
                id = alarmId,
                label = alarmLabel,
                content = alarmContent,
                time = alarmTime
            )

            notificationService.showAlarmNotification(alarm)

            if (repository.getConfig("vibration_enabled", true) as Boolean) {
                vibrate(context)
            }

            if (repository.getConfig("sound_enabled", true) as Boolean) {
                playSound(context)
            }

            val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtonePlayer = RingtoneManager.getRingtone(context, ringtone)
            ringtonePlayer?.play()
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }

    private fun playSound(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
