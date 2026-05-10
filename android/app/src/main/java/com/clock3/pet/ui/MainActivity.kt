package com.clock3.pet.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clock3.pet.R
import com.clock3.pet.data.model.Alarm
import com.clock3.pet.data.model.Pet
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.CountdownService
import com.clock3.pet.service.FloatingPetService
import com.clock3.pet.service.NotificationService
import com.clock3.pet.service.PetService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private lateinit var alarmService: AlarmService
    private lateinit var countdownService: CountdownService
    private lateinit var petService: PetService
    private lateinit var notificationService: NotificationService

    private lateinit var statusText: TextView
    private lateinit var menuContainer: LinearLayout
    private lateinit var currentTimeText: TextView
    private lateinit var nextAlarmText: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestOverlayPermission()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startFloatingService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = Clock3Repository(this)
        alarmService = AlarmService.getInstance(this)
        countdownService = CountdownService.getInstance(this)
        petService = PetService.getInstance(this)
        notificationService = NotificationService.getInstance(this)

        initViews()
        setupClickListeners()
        observeData()
        handleIntent(intent)
        checkPermissions()
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        menuContainer = findViewById(R.id.menuContainer)
        currentTimeText = findViewById(R.id.currentTimeText)
        nextAlarmText = findViewById(R.id.nextAlarmText)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnAlarms).setOnClickListener { showAlarms() }
        findViewById<Button>(R.id.btnAddAlarm).setOnClickListener { showAddAlarm() }
        findViewById<Button>(R.id.btnCountdown).setOnClickListener { showCountdown() }
        findViewById<Button>(R.id.btnPetStatus).setOnClickListener { showPetStatus() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportData() }
        findViewById<Button>(R.id.btnImport).setOnClickListener { importData() }
        findViewById<Button>(R.id.btnFloatingPet).setOnClickListener { toggleFloatingPet() }
    }

    private fun observeData() {
        lifecycleScope.launch {
            petService.loadPet()
        }

        alarmService.onAlarmTrigger { alarm ->
            runOnUiThread {
                showAlarmDialog(alarm)
            }
        }

        updateStatus()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra("action")) {
            "main_menu" -> {}
            "alarms" -> showAlarms()
            "countdown" -> showCountdown()
            "pet_status" -> showPetStatus()
            "settings" -> showSettings()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        requestOverlayPermission()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("需要悬浮窗权限来显示桌面宠物")
                .setPositiveButton("去授权") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingPetService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "悬浮球已启动", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFloatingPet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else {
            startFloatingService()
        }
    }

    private fun updateStatus() {
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        currentTimeText.text = currentTime

        lifecycleScope.launch {
            val pet = petService.loadPet()
            val alarms = repository.getEnabledAlarms()
            statusText.text = "🐾 ${pet.name} | 心情: ${pet.mood.emoji}"

            val nextAlarm = alarmService.getNextAlarm()
            if (nextAlarm != null) {
                nextAlarmText.text = "下一个: ${nextAlarm.label} @ ${nextAlarm.time}"
            } else {
                nextAlarmText.text = "暂无闹钟"
            }
        }

        currentTimeText.postDelayed({ updateStatus() }, 1000)
    }

    private fun showAlarms() {
        lifecycleScope.launch {
            val alarms = repository.getAllAlarmsSync()
            showAlarmsList(alarms)
        }
    }

    private fun showAlarmsList(alarms: List<Alarm>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_alarms, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.alarmsRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AlarmAdapter(alarms, this) { alarm, action ->
            when (action) {
                "toggle" -> toggleAlarm(alarm)
                "delete" -> deleteAlarm(alarm)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("⏰ 闹钟列表")
            .setView(dialogView)
            .setPositiveButton("新建闹钟") { _, _ -> showAddAlarm() }
            .setNegativeButton("返回", null)
            .show()
    }

    private fun showAddAlarm() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_alarm, null)
        val labelInput = dialogView.findViewById<EditText>(R.id.labelInput)
        val contentInput = dialogView.findViewById<EditText>(R.id.contentInput)
        val hourSpinner = dialogView.findViewById<Spinner>(R.id.hourSpinner)
        val minuteSpinner = dialogView.findViewById<Spinner>(R.id.minuteSpinner)
        val repeatSpinner = dialogView.findViewById<Spinner>(R.id.repeatSpinner)

        setupTimeSpinners(hourSpinner, minuteSpinner)
        setupRepeatSpinner(repeatSpinner)

        AlertDialog.Builder(this)
            .setTitle("新建闹钟")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val label = labelInput.text.toString().ifEmpty { "闹钟" }
                val content = contentInput.text.toString()
                val hour = hourSpinner.selectedItem.toString().toIntOrNull() ?: 8
                val minute = minuteSpinner.selectedItem.toString().toIntOrNull() ?: 0
                val time = String.format("%02d:%02d", hour, minute)
                val repeat = (repeatSpinner.selectedItemPosition + 1).toString()

                val alarm = Alarm(
                    label = label,
                    content = content,
                    time = time,
                    repeatType = Alarm.RepeatType.entries[repeatSpinner.selectedItemPosition],
                    enabled = true
                )

                lifecycleScope.launch {
                    val id = repository.addAlarm(alarm)
                    val savedAlarm = alarm.copy(id = id)
                    alarmService.scheduleAlarm(savedAlarm)
                    Toast.makeText(this@MainActivity, "闹钟已保存", Toast.LENGTH_SHORT).show()
                    showAlarms()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupTimeSpinners(hourSpinner: Spinner, minuteSpinner: Spinner) {
        val hours = (0..23).map { String.format("%02d", it) }
        val minutes = (0..59).map { String.format("%02d", it) }

        hourSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hours)
        minuteSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, minutes)
    }

    private fun setupRepeatSpinner(spinner: Spinner) {
        val repeatTypes = listOf("一次", "每天", "工作日", "周末", "自定义")
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
            showAlarms()
        }
    }

    private fun deleteAlarm(alarm: Alarm) {
        AlertDialog.Builder(this)
            .setTitle("删除闹钟")
            .setMessage("确定要删除这个闹钟吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteAlarm(alarm.id)
                    alarmService.cancelAlarm(alarm)
                    Toast.makeText(this@MainActivity, "闹钟已删除", Toast.LENGTH_SHORT).show()
                    showAlarms()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAlarmDialog(alarm: Alarm) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_alarm_trigger, null)
        dialogView.findViewById<TextView>(R.id.alarmLabel).text = alarm.label
        dialogView.findViewById<TextView>(R.id.alarmContent).text = alarm.content.ifEmpty { "时间到了!" }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            alarmService.snoozeAlarm(5)
            notificationService.cancelAlarmNotification(alarm.id)
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            alarmService.dismissAlarm()
            notificationService.cancelAlarmNotification(alarm.id)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCountdown() {
        startActivity(Intent(this, CountdownActivity::class.java))
    }

    private fun showPetStatus() {
        startActivity(Intent(this, PetActivity::class.java))
    }

    private fun showSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun exportData() {
        lifecycleScope.launch {
            val data = repository.exportData()
            Toast.makeText(this@MainActivity, "数据导出成功: ${data.toString()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        Toast.makeText(this, "数据导入功能", Toast.LENGTH_SHORT).show()
    }
}
