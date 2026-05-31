package com.clock3.pet.service

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clock3.pet.R
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.model.Countdown
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.receiver.AlarmReceiver
import com.clock3.pet.ui.MainActivityNew
import com.clock3.pet.utils.AppLog

class NotificationService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val repository = Clock3Repository(appContext)
    @Volatile
    private var currentRingtone: android.media.Ringtone? = null
    private val ringtoneLock = Any()

    fun showAlarmNotification(alarm: Alarm, fullScreenPendingIntent: PendingIntent? = null, silent: Boolean = false) {
        val intent = Intent(appContext, MainActivityNew::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", AlarmReceiver.ACTION_ALARM_TRIGGER)
            putExtra("alarm_id", alarm.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH
        val category = if (silent) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_ALARM

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(appContext.getString(R.string.notification_alarm_title, alarm.label))
            .setContentText(alarm.content.ifEmpty { appContext.getString(R.string.notification_alarm_text) })
            .setPriority(priority)
            .setCategory(category)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        fullScreenPendingIntent?.let {
            builder.setFullScreenIntent(it, true)
        }

        val notification = builder.build()
        val notificationId = alarm.id.toInt()

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId + ALARM_NOTIFICATION_OFFSET, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification permission not granted", e)
        }

        if (!silent && (repository.getConfig("vibration_enabled", true) as? Boolean ?: true)) {
            vibrate()
        }

        if (!silent && (repository.getConfig("sound_enabled", true) as? Boolean ?: true)) {
            playSound()
        }
    }

    fun stopSound() {
        synchronized(ringtoneLock) {
            try {
                currentRingtone?.stop()
                currentRingtone = null
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to stop ringtone", e)
            }
        }
    }

    private fun playSound() {
        synchronized(ringtoneLock) {
            try {
                currentRingtone?.stop()
                currentRingtone = null

                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (alarmUri == null) {
                    AppLog.w(TAG, "No alarm or notification URI available")
                    return
                }
                currentRingtone = RingtoneManager.getRingtone(appContext, alarmUri)
                currentRingtone?.play()
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to play alarm sound", e)
            }
        }
    }

    fun showCountdownNotification(countdown: Countdown) {
        val intent = Intent(appContext, com.clock3.pet.ui.CountdownActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("countdown_id", countdown.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            countdown.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_COUNTDOWN)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(appContext.getString(R.string.notification_countdown_title, countdown.label))
            .setContentText(appContext.getString(R.string.notification_countdown_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationId = countdown.id.toInt()
            NotificationManagerCompat.from(appContext).notify(notificationId + COUNTDOWN_NOTIFICATION_OFFSET, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification permission not granted", e)
        }
    }

    fun showServiceNotification() {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(appContext.getString(R.string.notification_service_title))
            .setContentText(appContext.getString(R.string.notification_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(SERVICE_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification permission not granted", e)
        }
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(appContext).cancel(notificationId)
    }

    fun cancelAlarmNotification(alarmId: Long) {
        NotificationManagerCompat.from(appContext).cancel(alarmId.toInt() + ALARM_NOTIFICATION_OFFSET)
    }

    private fun vibrate() {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            }

            val vib = vibrator ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 200, 500, 200, 500),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to vibrate", e)
        }
    }

    fun release() {
        stopSound()
    }

    companion object {
        private const val TAG = "NotificationService"
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_COUNTDOWN = "countdown_channel"
        const val CHANNEL_GENERAL = "general_channel"
        const val CHANNEL_SERVICE = "service_channel"
        const val SERVICE_NOTIFICATION_ID = 99999
        const val ALARM_NOTIFICATION_OFFSET = 200000
        const val COUNTDOWN_NOTIFICATION_OFFSET = 300000

        @Volatile
        private var INSTANCE: NotificationService? = null

        fun getInstance(context: Context): NotificationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationService(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun releaseInstance() {
            INSTANCE?.release()
            INSTANCE = null
        }
    }
}
