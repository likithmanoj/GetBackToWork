package com.getbacktowork.app.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Compact, draggable floating counter pill displayed while watching short-form video
 * (Reels, Shorts, TikTok) in Holiday or Travel mode so the user retains mindful awareness
 * without blocking playback abruptly.
 */
class ReelsFloatingCounterOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var containerView: LinearLayout? = null
    private var dotView: View? = null
    private var countTextView: TextView? = null
    private var labelTextView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isAdded = false

    fun show(count: Int, limit: Int, modeLabel: String) {
        val primary = if (limit > 0) "$count/$limit" else "$count"
        val badge = if (modeLabel == "REELS" || modeLabel == "DAILY") "REELS" else modeLabel
        val nearLimit = limit > 0 && count >= (limit * 0.8f).toInt().coerceAtLeast(1)
        showInternal(primary, badge, nearLimit)
    }

    fun showMinutes(minutes: Int, limitMinutes: Int, modeLabel: String) {
        val primary = if (limitMinutes > 0) "$minutes/${limitMinutes}m" else "${minutes}m"
        val badge = if (modeLabel == "DAILY" || modeLabel == "VIDEO") "VIDEO" else modeLabel
        val nearLimit = limitMinutes > 0 && minutes >= (limitMinutes * 0.8f).toInt().coerceAtLeast(1)
        showInternal(primary, badge, nearLimit)
    }

    private fun showInternal(primaryText: String, badgeLabel: String, nearLimit: Boolean) {
        mainHandler.post {
            if (!Settings.canDrawOverlays(context) || windowManager == null) {
                hideInternal()
                return@post
            }
            ensureView()
            updateContent(primaryText, badgeLabel, nearLimit)

            val view = containerView ?: return@post
            val params = layoutParams ?: return@post
            if (!isAdded) {
                try {
                    windowManager.addView(view, params)
                    isAdded = true
                } catch (_: Exception) {
                    isAdded = false
                }
            }
        }
    }

    fun hide() {
        mainHandler.post {
            hideInternal()
        }
    }

    fun destroy() {
        mainHandler.post {
            hideInternal()
            containerView = null
            dotView = null
            countTextView = null
            labelTextView = null
            layoutParams = null
        }
    }

    private fun hideInternal() {
        val view = containerView
        if (isAdded && view != null && windowManager != null) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        isAdded = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureView() {
        if (containerView != null) return

        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels

        fun dp(value: Int): Int = (value * density).toInt()

        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24).toFloat()
            setColor(Color.parseColor("#EB111317"))
            setStroke(dp(1), Color.parseColor("#3DFFFFFF"))
        }

        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#FFF59E0B"))
        }

        val dot = View(context).apply {
            background = dotDrawable
            layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                marginEnd = dp(8)
            }
        }

        val countTv = TextView(context).apply {
            setTextColor(Color.parseColor("#FFF4F4F5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            fontFeatureSettings = "tnum"
            maxLines = 1
        }

        val labelTv = TextView(context).apply {
            setTextColor(Color.parseColor("#FFA1A1AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.08f
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(6)
            }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pillBackground
            setPadding(dp(12), dp(7), dp(12), dp(7))
            elevation = dp(6).toFloat()
            addView(dot)
            addView(countTv)
            addView(labelTv)
        }

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - dp(148)).coerceAtLeast(dp(14))
            y = dp(98)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    if (isAdded) {
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                else -> false
            }
        }

        containerView = container
        dotView = dot
        countTextView = countTv
        labelTextView = labelTv
        layoutParams = params
    }

    private fun updateContent(primaryText: String, badgeLabel: String, nearLimit: Boolean) {
        val dotColor = if (nearLimit) "#FFFB7185" else "#FF34D399"
        val dotDrawable = dotView?.background as? GradientDrawable
        dotDrawable?.setColor(Color.parseColor(dotColor))

        val pillDrawable = containerView?.background as? GradientDrawable
        pillDrawable?.setStroke(
            (context.resources.displayMetrics.density * 1f).toInt().coerceAtLeast(1),
            Color.parseColor(if (nearLimit) "#66FB7185" else "#3DFFFFFF")
        )

        countTextView?.text = primaryText
        labelTextView?.text = badgeLabel
    }
}
