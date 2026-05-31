package com.clock3.pet.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.FloatingPetService
import com.clock3.pet.utils.AppLog
import com.clock3.pet.utils.CryptoUtils
import com.clock3.pet.utils.ThemeManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private val gson = Gson()
    private lateinit var importFileLauncher: ActivityResultLauncher<Intent>

    private lateinit var petSizeSlider: SeekBar
    private lateinit var petSizeValue: TextView
    private lateinit var petOpacitySlider: SeekBar
    private lateinit var petOpacityValue: TextView
    private lateinit var snoozeDurationSlider: SeekBar
    private lateinit var snoozeDurationValue: TextView
    private lateinit var maxSnoozeCountSlider: SeekBar
    private lateinit var maxSnoozeCountValue: TextView

    private lateinit var vibrationSwitch: SwitchCompat
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var sleepModeSwitch: SwitchCompat

    private val EXPORT_PASSWORD = CryptoUtils.getExportPassword()
    private var isLoadingSettings = false

    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))
        setContentView(R.layout.activity_settings)

        repository = Clock3Repository(this)

        importFileLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    importFromUri(uri)
                }
            }
        }

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

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<LinearLayout>(R.id.resetButton).setOnClickListener { resetSettings() }
        findViewById<LinearLayout>(R.id.exportButton).setOnClickListener { exportData() }
        findViewById<LinearLayout>(R.id.importButton).setOnClickListener { importData() }
    }

    private fun loadSettings() {
        isLoadingSettings = true
        val petSize = (repository.getConfig("pet_size", 100) as? Number)?.toInt() ?: 100
        val petOpacity = (repository.getConfig("pet_opacity", 1.0f) as? Number)?.toFloat() ?: 1.0f
        val snoozeDuration = (repository.getConfig("snooze_duration", 5) as? Number)?.toInt() ?: 5
        val maxSnoozeCount = (repository.getConfig("max_snooze_count", 3) as? Number)?.toInt() ?: 3
        val vibrationEnabled = repository.getConfig("vibration_enabled", true) as? Boolean ?: true
        val soundEnabled = repository.getConfig("sound_enabled", true) as? Boolean ?: true
        val sleepModeEnabled = repository.getConfig("sleep_mode_enabled", true) as? Boolean ?: true

        petSizeSlider.progress = petSize
        petSizeValue.text = petSize.toString()
        petOpacitySlider.progress = (petOpacity * 100).toInt()
        petOpacityValue.text = getString(R.string.settings_percent_format, (petOpacity * 100).toInt())
        snoozeDurationSlider.progress = snoozeDuration
        snoozeDurationValue.text = getString(R.string.settings_minutes_format, snoozeDuration)
        maxSnoozeCountSlider.progress = maxSnoozeCount
        maxSnoozeCountValue.text = getString(R.string.settings_count_format, maxSnoozeCount)

        vibrationSwitch.isChecked = vibrationEnabled
        soundSwitch.isChecked = soundEnabled
        sleepModeSwitch.isChecked = sleepModeEnabled
        isLoadingSettings = false
    }

    private fun setupListeners() {
        petSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                petSizeValue.text = progress.toString()
                if (!isLoadingSettings && fromUser) {
                    repository.setConfig("pet_size", progress)
                    FloatingPetService.notifySettingsChanged()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        petOpacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress / 100f
                petOpacityValue.text = getString(R.string.settings_percent_format, progress)
                if (!isLoadingSettings && fromUser) {
                    repository.setConfig("pet_opacity", opacity)
                    FloatingPetService.notifySettingsChanged()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        snoozeDurationSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                snoozeDurationValue.text = getString(R.string.settings_minutes_format, progress)
                if (!isLoadingSettings && fromUser) {
                    repository.setConfig("snooze_duration", progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        maxSnoozeCountSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                maxSnoozeCountValue.text = getString(R.string.settings_count_format, progress)
                if (!isLoadingSettings && fromUser) {
                    repository.setConfig("max_snooze_count", progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingSettings) {
                repository.setConfig("vibration_enabled", isChecked)
            }
        }

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingSettings) {
                repository.setConfig("sound_enabled", isChecked)
            }
        }

        sleepModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingSettings) {
                repository.setConfig("sleep_mode_enabled", isChecked)
            }
        }
    }

    private fun resetSettings() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_reset_title))
            .setMessage(getString(R.string.settings_reset_confirm))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                repository.setConfig("pet_size", 100)
                repository.setConfig("pet_opacity", 1.0f)
                repository.setConfig("snooze_duration", 5)
                repository.setConfig("max_snooze_count", 3)
                repository.setConfig("vibration_enabled", true)
                repository.setConfig("sound_enabled", true)
                repository.setConfig("sleep_mode_enabled", true)
                loadSettings()
                FloatingPetService.notifySettingsChanged()
                Toast.makeText(this, getString(R.string.settings_reset_done), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun importData() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        importFileLauncher.launch(Intent.createChooser(intent, getString(R.string.settings_choose_backup)))
    }

    private fun exportData() {
        lifecycleScope.launch {
            try {
                val data = repository.exportData()
                val json = gson.toJson(data)

                withContext(Dispatchers.IO) {
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val fileName = "clock3_backup_$timestamp.json"
                    val file = File(filesDir, fileName)
                    file.writeText(CryptoUtils.encrypt(json, EXPORT_PASSWORD), Charsets.UTF_8)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.settings_export_success, fileName),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Export failed", e)
                Toast.makeText(this@SettingsActivity, getString(R.string.settings_export_failed, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().use { it.readText() }
                    }
                } ?: throw Exception(getString(R.string.settings_cannot_read_file))

                val decryptedJson = withContext(Dispatchers.Default) {
                    CryptoUtils.decrypt(json, EXPORT_PASSWORD) ?: json
                }
                val data: Map<String, Any?> = withContext(Dispatchers.Default) {
                    val type = object : TypeToken<Map<String, Any?>>() {}.type
                    gson.fromJson<Map<String, Any?>>(decryptedJson, type)
                }

                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle(getString(R.string.settings_import_title))
                    .setMessage(getString(R.string.settings_import_confirm))
                    .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                        lifecycleScope.launch {
                            try {
                                val success = repository.importData(data)
                                if (success) {
                                    Toast.makeText(this@SettingsActivity, getString(R.string.settings_import_success), Toast.LENGTH_SHORT).show()
                                    FloatingPetService.notifySettingsChanged()
                                } else {
                                    Toast.makeText(this@SettingsActivity, getString(R.string.settings_import_failed), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                AppLog.e(TAG, "Import processing failed", e)
                                Toast.makeText(this@SettingsActivity, getString(R.string.settings_import_error, e.message), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            } catch (e: Exception) {
                AppLog.e(TAG, "Import read failed", e)
                Toast.makeText(this@SettingsActivity, getString(R.string.settings_import_error, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }
}
