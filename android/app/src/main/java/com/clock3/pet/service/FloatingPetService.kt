package com.clock3.pet.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.clock3.pet.R
import com.clock3.pet.data.repository.Clock3Repository
import com.clock3.pet.ui.MainActivityNew
import com.clock3.pet.ui.PomodoroActivity
import com.clock3.pet.ui.ShopActivity
import com.clock3.pet.ui.AlarmActivity
import com.clock3.pet.ui.SettingsActivity
import com.clock3.pet.utils.AppLog
import kotlinx.coroutines.*
import kotlin.math.abs

data class PomodoroState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isBreak: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0
)

class FloatingPetService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var petImageView: ImageView
    private lateinit var pomodoroStatusLayout: LinearLayout
    private lateinit var pomodoroStatusText: TextView
    private lateinit var pomodoroTimeText: TextView

    private var menuView: View? = null
    @Volatile
    private var isMenuShowing = false

    private val repository by lazy { Clock3Repository(applicationContext) }
    private val notificationService by lazy { NotificationService.getInstance(applicationContext) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var isDragging = false
    private var lastTouchTime = 0L
    private var pendingClickRunnable: Runnable? = null

    private val updateRunnable = Runnable {
        updateSettings()
    }

    private fun startUpdateLoop() {
        scope.launch {
            while (isActive) {
                updatePomodoroStatus()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_POMODORO = "com.clock3.pet.UPDATE_POMODORO"
        const val ACTION_UPDATE_SETTINGS = "com.clock3.pet.UPDATE_SETTINGS"
        const val UPDATE_INTERVAL_MS = 1000L
        const val DOUBLE_CLICK_THRESHOLD_MS = 300L
        const val DRAG_THRESHOLD_DP = 10
        const val MENU_OFFSET_X = 200
        const val MENU_OFFSET_Y = 10
        const val DEFAULT_PET_X = 800
        const val DEFAULT_PET_Y = 400
        const val DEFAULT_PET_SIZE = 100
        const val DEFAULT_PET_OPACITY = 1.0f
        const val INITIAL_DELAY_MS = 500L

        @Volatile
        private var pomodoroState = PomodoroState()

        @Volatile
        private var INSTANCE: FloatingPetService? = null

        fun updatePomodoroState(
            context: Context,
            isRunning: Boolean,
            isPaused: Boolean,
            isBreak: Boolean,
            remainingSeconds: Int,
            totalSeconds: Int
        ) {
            pomodoroState = PomodoroState(
                isRunning = isRunning,
                isPaused = isPaused,
                isBreak = isBreak,
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds
            )

            val intent = Intent(context, FloatingPetService::class.java).apply {
                action = ACTION_UPDATE_POMODORO
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifySettingsChanged() {
            INSTANCE?.let { service ->
                if (service.isSafeToPost()) {
                    service.floatingView.post(service.updateRunnable)
                }
            }
        }
    }

    private fun isSafeToPost(): Boolean {
        return ::floatingView.isInitialized && ::windowManager.isInitialized && floatingView.isAttachedToWindow
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (!Settings.canDrawOverlays(this)) {
            AppLog.w("FloatingPet", "Overlay permission not granted, cannot create floating view")
            stopSelf()
            return
        }
        createFloatingView()
        createNotification()
        INSTANCE = this
        scope.launch {
            delay(INITIAL_DELAY_MS)
            startUpdateLoop()
        }
    }

    private fun createFloatingView() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_pet, null)

        petImageView = floatingView.findViewById(R.id.petImage)
        pomodoroStatusLayout = floatingView.findViewById(R.id.pomodoroStatusLayout)
        pomodoroStatusText = floatingView.findViewById(R.id.pomodoroStatusText)
        pomodoroTimeText = floatingView.findViewById(R.id.pomodoroTimeText)

        val petSizePercent = ((repository.getConfig("pet_size", DEFAULT_PET_SIZE) as? Int) ?: DEFAULT_PET_SIZE).toFloat() / 100f
        val opacity = (repository.getConfig("pet_opacity", DEFAULT_PET_OPACITY) as? Float) ?: DEFAULT_PET_OPACITY

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (repository.getConfig("pet_x", DEFAULT_PET_X) as? Int) ?: DEFAULT_PET_X
            y = (repository.getConfig("pet_y", DEFAULT_PET_Y) as? Int) ?: DEFAULT_PET_Y
        }

        floatingView.alpha = opacity
        applyPetSize(petSizePercent)
        floatingView.setOnTouchListener { _, event ->
            handleTouch(event, layoutParams)
        }

        floatingView.setOnLongClickListener {
            handleLongClick()
            true
        }

        try {
            windowManager.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            AppLog.e("FloatingPet", "addView floatingView failed", e)
            return
        }
        updatePetStatus()
    }

    private fun applyPetSize(percent: Float) {
        val container = floatingView as? LinearLayout
        val frameLayout = container?.getChildAt(0) as? android.widget.FrameLayout
        val imageView = petImageView

        val baseFrameSize = 64.dpToPx()
        val baseImageSize = 52.dpToPx()

        frameLayout?.layoutParams?.let { params ->
            params.width = (baseFrameSize * percent).toInt()
            params.height = (baseFrameSize * percent).toInt()
            frameLayout.layoutParams = params
        }

        imageView.layoutParams?.let { params ->
            params.width = (baseImageSize * percent).toInt()
            params.height = (baseImageSize * percent).toInt()
            imageView.layoutParams = params
        }
    }

    private fun handleTouch(event: MotionEvent, layoutParams: WindowManager.LayoutParams): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pendingClickRunnable?.let { floatingView.removeCallbacks(it) }
                pendingClickRunnable = null

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

                val dragThresholdPx = (DRAG_THRESHOLD_DP * resources.displayMetrics.density).toInt()
                if (!isDragging && (abs(deltaX) > dragThresholdPx || abs(deltaY) > dragThresholdPx)) {
                    isDragging = true
                    pendingClickRunnable?.let { floatingView.removeCallbacks(it) }
                    pendingClickRunnable = null
                    hideMenu()
                }

                if (isDragging) {
                    layoutParams.x = initialX + deltaX.toInt()
                    layoutParams.y = initialY + deltaY.toInt()
                    try {
                        windowManager.updateViewLayout(floatingView, layoutParams)
                    } catch (e: Exception) {
                        AppLog.w("FloatingPet", "updateViewLayout failed during drag", e)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    repository.setConfig("pet_x", layoutParams.x)
                    repository.setConfig("pet_y", layoutParams.y)
                    isDragging = false
                    return true
                }

                val currentTime = System.currentTimeMillis()
                val isDoubleClick = (currentTime - lastTouchTime) < DOUBLE_CLICK_THRESHOLD_MS
                lastTouchTime = currentTime

                if (isDoubleClick) {
                    pendingClickRunnable?.let { floatingView.removeCallbacks(it) }
                    pendingClickRunnable = null
                    openPomodoro()
                } else {
                    val runnable = object : Runnable {
                        override fun run() {
                            if (pendingClickRunnable === this) {
                                toggleMenu()
                            }
                            pendingClickRunnable = null
                        }
                    }
                    pendingClickRunnable = runnable
                    floatingView.postDelayed(runnable, DOUBLE_CLICK_THRESHOLD_MS)
                }

                isDragging = false
                return true
            }
        }
        return false
    }

    private fun handleLongClick() {
        pendingClickRunnable?.let { floatingView.removeCallbacks(it) }
        pendingClickRunnable = null
        hideMenu()
        val intent = Intent(this, MainActivityNew::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun toggleMenu() {
        if (isMenuShowing) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        if (isMenuShowing) return
        if (!::floatingView.isInitialized) return

        try {
            if (!floatingView.isAttachedToWindow) return

            val themedContext = ContextThemeWrapper(this, R.style.Theme_Clock3)
            val inflater = LayoutInflater.from(themedContext)
            val menu = inflater.inflate(R.layout.floating_menu, null)

            val location = IntArray(2)
            floatingView.getLocationOnScreen(location)

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val statusBarHeight = getStatusBarHeight()

            menu.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            )
            val menuWidth = menu.measuredWidth
            val menuHeight = menu.measuredHeight

            var menuX = location[0] - MENU_OFFSET_X
            var menuY = location[1] + floatingView.height + MENU_OFFSET_Y

            if (menuX < 0) menuX = MENU_OFFSET_X
            if (menuX + menuWidth > screenWidth) {
                menuX = screenWidth - menuWidth - MENU_OFFSET_X
            }

            if (menuY < statusBarHeight) menuY = statusBarHeight + MENU_OFFSET_Y
            if (menuY + menuHeight > screenHeight) {
                menuY = location[1] - menuHeight
                if (menuY < statusBarHeight) menuY = statusBarHeight + MENU_OFFSET_Y
            }

            val menuLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = menuX
                y = menuY
            }

            menu.findViewById<LinearLayout>(R.id.menuStartPomodoro)?.setOnClickListener {
                hideMenu()
                startPomodoro()
            }

            menu.findViewById<LinearLayout>(R.id.menuViewPomodoro)?.setOnClickListener {
                hideMenu()
                openPomodoro()
            }

            menu.findViewById<LinearLayout>(R.id.menuSetAlarm)?.setOnClickListener {
                hideMenu()
                openAlarms()
            }

            menu.findViewById<LinearLayout>(R.id.menuShop)?.setOnClickListener {
                hideMenu()
                openShop()
            }

            menu.findViewById<LinearLayout>(R.id.menuHome)?.setOnClickListener {
                hideMenu()
                openMainActivity()
            }

            menu.findViewById<LinearLayout>(R.id.menuSettings)?.setOnClickListener {
                hideMenu()
                openSettings()
            }

            synchronized(this) {
                if (isMenuShowing) return
                windowManager.addView(menu, menuLayoutParams)
                menuView = menu
                isMenuShowing = true
            }
        } catch (e: Exception) {
            AppLog.e("FloatingPet", "showMenu failed", e)
            menuView = null
            isMenuShowing = false
        }
    }

    private fun hideMenu() {
        val view: View?
        synchronized(this) {
            view = menuView
            menuView = null
            isMenuShowing = false
        }
        if (view != null && view.isAttachedToWindow) {
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                AppLog.w("FloatingPet", "hideMenu removeView failed", e)
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun startPomodoro() {
        val intent = Intent(this, PomodoroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("auto_start", true)
        }
        startActivity(intent)
    }

    private fun openPomodoro() {
        val intent = Intent(this, PomodoroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun openAlarms() {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun openShop() {
        val intent = Intent(this, ShopActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivityNew::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun updatePomodoroStatus() {
        if (!::pomodoroStatusLayout.isInitialized) return

        val state = pomodoroState

        if (state.isRunning) {
            pomodoroStatusLayout.visibility = View.VISIBLE

            if (state.isPaused) {
                pomodoroStatusText.text = getString(R.string.floating_pet_paused)
            } else if (state.isBreak) {
                pomodoroStatusText.text = getString(R.string.floating_pet_break)
            } else {
                pomodoroStatusText.text = getString(R.string.floating_pet_focusing)
            }

            val minutes = state.remainingSeconds / 60
            val seconds = state.remainingSeconds % 60
            pomodoroTimeText.text = String.format("%02d:%02d", minutes, seconds)
        } else {
            pomodoroStatusLayout.visibility = View.GONE
        }
    }

    private fun updateSettings() {
        if (!::floatingView.isInitialized) return

        val petSizePercent = ((repository.getConfig("pet_size", DEFAULT_PET_SIZE) as? Int) ?: DEFAULT_PET_SIZE).toFloat() / 100f
        val opacity = (repository.getConfig("pet_opacity", DEFAULT_PET_OPACITY) as? Float) ?: DEFAULT_PET_OPACITY

        floatingView.alpha = opacity
        applyPetSize(petSizePercent)
    }

    private fun updatePetStatus() {
        scope.launch {
            if (!isActive) return@launch
            val pet = repository.getPetSync()
            if (!isActive) return@launch
            petImageView.contentDescription = pet.name
        }
    }

    private fun createNotification() {
        val notification = NotificationCompat.Builder(this, NotificationService.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_floating_ball)
            .setContentTitle(getString(R.string.notification_service_title))
            .setContentText(getString(R.string.notification_service_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            startForeground(NotificationService.SERVICE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            AppLog.e("FloatingPet", "startForeground failed", e)
            stopSelf()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_POMODORO) {
            updatePomodoroStatus()
        } else if (intent?.action == ACTION_UPDATE_SETTINGS) {
            updateSettings()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
        hideMenu()
        if (::floatingView.isInitialized) {
            floatingView.removeCallbacks(updateRunnable)
            pendingClickRunnable?.let { floatingView.removeCallbacks(it) }
            pendingClickRunnable = null
            if (floatingView.isAttachedToWindow) {
                try {
                    windowManager.removeView(floatingView)
                } catch (e: Exception) {
                    AppLog.w("FloatingPet", "onDestroy removeView failed", e)
                }
            }
        }
        notificationService.cancelNotification(NotificationService.SERVICE_NOTIFICATION_ID)
        scope.cancel()
    }
}
