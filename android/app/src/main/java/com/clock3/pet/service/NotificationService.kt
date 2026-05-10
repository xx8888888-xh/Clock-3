package com.clock3.pet.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
import com.clock3.pet.ui.MainActivity

class NotificationService(private val context: Context) {
    private val repository = Clock3Repository(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.notification_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "闹钟提醒通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val countdownChannel = NotificationChannel(
                CHANNEL_COUNTDOWN,
                context.getString(R.string.notification_channel_countdown),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "倒计时提醒通知"
                enableVibration(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮球服务通知"
            }

            notificationManager.createNotificationChannels(
                listOf(alarmChannel, countdownChannel, serviceChannel)
            )
        }
    }

    fun showAlarmNotification(alarm: Alarm) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "alarm")
            putExtra("alarm_id", alarm.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_alarm_title, alarm.label))
            .setContentText(alarm.content.ifEmpty { context.getString(R.string.notification_alarm_text) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alarm.id.toInt(), notification)
        } catch (e: SecurityException) {
        }

        if (repository.getConfig("vibration_enabled", true) as Boolean) {
            vibrate()
        }
    }

    fun showCountdownNotification(countdown: Countdown) {
        val notification = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_countdown_title, countdown.label))
            .setContentText(context.getString(R.string.notification_countdown_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(countdown.id.toInt() + 10000, notification)
        } catch (e: SecurityException) {
        }
    }

    fun showServiceNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_service_title))
            .setContentText(context.getString(R.string.notification_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(SERVICE_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
        }
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAlarmNotification(alarmId: Long) {
        notificationManager.cancel(alarmId.toInt())
    }

    private fun vibrate() {
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

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_COUNTDOWN = "countdown_channel"
        const val CHANNEL_SERVICE = "service_channel"
        const val SERVICE_NOTIFICATION_ID = 99999

        @Volatile
        private var INSTANCE: NotificationService? = null

        fun getInstance(context: Context): NotificationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
