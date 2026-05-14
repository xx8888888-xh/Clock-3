package com.clock3.pet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clock3.pet.service.AlarmService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val alarmService = AlarmService.getInstance(context)
            alarmService.startChecking()
        }
    }
}
