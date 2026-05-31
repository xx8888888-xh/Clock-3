package com.clock3.pet.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.model.Pet
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.AlarmService
import com.clock3.pet.service.FloatingPetService
import com.clock3.pet.service.PetService
import com.clock3.pet.utils.ThemeManager
import com.clock3.pet.widget.CirclePetView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivityNew : AppCompatActivity() {

    private lateinit var repository: Clock3Repository
    private lateinit var petService: PetService
    private lateinit var alarmService: AlarmService
    private lateinit var circlePetView: CirclePetView
    private lateinit var petLevelText: TextView
    private lateinit var expProgressBar: ProgressBar
    private lateinit var nextAlarmText: TextView
    private lateinit var currentTimeText: TextView
    private var isFloatingBallEnabled = false

    companion object {
        const val CLOCK_UPDATE_INTERVAL_MS = 1000L
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val denied = permissions.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_notification))
                .setMessage(getString(R.string.permission_denied))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startFloatingPetService()
        } else {
            Toast.makeText(this, getString(R.string.permission_overlay_required), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))
        setContentView(R.layout.activity_main_new)

        repository = Clock3Repository(this)
        petService = PetService.getInstance(this)
        alarmService = AlarmService.getInstance(this)
        alarmService.startChecking()

        initViews()
        setupClickListeners()
        checkPermissions()
        loadPetData()
        startClockUpdate()
    }

    private fun initViews() {
        circlePetView = findViewById(R.id.circlePetView)
        petLevelText = findViewById(R.id.petLevelText)
        expProgressBar = findViewById(R.id.expProgressBar)
        nextAlarmText = findViewById(R.id.nextAlarmText)
        currentTimeText = findViewById(R.id.currentTimeText)
    }

    private fun setupClickListeners() {
        findViewById<LinearLayout>(R.id.btnAlarm).setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnPomodoro).setOnClickListener {
            startActivity(Intent(this, PomodoroActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnPet).setOnClickListener {
            startActivity(Intent(this, PetActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnCountdown).setOnClickListener {
            startActivity(Intent(this, CountdownActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnFloatingBall).setOnClickListener {
            toggleFloatingBall()
        }
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        checkFloatingBallPermission()
    }

    private fun checkFloatingBallPermission() {
        lifecycleScope.launch {
            isFloatingBallEnabled = repository.getConfig("floating_pet_enabled", false) as? Boolean ?: false
            if (isFloatingBallEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivityNew)) {
                repository.setConfig("floating_pet_enabled", false)
                isFloatingBallEnabled = false
            }
            if (isFloatingBallEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this@MainActivityNew)) {
                startFloatingPetService()
            }
        }
    }

    private fun loadPetData() {
        lifecycleScope.launch {
            val pet = petService.loadPet()
            updateUI(pet)
        }
        updateNextAlarm()
    }

    private fun updateUI(pet: Pet) {
        petLevelText.text = getString(R.string.main_level_format, pet.level)
        val expPercent = (if (pet.getExpForNextLevel() > 0) {
            pet.exp * 100 / pet.getExpForNextLevel()
        } else {
            0
        }).coerceAtMost(100)
        expProgressBar.progress = expPercent
        circlePetView.setMood(PetMoodMapper.map(pet.mood))
    }

    private fun updateNextAlarm() {
        lifecycleScope.launch {
            val nextAlarm = alarmService.getNextAlarm()
            if (nextAlarm != null) {
                nextAlarmText.text = getString(R.string.main_next_alarm, nextAlarm.time)
                nextAlarmText.visibility = TextView.VISIBLE
            } else {
                nextAlarmText.visibility = TextView.GONE
            }
        }
    }

    private fun startClockUpdate() {
        lifecycleScope.launch {
            while (isActive) {
                val timeStr = LocalDateTime.now().format(TIME_FORMATTER)
                currentTimeText.text = timeStr
                delay(CLOCK_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun toggleFloatingBall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return
        }

        isFloatingBallEnabled = !isFloatingBallEnabled
        lifecycleScope.launch {
            repository.setConfig("floating_pet_enabled", isFloatingBallEnabled)
        }
        if (isFloatingBallEnabled) {
            startFloatingPetService()
            Toast.makeText(this, getString(R.string.main_floating_pet_shown), Toast.LENGTH_SHORT).show()
        } else {
            stopService(Intent(this, FloatingPetService::class.java))
            Toast.makeText(this, getString(R.string.main_floating_pet_hidden), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFloatingPetService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, FloatingPetService::class.java))
        } else {
            startService(Intent(this, FloatingPetService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadPetData()
        updateNextAlarm()
        checkFloatingBallPermission()
    }
}
