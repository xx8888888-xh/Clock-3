package com.clock3.pet

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.CountdownService
import com.clock3.pet.service.NotificationService
import com.clock3.pet.service.PetService
import com.clock3.pet.widget.CirclePetView

class Clock3Application : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
        startServices()
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    NotificationService.CHANNEL_ALARM,
                    getString(R.string.notification_channel_alarm),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.notification_channel_alarm_desc)
                    enableVibration(true)
                    setBypassDnd(true)
                },
                NotificationChannel(
                    NotificationService.CHANNEL_COUNTDOWN,
                    getString(R.string.notification_channel_countdown),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.notification_channel_countdown_desc)
                },
                NotificationChannel(
                    NotificationService.CHANNEL_GENERAL,
                    getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = getString(R.string.notification_channel_general_desc)
                },
                NotificationChannel(
                    NotificationService.CHANNEL_SERVICE,
                    getString(R.string.notification_channel_service),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_service_desc)
                }
            )

            channels.forEach { channel ->
                if (notificationManager.getNotificationChannel(channel.id) == null) {
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }

    private fun startServices() {
        val alarmService = AlarmService.getInstance(this)
        if (!alarmService.isRunning()) {
            alarmService.startChecking()
        }
        val countdownService = CountdownService.getInstance(this)
        if (!countdownService.isRunning()) {
            countdownService.startChecking()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        releaseAllServices()
    }

    fun releaseAllServices() {
        PetService.releaseInstance()
        AlarmService.releaseInstance()
        CountdownService.releaseInstance()
        NotificationService.releaseInstance()
        CirclePetView.clearSharedCache()
    }
}
