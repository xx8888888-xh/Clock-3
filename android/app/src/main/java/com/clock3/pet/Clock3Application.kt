package com.clock3.pet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.CountdownService
import com.clock3.pet.service.NotificationService

class Clock3Application : Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()

        AlarmService.getInstance(this).startChecking()
        CountdownService.getInstance(this).startChecking()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                NotificationService.CHANNEL_ALARM,
                getString(R.string.notification_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "闹钟提醒通知"
                enableVibration(true)
            }

            val countdownChannel = NotificationChannel(
                NotificationService.CHANNEL_COUNTDOWN,
                getString(R.string.notification_channel_countdown),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "倒计时提醒通知"
            }

            val serviceChannel = NotificationChannel(
                NotificationService.CHANNEL_SERVICE,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮球服务通知"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(
                listOf(alarmChannel, countdownChannel, serviceChannel)
            )
        }
    }
}
