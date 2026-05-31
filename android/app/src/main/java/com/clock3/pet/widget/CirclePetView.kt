package com.clock3.pet.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.clock3.pet.R
import java.util.concurrent.ConcurrentHashMap

class CirclePetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Mood {
        HAPPY,
        FOCUSED,
        RESTING,
        EXCITED,
        HUNGRY,
        BORED
    }

    private var currentMood = Mood.HAPPY
    private var petBitmap: Bitmap? = null
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val moodToDrawableMap = mapOf(
        Mood.HAPPY to R.drawable.pet_happy,
        Mood.FOCUSED to R.drawable.pet_focused,
        Mood.RESTING to R.drawable.pet_resting,
        Mood.EXCITED to R.drawable.pet_excited,
        Mood.HUNGRY to R.drawable.pet_hungry,
        Mood.BORED to R.drawable.pet_bored
    )

    private val srcRect = Rect()
    private val dstRect = RectF()

    init {
        loadBitmapForMood(currentMood)
    }

    fun setMood(mood: Mood) {
        if (currentMood != mood) {
            currentMood = mood
            loadBitmapForMood(mood)
            invalidate()
        }
    }

    fun getMood(): Mood = currentMood

    private fun loadBitmapForMood(mood: Mood) {
        val cached = sharedBitmapCache[mood]
        if (cached != null && !cached.isRecycled) {
            petBitmap = cached
            return
        }

        val resId = moodToDrawableMap[mood] ?: R.drawable.pet_happy
        val appContext = context.applicationContext

        sharedBitmapCache.compute(mood) { _, existing ->
            if (existing != null && !existing.isRecycled) return@compute existing
            existing?.recycle()
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(appContext.resources, resId, options)
            options.inSampleSize = calculateInSampleSize(options, DEFAULT_DESIRED_SIZE, DEFAULT_DESIRED_SIZE)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeResource(appContext.resources, resId, options)
        }?.let {
            if (!it.isRecycled) {
                petBitmap = it
            } else {
                sharedBitmapCache.remove(mood)
            }
        }
    }

    companion object {
        private const val DEFAULT_DESIRED_SIZE = 300
        private val sharedBitmapCache = ConcurrentHashMap<Mood, Bitmap>()

        fun clearSharedCache() {
            sharedBitmapCache.forEach { _, bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            sharedBitmapCache.clear()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = DEFAULT_DESIRED_SIZE
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredSize, widthSize)
            else -> desiredSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredSize, heightSize)
            else -> desiredSize
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bitmap = petBitmap ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val padding = viewWidth * 0.05f
        val drawWidth = viewWidth - padding * 2
        val drawHeight = viewHeight - padding * 2

        srcRect.set(0, 0, bitmap.width, bitmap.height)
        dstRect.set(padding, padding, padding + drawWidth, padding + drawHeight)

        canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        petBitmap = null
    }
}
