package com.clock3.pet.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.ShopRepository
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.FloatingPetService
import com.clock3.pet.service.PetService
import com.clock3.pet.utils.ExpCalculator
import com.clock3.pet.utils.ThemeManager
import com.clock3.pet.widget.CirclePetView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PomodoroActivity : AppCompatActivity() {

    private lateinit var repository: Clock3Repository
    private lateinit var shopRepository: ShopRepository
    private lateinit var petService: PetService

    private lateinit var eventInput: EditText
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressRing: ProgressBar
    private lateinit var petView: CirclePetView
    private lateinit var completedCountText: TextView
    private lateinit var expGainedText: TextView
    private lateinit var btnPause: Button
    private lateinit var btnSkip: Button
    private lateinit var btnSave: Button
    private lateinit var btnUseBreakTicket: Button

    private var focusSeconds = 25 * 60
    private var breakSeconds = 5 * 60
    private var autoStartBreak = false
    private var totalSeconds = 25 * 60
    private var remainingSeconds = totalSeconds
    private var isRunning = false
    private var isPaused = false
    private var completedPomodoros = 0
    private var totalExpGained = 0
    private var isBreakTime = false
    private var wasSkipped = false

    companion object {
        const val KEY_REMAINING_SECONDS = "remaining_seconds"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_IS_PAUSED = "is_paused"
        const val KEY_IS_BREAK_TIME = "is_break_time"
        const val KEY_COMPLETED_POMODOROS = "completed_pomodoros"
        const val KEY_TOTAL_EXP = "total_exp"
        const val KEY_WAS_SKIPPED = "was_skipped"
        const val TIMER_TICK_MS = 1000L
        const val KEY_BG_SAVE_TIME = "pomodoro_bg_save_time"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) {
                saveBackgroundState()
                return
            }
            if (isRunning && !isPaused) {
                remainingSeconds--
                if (remainingSeconds % 5 == 0) {
                    saveBackgroundState()
                }
                updateTimerDisplay()
                updateProgressRing()
                updateFloatingService()

                if (remainingSeconds <= 0) {
                    onTimerComplete()
                } else {
                    handler.postDelayed(this, TIMER_TICK_MS)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))
        setContentView(R.layout.activity_pomodoro)

        repository = Clock3Repository(this)
        shopRepository = ShopRepository(this)
        petService = PetService.getInstance(this)

        loadSettings()
        initViews()
        setupClickListeners()
        loadTodayStats()
        updateTimerDisplay()
        updateProgressRing()
        updatePetMood(CirclePetView.Mood.HAPPY)

        savedInstanceState?.let {
            remainingSeconds = it.getInt(KEY_REMAINING_SECONDS, totalSeconds)
            isRunning = it.getBoolean(KEY_IS_RUNNING, false)
            isPaused = it.getBoolean(KEY_IS_PAUSED, false)
            isBreakTime = it.getBoolean(KEY_IS_BREAK_TIME, false)
            completedPomodoros = it.getInt(KEY_COMPLETED_POMODOROS, 0)
            totalExpGained = it.getInt(KEY_TOTAL_EXP, 0)
            wasSkipped = it.getBoolean(KEY_WAS_SKIPPED, false)

            if (isRunning && !isPaused) {
                startTimer()
            } else if (isPaused) {
                updateTimerDisplay()
                updateProgressRing()
                btnPause.text = getString(R.string.countdown_resume)
                statusText.text = getString(R.string.pomodoro_paused)
                updatePetMood(CirclePetView.Mood.HUNGRY)
            }
        }

        if (intent.getBooleanExtra("auto_start", false) && !isRunning) {
            startTimer()
        }

        restoreBackgroundState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_REMAINING_SECONDS, remainingSeconds)
        outState.putBoolean(KEY_IS_RUNNING, isRunning)
        outState.putBoolean(KEY_IS_PAUSED, isPaused)
        outState.putBoolean(KEY_IS_BREAK_TIME, isBreakTime)
        outState.putInt(KEY_COMPLETED_POMODOROS, completedPomodoros)
        outState.putInt(KEY_TOTAL_EXP, totalExpGained)
        outState.putBoolean(KEY_WAS_SKIPPED, wasSkipped)
    }

    private fun loadSettings() {
        focusSeconds = ((repository.getConfig("pomodoro_focus", 25) as? Number)?.toInt() ?: 25) * 60
        breakSeconds = ((repository.getConfig("pomodoro_break", 5) as? Number)?.toInt() ?: 5) * 60
        autoStartBreak = (repository.getConfig("pomodoro_auto_break", false) as? Boolean) ?: false
        totalSeconds = focusSeconds
        remainingSeconds = totalSeconds
    }

    private fun initViews() {
        eventInput = findViewById(R.id.eventInput)
        timerText = findViewById(R.id.timerText)
        statusText = findViewById(R.id.statusText)
        progressRing = findViewById(R.id.progressRing)
        petView = findViewById(R.id.petView)
        completedCountText = findViewById(R.id.completedCountText)
        expGainedText = findViewById(R.id.expGainedText)
        btnPause = findViewById(R.id.btnPause)
        btnSkip = findViewById(R.id.btnSkip)
        btnSave = findViewById(R.id.btnSave)
        btnUseBreakTicket = findViewById(R.id.btnUseBreakTicket)

        updateBreakTicketButton()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { showSettings() }
        findViewById<LinearLayout>(R.id.btnExpShop).setOnClickListener { showExpShop() }
    }

    private fun setupClickListeners() {
        btnPause.setOnClickListener { togglePause() }
        btnSkip.setOnClickListener { skipTimer() }
        btnSave.setOnClickListener { saveConfiguration() }
        btnUseBreakTicket.setOnClickListener { useBreakTicket() }
    }

    private fun updateBreakTicketButton() {
        val tickets = shopRepository.getBreakTickets()
        btnUseBreakTicket.visibility = if (tickets > 0) View.VISIBLE else View.GONE
        btnUseBreakTicket.text = getString(R.string.pomodoro_break_ticket, tickets)
    }

    private fun useBreakTicket() {
        val tickets = shopRepository.getBreakTickets()
        if (tickets <= 0) {
            Toast.makeText(this, getString(R.string.pomodoro_no_break_ticket), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pomodoro_use_break_ticket_title))
            .setMessage(getString(R.string.pomodoro_use_break_ticket_msg))
            .setPositiveButton(getString(R.string.pomodoro_use_10min)) { _, _ ->
                if (shopRepository.useBreakTicket(10)) {
                    if (isBreakTime) {
                        remainingSeconds += 10 * 60
                        updateTimerDisplay()
                        updateProgressRing()
                    }
                    Toast.makeText(this, getString(R.string.pomodoro_used_break_ticket), Toast.LENGTH_SHORT).show()
                    updateBreakTicketButton()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadTodayStats() {
        completedPomodoros = repository.getTodayPomodoroCount()
        totalExpGained = repository.getTodayExp()
        updateStatsDisplay()
    }

    private fun updateTimerDisplay() {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgressRing() {
        if (totalSeconds > 0) {
            val progress = (totalSeconds - remainingSeconds) * 100 / totalSeconds
            progressRing.progress = progress
        }
    }

    private fun updateStatsDisplay() {
        completedCountText.text = completedPomodoros.toString()
        expGainedText.text = "+$totalExpGained"
    }

    private fun updatePetMood(mood: CirclePetView.Mood) {
        petView.setMood(mood)
    }

    private fun updateFloatingService() {
        FloatingPetService.updatePomodoroState(
            this,
            isRunning,
            isPaused,
            isBreakTime,
            remainingSeconds,
            totalSeconds
        )
    }

    private fun togglePause() {
        if (!isRunning) {
            startTimer()
        } else if (isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }

    private fun startTimer() {
        isRunning = true
        isPaused = false
        btnPause.text = getString(R.string.countdown_pause)
        statusText.text = if (isBreakTime) getString(R.string.pomodoro_break) else getString(R.string.pomodoro_focusing)
        updatePetMood(if (isBreakTime) CirclePetView.Mood.RESTING else CirclePetView.Mood.FOCUSED)
        updateFloatingService()
        handler.post(timerRunnable)
    }

    private fun pauseTimer() {
        isPaused = true
        btnPause.text = getString(R.string.countdown_resume)
        statusText.text = getString(R.string.pomodoro_paused)
        updatePetMood(CirclePetView.Mood.HUNGRY)
        updateFloatingService()
    }

    private fun resumeTimer() {
        isPaused = false
        btnPause.text = getString(R.string.countdown_pause)
        statusText.text = if (isBreakTime) getString(R.string.pomodoro_break) else getString(R.string.pomodoro_focusing)
        updatePetMood(if (isBreakTime) CirclePetView.Mood.RESTING else CirclePetView.Mood.FOCUSED)
        updateFloatingService()
        handler.post(timerRunnable)
    }

    private fun skipTimer() {
        if (isRunning) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.pomodoro_skip_title))
                .setMessage(getString(R.string.pomodoro_skip_confirm, if (isBreakTime) getString(R.string.pomodoro_break_phase) else getString(R.string.pomodoro_focus_phase)))
                .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    isRunning = false
                    wasSkipped = !isBreakTime
                    handler.removeCallbacks(timerRunnable)
                    updateFloatingService()
                    startNextPhase()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            startNextPhase()
        }
    }

    private fun onTimerComplete() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        updateFloatingService()

        if (!isBreakTime) {
            completedPomodoros++
            val expEarned = calculateExp()
            totalExpGained += expEarned

            repository.savePomodoroRecord(completedPomodoros, totalExpGained)
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    petService.addExp(expEarned)
                }
            }

            updateStatsDisplay()
            updatePetMood(CirclePetView.Mood.EXCITED)
            Toast.makeText(this, getString(R.string.pomodoro_exp_gained, expEarned), Toast.LENGTH_SHORT).show()
        }

        wasSkipped = false
        startNextPhase()
    }

    private fun startNextPhase() {
        isBreakTime = !isBreakTime

        if (isBreakTime) {
            totalSeconds = breakSeconds
            remainingSeconds = totalSeconds
            statusText.text = getString(R.string.pomodoro_break_time)
            updatePetMood(CirclePetView.Mood.RESTING)
            
            if (autoStartBreak) {
                startTimer()
            } else {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.pomodoro_break_alert_title))
                    .setMessage(getString(R.string.pomodoro_break_alert_msg, breakSeconds / 60))
                    .setPositiveButton(getString(R.string.pomodoro_start_break)) { _, _ ->
                        startTimer()
                    }
                    .show()
            }
        } else {
            totalSeconds = focusSeconds
            remainingSeconds = totalSeconds
            statusText.text = getString(R.string.pomodoro_focusing)
            updatePetMood(CirclePetView.Mood.FOCUSED)
        }

        updateTimerDisplay()
        updateProgressRing()
        btnPause.text = getString(R.string.countdown_start)
        updateFloatingService()
    }

    private fun calculateExp(): Int {
        val hasEvent = eventInput.text.toString().isNotBlank()
        val actualFocusSeconds = if (wasSkipped) {
            (focusSeconds - remainingSeconds).coerceAtLeast(60)
        } else {
            focusSeconds
        }
        return ExpCalculator.calculatePomodoroExp(
            focusSeconds = actualFocusSeconds,
            completedFully = !wasSkipped,
            hasEvent = hasEvent,
            completedPomodoros = completedPomodoros
        )
    }

    private fun saveConfiguration() {
        val eventName = eventInput.text.toString()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pomodoro_save_config_title))
            .setMessage(getString(R.string.pomodoro_save_config_msg))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                repository.setConfig("pomodoro_event", eventName)
                repository.setConfig("pomodoro_focus", focusSeconds / 60)
                repository.setConfig("pomodoro_break", breakSeconds / 60)
                repository.setConfig("pomodoro_auto_break", autoStartBreak)
                Toast.makeText(this@PomodoroActivity, getString(R.string.pomodoro_config_saved), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showSettings() {
        val options = arrayOf(
            getString(R.string.pomodoro_focus_duration, focusSeconds / 60), 
            getString(R.string.pomodoro_break_duration, breakSeconds / 60), 
            getString(R.string.pomodoro_auto_break, if (autoStartBreak) getString(R.string.pomodoro_auto_break_on) else getString(R.string.pomodoro_auto_break_off))
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pomodoro_settings_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFocusTimePicker()
                    1 -> showBreakTimePicker()
                    2 -> showAutoBreakToggle()
                }
            }
            .setNegativeButton(getString(R.string.back), null)
            .show()
    }

    private fun showFocusTimePicker() {
        val minutes = arrayOf("15", "20", "25", "30", "45", "60")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pomodoro_select_focus_title))
            .setItems(minutes) { _, which ->
                val newMinutes = minutes[which].toInt()
                focusSeconds = newMinutes * 60
                if (!isRunning && !isBreakTime) {
                    totalSeconds = focusSeconds
                    remainingSeconds = totalSeconds
                    updateTimerDisplay()
                    updateProgressRing()
                }
                Toast.makeText(this, getString(R.string.pomodoro_focus_time_set, newMinutes), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showBreakTimePicker() {
        val minutes = arrayOf("5", "10", "15", "20", "30")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pomodoro_select_break_title))
            .setItems(minutes) { _, which ->
                val newMinutes = minutes[which].toInt()
                breakSeconds = newMinutes * 60
                if (!isRunning && isBreakTime) {
                    totalSeconds = breakSeconds
                    remainingSeconds = totalSeconds
                    updateTimerDisplay()
                    updateProgressRing()
                }
                Toast.makeText(this, getString(R.string.pomodoro_break_set, newMinutes), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showAutoBreakToggle() {
        val options = arrayOf(getString(R.string.pomodoro_auto_break_on), getString(R.string.pomodoro_auto_break_off))
        val currentIndex = if (autoStartBreak) 0 else 1
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.pomodoro_auto_break))
            .setSingleChoiceItems(options, currentIndex) { _, which ->
                autoStartBreak = which == 0
                Toast.makeText(this, getString(R.string.pomodoro_auto_break_toggled, if (autoStartBreak) getString(R.string.pomodoro_auto_break_on) else getString(R.string.pomodoro_auto_break_off)), Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(getString(R.string.confirm), null)
            .show()
    }

    private fun showExpShop() {
        startActivity(Intent(this, ShopActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        updateBreakTicketButton()
    }

    override fun onPause() {
        super.onPause()
        saveBackgroundState()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        if (isRunning) {
            saveBackgroundState()
        }
    }

    private fun saveBackgroundState() {
        repository.setConfig("pomodoro_bg_remaining", remainingSeconds)
        repository.setConfig("pomodoro_bg_running", isRunning)
        repository.setConfig("pomodoro_bg_paused", isPaused)
        repository.setConfig("pomodoro_bg_break", isBreakTime)
        repository.setConfig("pomodoro_bg_completed", completedPomodoros)
        repository.setConfig("pomodoro_bg_exp", totalExpGained)
        repository.setConfig("pomodoro_bg_focus_seconds", focusSeconds)
        repository.setConfig("pomodoro_bg_break_seconds", breakSeconds)
        repository.setConfig("pomodoro_bg_total_seconds", totalSeconds)
        repository.setConfig(KEY_BG_SAVE_TIME, System.currentTimeMillis())
    }

    private fun restoreBackgroundState() {
        if (isRunning) return
        val bgRemaining = (repository.getConfig("pomodoro_bg_remaining", -1) as? Number)?.toInt() ?: -1
        if (bgRemaining <= 0) return

        val bgRunning = repository.getConfig("pomodoro_bg_running", false) as? Boolean ?: false
        val bgPaused = repository.getConfig("pomodoro_bg_paused", false) as? Boolean ?: false
        val bgBreak = repository.getConfig("pomodoro_bg_break", false) as? Boolean ?: false
        val bgCompleted = (repository.getConfig("pomodoro_bg_completed", 0) as? Number)?.toInt() ?: 0
        val bgExp = (repository.getConfig("pomodoro_bg_exp", 0) as? Number)?.toInt() ?: 0
        val bgFocusSeconds = (repository.getConfig("pomodoro_bg_focus_seconds", 25 * 60) as? Number)?.toInt() ?: 25 * 60
        val bgBreakSeconds = (repository.getConfig("pomodoro_bg_break_seconds", 5 * 60) as? Number)?.toInt() ?: 5 * 60
        val bgTotalSeconds = (repository.getConfig("pomodoro_bg_total_seconds", 25 * 60) as? Number)?.toInt() ?: 25 * 60
        val bgSaveTime = (repository.getConfig(KEY_BG_SAVE_TIME, 0L) as? Number)?.toLong() ?: 0L

        if (bgRunning) {
            remainingSeconds = bgRemaining
            isBreakTime = bgBreak
            focusSeconds = bgFocusSeconds
            breakSeconds = bgBreakSeconds
            totalSeconds = bgTotalSeconds
            completedPomodoros = bgCompleted
            totalExpGained = bgExp

            if (!bgPaused && bgSaveTime > 0) {
                val elapsedSeconds = ((System.currentTimeMillis() - bgSaveTime) / 1000).toInt()
                remainingSeconds = (bgRemaining - elapsedSeconds).coerceAtLeast(0)
                if (remainingSeconds == 0) {
                    isBreakTime = !isBreakTime
                    totalSeconds = if (isBreakTime) breakSeconds else focusSeconds
                    remainingSeconds = totalSeconds
                }
            }

            if (bgPaused) {
                isRunning = true
                isPaused = true
                updateTimerDisplay()
                updateProgressRing()
                btnPause.text = getString(R.string.countdown_resume)
                statusText.text = getString(R.string.pomodoro_paused)
                updatePetMood(CirclePetView.Mood.HUNGRY)
                updateFloatingService()
            } else {
                updateTimerDisplay()
                updateProgressRing()
                updateStatsDisplay()
                startTimer()
            }

            repository.setConfig("pomodoro_bg_remaining", -1)
        }
    }
}
