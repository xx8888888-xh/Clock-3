package com.clock3.pet.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.clock3.pet.R
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.NotificationService

class AlarmActivity : AppCompatActivity() {
    private lateinit var alarmService: AlarmService
    private lateinit var notificationService: NotificationService

    private var alarm: Alarm? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        alarmService = AlarmService.getInstance(this)
        notificationService = NotificationService.getInstance(this)

        val alarmId = intent.getLongExtra("alarm_id", -1)
        val alarmLabel = intent.getStringExtra("alarm_label") ?: "闹钟"
        val alarmContent = intent.getStringExtra("alarm_content") ?: ""
        val alarmTime = intent.getStringExtra("alarm_time") ?: ""

        alarm = Alarm(
            id = alarmId,
            label = alarmLabel,
            content = alarmContent,
            time = alarmTime
        )

        initViews()
        setupListeners()
    }

    private fun initViews() {
        alarm?.let { a ->
            findViewById<TextView>(R.id.alarmLabel).text = a.label
            findViewById<TextView>(R.id.alarmContent).text = a.content.ifEmpty { "时间到了!" }
        }
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            alarm?.let { a ->
                alarmService.snoozeAlarm(5)
                notificationService.cancelAlarmNotification(a.id)
                finish()
            }
        }

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            alarm?.let { a ->
                alarmService.dismissAlarm()
                notificationService.cancelAlarmNotification(a.id)
                finish()
            }
        }
    }
}
