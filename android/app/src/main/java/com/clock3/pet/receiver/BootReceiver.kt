package com.clock3.pet.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.AlarmService
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT_POWERON) return

        val repository = Clock3Repository(context.applicationContext)
        val pendingResult = goAsync()

        val supervisorJob = SupervisorJob()
        CoroutineScope(supervisorJob + Dispatchers.IO).launch {
            try {
                withTimeout(9000) {
                    val alarms = repository.getAllAlarmsSync()
                    val alarmService = AlarmService.getInstance(context.applicationContext)
                    for (alarm in alarms) {
                        if (alarm.enabled) {
                            try {
                                alarmService.scheduleAlarm(alarm)
                            } catch (e: Exception) {
                                AppLog.e(TAG, "Failed to schedule alarm ${alarm.id}", e)
                            }
                        }
                    }
                    alarmService.startChecking()
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to reschedule alarms", e)
            } finally {
                supervisorJob.cancel()
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
