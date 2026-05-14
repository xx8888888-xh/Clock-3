package com.clock3.pet.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.clock3.pet.R
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.FloatingPetService

class SettingsActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository

    private lateinit var petSizeSlider: SeekBar
    private lateinit var petSizeValue: TextView
    private lateinit var petOpacitySlider: SeekBar
    private lateinit var petOpacityValue: TextView
    private lateinit var snoozeDurationSlider: SeekBar
    private lateinit var snoozeDurationValue: TextView
    private lateinit var maxSnoozeCountSlider: SeekBar
    private lateinit var maxSnoozeCountValue: TextView

    private lateinit var vibrationSwitch: Switch
    private lateinit var soundSwitch: Switch
    private lateinit var sleepModeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        repository = Clock3Repository(this)

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        petSizeSlider = findViewById(R.id.petSizeSlider)
        petSizeValue = findViewById(R.id.petSizeValue)
        petOpacitySlider = findViewById(R.id.petOpacitySlider)
        petOpacityValue = findViewById(R.id.petOpacityValue)
        snoozeDurationSlider = findViewById(R.id.snoozeDurationSlider)
        snoozeDurationValue = findViewById(R.id.snoozeDurationValue)
        maxSnoozeCountSlider = findViewById(R.id.maxSnoozeCountSlider)
        maxSnoozeCountValue = findViewById(R.id.maxSnoozeCountValue)

        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        soundSwitch = findViewById(R.id.soundSwitch)
        sleepModeSwitch = findViewById(R.id.sleepModeSwitch)

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.resetButton).setOnClickListener { resetSettings() }
    }

    private fun loadSettings() {
        val petSize = repository.getConfig("pet_size", 100) as Int
        val petOpacity = repository.getConfig("pet_opacity", 1.0f) as Float
        val snoozeDuration = repository.getConfig("snooze_duration", 5) as Int
        val maxSnoozeCount = repository.getConfig("max_snooze_count", 3) as Int
        val vibrationEnabled = repository.getConfig("vibration_enabled", true) as Boolean
        val soundEnabled = repository.getConfig("sound_enabled", true) as Boolean
        val sleepModeEnabled = repository.getConfig("sleep_mode_enabled", true) as Boolean

        petSizeSlider.progress = petSize
        petSizeValue.text = "$petSize"
        petOpacitySlider.progress = (petOpacity * 100).toInt()
        petOpacityValue.text = "${(petOpacity * 100).toInt()}%"
        snoozeDurationSlider.progress = snoozeDuration
        snoozeDurationValue.text = "$snoozeDuration 分钟"
        maxSnoozeCountSlider.progress = maxSnoozeCount
        maxSnoozeCountValue.text = "$maxSnoozeCount 次"

        vibrationSwitch.isChecked = vibrationEnabled
        soundSwitch.isChecked = soundEnabled
        sleepModeSwitch.isChecked = sleepModeEnabled
    }

    private fun setupListeners() {
        petSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                petSizeValue.text = "$progress"
                repository.setConfig("pet_size", progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        petOpacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress / 100f
                petOpacityValue.text = "$progress%"
                repository.setConfig("pet_opacity", opacity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        snoozeDurationSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                snoozeDurationValue.text = "$progress 分钟"
                repository.setConfig("snooze_duration", progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        maxSnoozeCountSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxSnoozeCountValue.text = "$progress 次"
                repository.setConfig("max_snooze_count", progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            repository.setConfig("vibration_enabled", isChecked)
        }

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            repository.setConfig("sound_enabled", isChecked)
        }

        sleepModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            repository.setConfig("sleep_mode_enabled", isChecked)
        }
    }

    private fun resetSettings() {
        AlertDialog.Builder(this)
            .setTitle("恢复默认设置")
            .setMessage("确定要恢复所有设置为默认值吗？")
            .setPositiveButton("确定") { _, _ ->
                repository.setConfig("pet_size", 100)
                repository.setConfig("pet_opacity", 1.0f)
                repository.setConfig("snooze_duration", 5)
                repository.setConfig("max_snooze_count", 3)
                repository.setConfig("vibration_enabled", true)
                repository.setConfig("sound_enabled", true)
                repository.setConfig("sleep_mode_enabled", true)
                loadSettings()
                Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
