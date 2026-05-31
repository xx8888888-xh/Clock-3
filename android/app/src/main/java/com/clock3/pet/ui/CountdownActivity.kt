package com.clock3.pet.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.model.Countdown
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.CountdownService
import com.clock3.pet.service.NotificationService
import com.clock3.pet.utils.AppLog
import com.clock3.pet.utils.ThemeManager
import kotlinx.coroutines.launch

class CountdownActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private lateinit var countdownService: CountdownService
    private lateinit var notificationService: NotificationService

    private lateinit var countdownText: TextView
    private lateinit var labelInput: EditText
    private lateinit var minuteInput: EditText
    private lateinit var secondInput: EditText
    private lateinit var startButton: LinearLayout
    private lateinit var pauseButton: LinearLayout
    private lateinit var startButtonText: TextView
    private lateinit var resetButton: LinearLayout
    private lateinit var pauseButtonText: TextView

    private var countDownTimer: CountDownTimer? = null
    private var currentCountdownId: Long? = null
    private var remainingMillis: Long = 0
    private var isRunning = false

    companion object {
        const val KEY_REMAINING_MILLIS = "remaining_millis"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_COUNTDOWN_ID = "countdown_id"
        const val MAX_COUNTDOWN_SECONDS = 86400
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this, ThemeManager.getCurrentTheme(this))
        setContentView(R.layout.activity_countdown)

        repository = Clock3Repository(this)
        countdownService = CountdownService.getInstance(this)
        notificationService = NotificationService.getInstance(this)

        initViews()
        setupListeners()

        savedInstanceState?.let {
            remainingMillis = it.getLong(KEY_REMAINING_MILLIS, 0)
            isRunning = it.getBoolean(KEY_IS_RUNNING, false)
            currentCountdownId = it.getLong(KEY_COUNTDOWN_ID, -1).takeIf { id -> id != -1L }

            if (remainingMillis > 0) {
                updateDisplay()
                if (isRunning) {
                    startCountDownTimer(remainingMillis)
                    startButton.isEnabled = false
                    pauseButton.isEnabled = true
                    startButtonText.text = getString(R.string.countdown_running)
                } else {
                    startButton.isEnabled = true
                    pauseButton.isEnabled = true
                    startButtonText.text = getString(R.string.countdown_start)
                    pauseButtonText.text = getString(R.string.countdown_resume)
                }
            }
        }

        if (savedInstanceState == null) {
            val restored = restoreCountdownState()
            if (!restored) {
                lifecycleScope.launch {
                    val countdowns = repository.getAllCountdownsSync()
                    val runningCountdown = countdowns.find { it.status == Countdown.CountdownStatus.RUNNING }
                        ?: countdowns.find { it.status == Countdown.CountdownStatus.PAUSED }

                    runningCountdown?.let { countdown ->
                        currentCountdownId = countdown.id
                        countdown.updateRemaining()
                        val remaining = countdown.remainingSeconds
                        remainingMillis = remaining * 1000L
                        labelInput.setText(countdown.label)

                        if (remaining > 0) {
                            updateDisplay()
                            if (countdown.status == Countdown.CountdownStatus.RUNNING) {
                                isRunning = true
                                startCountDownTimer(remainingMillis)
                                startButton.isEnabled = false
                                pauseButton.isEnabled = true
                                startButtonText.text = getString(R.string.countdown_running)
                            } else {
                                isRunning = false
                                startButton.isEnabled = true
                                pauseButton.isEnabled = true
                                startButtonText.text = getString(R.string.countdown_start)
                                pauseButtonText.text = getString(R.string.countdown_resume)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_REMAINING_MILLIS, remainingMillis)
        outState.putBoolean(KEY_IS_RUNNING, isRunning)
        outState.putLong(KEY_COUNTDOWN_ID, currentCountdownId ?: -1)
    }

    private fun initViews() {
        countdownText = findViewById(R.id.countdownText)
        labelInput = findViewById(R.id.labelInput)
        minuteInput = findViewById(R.id.minuteInput)
        secondInput = findViewById(R.id.secondInput)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        resetButton = findViewById(R.id.resetButton)
        startButtonText = startButton.findViewById(R.id.startButtonText)
        pauseButtonText = pauseButton.findViewById(R.id.pauseButtonText)

        startButton.isEnabled = true
        pauseButton.isEnabled = false
    }

    private fun setupListeners() {
        startButton.setOnClickListener { startCountdown() }
        pauseButton.setOnClickListener { pauseCountdown() }
        resetButton.setOnClickListener { resetCountdown() }

        findViewById<LinearLayout>(R.id.backButton).setOnClickListener { finish() }
    }

    private fun startCountdown() {
        val rawLabel = labelInput.text.toString().ifEmpty { getString(R.string.countdown_default_label) }
        val label = rawLabel.take(20)
        val minutes = minuteInput.text.toString().toIntOrNull() ?: 0
        val seconds = secondInput.text.toString().toIntOrNull() ?: 0
        val totalSeconds = minutes * 60 + seconds

        if (minutes < 0 || seconds < 0 || seconds > 59) {
            Toast.makeText(this, getString(R.string.countdown_invalid_time), Toast.LENGTH_SHORT).show()
            return
        }

        if (totalSeconds <= 0) {
            Toast.makeText(this, getString(R.string.countdown_invalid_time), Toast.LENGTH_SHORT).show()
            return
        }

        if (totalSeconds > MAX_COUNTDOWN_SECONDS) {
            Toast.makeText(this, getString(R.string.countdown_too_long), Toast.LENGTH_SHORT).show()
            return
        }

        remainingMillis = totalSeconds * 1000L

        startButton.isEnabled = false
        pauseButton.isEnabled = false

        lifecycleScope.launch {
            val countdown = countdownService.createCountdown(label, totalSeconds)
            currentCountdownId = countdown.id
            isRunning = true
            pauseButton.isEnabled = true
            startButtonText.text = getString(R.string.countdown_running)
            startCountDownTimer(remainingMillis)
        }
    }

    private fun startCountDownTimer(millis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateDisplay()
            }

            override fun onFinish() {
                onCountdownComplete()
            }
        }.start()
    }

    private fun pauseCountdown() {
        if (isRunning) {
            countDownTimer?.cancel()
            isRunning = false
            pauseButtonText.text = getString(R.string.countdown_resume)

            currentCountdownId?.let { id ->
                lifecycleScope.launch {
                    countdownService.pauseCountdown(id)
                }
            }
        } else {
            isRunning = true
            pauseButtonText.text = getString(R.string.countdown_pause)

            startCountDownTimer(remainingMillis)

            currentCountdownId?.let { id ->
                lifecycleScope.launch {
                    countdownService.resumeCountdown(id)
                }
            }
        }
    }

    private fun resetCountdown() {
        countDownTimer?.cancel()
        isRunning = false
        remainingMillis = 0

        val idToDelete = currentCountdownId
        currentCountdownId = null

        countdownText.text = getString(R.string.countdown_zero_time)
        startButton.isEnabled = true
        pauseButton.isEnabled = false
        startButtonText.text = getString(R.string.countdown_start)
        pauseButtonText.text = getString(R.string.countdown_pause)

        repository.setConfig("countdown_bg_millis", 0)
        repository.setConfig("countdown_bg_id", -1L)

        idToDelete?.let { id ->
            lifecycleScope.launch {
                countdownService.deleteCountdown(id)
            }
        }
    }

    private fun updateDisplay() {
        val totalSeconds = remainingMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        countdownText.text = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun onCountdownComplete() {
        isRunning = false
        countdownText.text = getString(R.string.countdown_zero_time)
        startButton.isEnabled = true
        pauseButton.isEnabled = false

        val label = labelInput.text.toString().ifEmpty { getString(R.string.countdown_default_label) }

        Toast.makeText(this, getString(R.string.countdown_complete_msg, label), Toast.LENGTH_LONG).show()

        val completedId = currentCountdownId
        currentCountdownId = null

        completedId?.let { id ->
            lifecycleScope.launch {
                countdownService.completeCountdown(id)
            }
            notificationService.showCountdownNotification(
                Countdown(id = id, label = label, targetTime = java.time.LocalDateTime.now())
            )
        }
    }

    override fun onPause() {
        super.onPause()
        saveCountdownState()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (isRunning && remainingMillis > 0) {
            saveCountdownState()
        }
    }

    private fun saveCountdownState() {
        if (remainingMillis > 0) {
            repository.setConfig("countdown_bg_millis", remainingMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            repository.setConfig("countdown_bg_running", isRunning)
            currentCountdownId?.let { repository.setConfig("countdown_bg_id", it) }
        }
    }

    private fun restoreCountdownState(): Boolean {
        val bgMillis = (repository.getConfig("countdown_bg_millis", 0) as? Number)?.toLong() ?: 0L
        val bgRunning = repository.getConfig("countdown_bg_running", false) as? Boolean ?: false
        val bgId = (repository.getConfig("countdown_bg_id", -1L) as? Number)?.toLong() ?: -1L

        if (bgMillis > 0 && bgId > 0) {
            remainingMillis = bgMillis
            currentCountdownId = bgId
            isRunning = bgRunning
            updateDisplay()

            if (bgRunning) {
                startCountDownTimer(bgMillis)
                startButton.isEnabled = false
                pauseButton.isEnabled = true
                startButtonText.text = getString(R.string.countdown_running)
            } else {
                startButton.isEnabled = true
                pauseButton.isEnabled = true
                startButtonText.text = getString(R.string.countdown_start)
                pauseButtonText.text = getString(R.string.countdown_resume)
            }

            repository.setConfig("countdown_bg_millis", 0)
            return true
        }
        return false
    }
}
