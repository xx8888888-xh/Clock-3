package com.clock3.pet.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.clock3.pet.R
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.ui.MainActivity
import kotlinx.coroutines.*
import kotlin.math.abs

class FloatingPetService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var petImageView: ImageView
    private lateinit var petStatusView: TextView

    private val repository by lazy { Clock3Repository(this) }
    private val notificationService by lazy { NotificationService.getInstance(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var isDragging = false
    private var lastTouchTime = 0L
    private var touchCount = 0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingView()
        createNotification()
    }

    private fun createFloatingView() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_pet, null)

        petImageView = floatingView.findViewById(R.id.petImage)
        petStatusView = floatingView.findViewById(R.id.petStatus)

        val petSize = (repository.getConfig("pet_size", 100) as Int).toFloat()
        val opacity = (repository.getConfig("pet_opacity", 1.0f) as Float)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (repository.getConfig("pet_x", 800) as Int)
            y = (repository.getConfig("pet_y", 400) as Int)
            width = petSize.toInt().dpToPx()
            height = petSize.toInt().dpToPx()
        }

        floatingView.alpha = opacity
        floatingView.setOnTouchListener { _, event ->
            handleTouch(event, layoutParams)
        }

        floatingView.setOnClickListener {
            handleClick()
        }

        floatingView.setOnLongClickListener {
            handleLongClick()
            true
        }

        windowManager.addView(floatingView, layoutParams)

        updatePetStatus()
    }

    private fun handleTouch(event: MotionEvent, layoutParams: WindowManager.LayoutParams): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (!isDragging && (abs(deltaX) > 10 || abs(deltaY) > 10)) {
                    isDragging = true
                }

                if (isDragging) {
                    layoutParams.x = initialX + deltaX.toInt()
                    layoutParams.y = initialY - deltaY.toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    repository.setConfig("pet_x", layoutParams.x)
                    repository.setConfig("pet_y", layoutParams.y)
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTouchTime < 300) {
                    touchCount++
                } else {
                    touchCount = 1
                }
                lastTouchTime = currentTime

                if (!isDragging && touchCount == 1) {
                    floatingView.postDelayed({
                        if (touchCount == 1) {
                            showMainMenu()
                        }
                    }, 300)
                } else if (!isDragging && touchCount == 2) {
                    openAlarms()
                } else if (!isDragging && touchCount == 3) {
                    openCountdown()
                }

                isDragging = false
                return true
            }
        }
        return false
    }

    private fun handleClick() {
        showMainMenu()
    }

    private fun handleLongClick() {
        val popup = PopupMenu(this, floatingView)
        popup.menu.add(0, 1, 0, "⏰ 闹钟列表")
        popup.menu.add(0, 2, 1, "⏱️ 倒计时")
        popup.menu.add(0, 3, 2, "🐶 宠物状态")
        popup.menu.add(0, 4, 3, "⚙️ 设置")
        popup.menu.add(0, 5, 4, "❌ 退出")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> openAlarms()
                2 -> openCountdown()
                3 -> openPetStatus()
                4 -> openSettings()
                5 -> stopSelf()
            }
            true
        }

        popup.show()
    }

    private fun showMainMenu() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("action", "main_menu")
        }
        startActivity(intent)
    }

    private fun openAlarms() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("action", "alarms")
        }
        startActivity(intent)
    }

    private fun openCountdown() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("action", "countdown")
        }
        startActivity(intent)
    }

    private fun openPetStatus() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("action", "pet_status")
        }
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("action", "settings")
        }
        startActivity(intent)
    }

    private fun updatePetStatus() {
        scope.launch {
            val pet = repository.getPetSync()
            petStatusView.text = pet.mood.uppercase()
        }
    }

    private fun createNotification() {
        notificationService.showServiceNotification()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        notificationService.cancelNotification(NotificationService.SERVICE_NOTIFICATION_ID)
        scope.cancel()
    }
}
