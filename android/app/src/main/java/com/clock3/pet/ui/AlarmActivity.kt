package com.clock3.pet.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clock3.pet.R
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.receiver.AlarmReceiver
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.NotificationService
import com.clock3.pet.utils.ThemeManager
import kotlinx.coroutines.launch

class AlarmActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private lateinit var alarmService: AlarmService
    private lateinit var notificationService: NotificationService
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private var alarms: List<Alarm> = emptyList()
    private var selectedRepeatDays = booleanArrayOf(true, true, true, true, true, true, true)
    private var currentRepeatType = Alarm.RepeatType.ONCE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))

        repository = Clock3Repository(this)
        alarmService = AlarmService.getInstance(this)
        notificationService = NotificationService.getInstance(this)

        val action = intent?.getStringExtra("action")
        if (action == AlarmReceiver.ACTION_ALARM_TRIGGER) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            showAlarmTrigger()
            return
        }

        setContentView(R.layout.activity_alarm_list)

        recyclerView = findViewById(R.id.alarmsRecyclerView)
        emptyText = findViewById(R.id.emptyText)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.addAlarmButton).setOnClickListener { showAddAlarmDialog() }

        loadAlarms()
    }

    private fun showAlarmTrigger() {
        setContentView(R.layout.dialog_alarm_trigger)

        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) {
            finish()
            return
        }
        val alarmLabel = intent.getStringExtra("alarm_label") ?: getString(R.string.alarm_default_label)
        val alarmContent = intent.getStringExtra("alarm_content") ?: ""
        val alarmTime = intent.getStringExtra("alarm_time") ?: ""

        lifecycleScope.launch {
            val dbAlarm = repository.getAlarmById(alarmId)
            if (isFinishing || isDestroyed) return@launch
            val alarm = dbAlarm ?: Alarm(id = alarmId, label = alarmLabel, content = alarmContent, time = alarmTime)

            findViewById<TextView>(R.id.alarmLabel).text = alarm.label
            findViewById<TextView>(R.id.alarmContent).text = alarm.content.ifEmpty { getString(R.string.notification_alarm_text) }

            findViewById<LinearLayout>(R.id.btnSnooze).setOnClickListener {
                val snoozeMinutes = (repository.getConfig("snooze_duration", 5) as? Number)?.toInt() ?: 5
                alarmService.snoozeAlarm(snoozeMinutes)
                notificationService.stopSound()
                notificationService.cancelAlarmNotification(alarm.id)
                finish()
            }

            findViewById<LinearLayout>(R.id.btnDismiss).setOnClickListener {
                alarmService.dismissAlarm()
                notificationService.stopSound()
                notificationService.cancelAlarmNotification(alarm.id)
                finish()
            }
        }
    }

    private fun loadAlarms() {
        lifecycleScope.launch {
            alarms = repository.getAllAlarmsSync()
            updateAlarmList()
        }
    }

    private var alarmAdapter: AlarmAdapter? = null

    private fun updateAlarmList() {
        if (alarms.isEmpty()) {
            emptyText.visibility = TextView.VISIBLE
            recyclerView.visibility = RecyclerView.GONE
        } else {
            emptyText.visibility = TextView.GONE
            recyclerView.visibility = RecyclerView.VISIBLE
            if (alarmAdapter == null) {
                recyclerView.layoutManager = LinearLayoutManager(this)
                alarmAdapter = AlarmAdapter(alarms) { alarm, action ->
                    when (action) {
                        "toggle" -> toggleAlarm(alarm)
                        "delete" -> deleteAlarm(alarm)
                    }
                }
                recyclerView.adapter = alarmAdapter
            } else {
                alarmAdapter?.updateAlarms(alarms)
            }
        }
    }

    private fun showAddAlarmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_alarm, null)
        val labelInput = dialogView.findViewById<EditText>(R.id.labelInput)
        val contentInput = dialogView.findViewById<EditText>(R.id.contentInput)
        val hourSpinner = dialogView.findViewById<Spinner>(R.id.hourSpinner)
        val minuteSpinner = dialogView.findViewById<Spinner>(R.id.minuteSpinner)
        val repeatSpinner = dialogView.findViewById<Spinner>(R.id.repeatSpinner)

        setupTimeSpinners(hourSpinner, minuteSpinner)
        setupRepeatSpinner(repeatSpinner)

        selectedRepeatDays = booleanArrayOf(false, false, false, false, false, false, false)
        currentRepeatType = Alarm.RepeatType.ONCE

        repeatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                currentRepeatType = Alarm.RepeatType.entries[position]
                if (currentRepeatType == Alarm.RepeatType.CUSTOM) {
                    showCustomRepeatDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alarm_add_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val rawLabel = labelInput.text.toString().ifEmpty { getString(R.string.alarm_default_label) }
                val label = rawLabel.take(20)
                val content = contentInput.text.toString().take(50)
                val hour = hourSpinner.selectedItem.toString().toIntOrNull() ?: 8
                val minute = minuteSpinner.selectedItem.toString().toIntOrNull() ?: 0

                if (hour !in 0..23 || minute !in 0..59) {
                    Toast.makeText(this@AlarmActivity, getString(R.string.alarm_time_invalid), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val time = String.format("%02d:%02d", hour, minute)

                val repeatDays = mutableListOf<Int>()
                for (i in selectedRepeatDays.indices) {
                    if (selectedRepeatDays[i]) {
                        repeatDays.add(i + 1)
                    }
                }

                val alarm = Alarm(
                    label = label,
                    content = content,
                    time = time,
                    repeatType = currentRepeatType,
                    repeatDays = repeatDays,
                    enabled = true
                )

                lifecycleScope.launch {
                    val id = repository.addAlarm(alarm)
                    val savedAlarm = alarm.copy(id = id)
                    alarmService.scheduleAlarm(savedAlarm)
                    Toast.makeText(this@AlarmActivity, getString(R.string.alarm_saved), Toast.LENGTH_SHORT).show()
                    loadAlarms()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showCustomRepeatDialog() {
        val days = arrayOf(
            getString(R.string.day_monday), getString(R.string.day_tuesday),
            getString(R.string.day_wednesday), getString(R.string.day_thursday),
            getString(R.string.day_friday), getString(R.string.day_saturday),
            getString(R.string.day_sunday)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alarm_custom_repeat_title))
            .setMultiChoiceItems(days, selectedRepeatDays) { _, which, isChecked ->
                selectedRepeatDays[which] = isChecked
            }
            .setPositiveButton(getString(R.string.confirm), null)
            .show()
    }

    private fun setupTimeSpinners(hourSpinner: Spinner, minuteSpinner: Spinner) {
        val hours = (0..23).map { String.format("%02d", it) }
        val minutes = (0..59).map { String.format("%02d", it) }
        hourSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hours)
        minuteSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, minutes)
    }

    private fun setupRepeatSpinner(spinner: Spinner) {
        val repeatTypes = listOf(
            getString(R.string.repeat_once_short),
            getString(R.string.repeat_daily_short),
            getString(R.string.repeat_workdays_short),
            getString(R.string.repeat_weekend_short),
            getString(R.string.repeat_custom_short)
        )
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, repeatTypes)
    }

    private fun toggleAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            repository.toggleAlarm(alarm.id, !alarm.enabled)
            if (!alarm.enabled) {
                alarmService.scheduleAlarm(alarm.copy(enabled = true))
            } else {
                alarmService.cancelAlarm(alarm)
            }
            loadAlarms()
        }
    }

    private fun deleteAlarm(alarm: Alarm) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alarm_delete_title))
            .setMessage(getString(R.string.alarm_delete_confirm))
            .setPositiveButton(getString(R.string.alarm_delete)) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteAlarm(alarm.id)
                    alarmService.cancelAlarm(alarm)
                    Toast.makeText(this@AlarmActivity, getString(R.string.alarm_deleted), Toast.LENGTH_SHORT).show()
                    loadAlarms()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::recyclerView.isInitialized) {
            loadAlarms()
        }
    }
}
