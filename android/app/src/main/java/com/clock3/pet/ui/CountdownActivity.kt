package com.clock3.pet.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clock3.pet.R
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.service.CountdownService
import com.clock3.pet.service.NotificationService
import kotlinx.coroutines.launch

class CountdownActivity : AppCompatActivity() {
    private lateinit var repository: Clock3Repository
    private lateinit var countdownService: CountdownService
    private lateinit var notificationService: NotificationService

    private lateinit var countdownText: TextView
    private lateinit var labelInput: EditText
    private lateinit var minuteInput: EditText
    private lateinit var secondInput: EditText
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button

    private var countDownTimer: CountDownTimer? = null
    private var currentCountdownId: Long? = null
    private var remainingMillis: Long = 0
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        repository = Clock3Repository(this)
        countdownService = CountdownService.getInstance(this)
        notificationService = NotificationService.getInstance(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        countdownText = findViewById(R.id.countdownText)
        labelInput = findViewById(R.id.labelInput)
        minuteInput = findViewById(R.id.minuteInput)
        secondInput = findViewById(R.id.secondInput)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        resetButton = findViewById(R.id.resetButton)

        pauseButton.isEnabled = false
    }

    private fun setupListeners() {
        startButton.setOnClickListener { startCountdown() }
        pauseButton.setOnClickListener { pauseCountdown() }
        resetButton.setOnClickListener { resetCountdown() }

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }
    }

    private fun startCountdown() {
        val label = labelInput.text.toString().ifEmpty { "倒计时" }
        val minutes = minuteInput.text.toString().toIntOrNull() ?: 0
        val seconds = secondInput.text.toString().toIntOrNull() ?: 0
        val totalSeconds = minutes * 60 + seconds

        if (totalSeconds <= 0) {
            Toast.makeText(this, "请输入有效的时间", Toast.LENGTH_SHORT).show()
            return
        }

        remainingMillis = totalSeconds * 1000L

        lifecycleScope.launch {
            val countdown = countdownService.createCountdown(label, totalSeconds)
            currentCountdownId = countdown.id
        }

        isRunning = true
        startButton.isEnabled = false
        pauseButton.isEnabled = true

        countDownTimer = object : CountDownTimer(remainingMillis, 100) {
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
            pauseButton.text = "▶️ 继续"

            currentCountdownId?.let { id ->
                lifecycleScope.launch {
                    countdownService.pauseCountdown(id)
                }
            }
        } else {
            isRunning = true
            pauseButton.text = "⏸️ 暂停"

            countDownTimer = object : CountDownTimer(remainingMillis, 100) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingMillis = millisUntilFinished
                    updateDisplay()
                }

                override fun onFinish() {
                    onCountdownComplete()
                }
            }.start()

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
        currentCountdownId = null

        countdownText.text = "00:00"
        startButton.isEnabled = true
        pauseButton.isEnabled = false
        pauseButton.text = "⏸️ 暂停"

        currentCountdownId?.let { id ->
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
        countdownText.text = "00:00"
        startButton.isEnabled = true
        pauseButton.isEnabled = false

        val label = labelInput.text.toString().ifEmpty { "倒计时" }

        Toast.makeText(this, "🎉 $label 完成!", Toast.LENGTH_LONG).show()

        notificationService.showCountdownNotification(
            com.clock3.pet.data.model.Countdown(
                id = currentCountdownId ?: 0,
                label = label,
                targetTime = java.time.LocalDateTime.now()
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
