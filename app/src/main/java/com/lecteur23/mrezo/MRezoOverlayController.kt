package com.lecteur23.mrezo

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

object MRezoOverlayController {
    private const val MAX_OVERLAY_DURATION_MS = 40_000L
    private const val PREFS_NAME = "mrezo_security"
    private const val KEY_USSD_STEPS = "ussd_steps"
    private const val KEY_USSD_STEP_INDEX = "ussd_step_index"
    private const val KEY_USSD_RUNNING = "ussd_running"
    private const val KEY_USSD_RESULT_MESSAGE = "ussd_result_message"
    private const val KEY_USSD_RESULT_STATUS = "ussd_result_status"
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var overlayContext: Context? = null
    private var overlayIsAccessibility = false
    private var safetyHideRunnable: Runnable? = null

    fun canDraw(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    fun show(context: Context) {
        showInternal(context = context, useAccessibilityOverlay = false)
    }

    fun showFromAccessibilityService(service: AccessibilityService) {
        showInternal(context = service, useAccessibilityOverlay = true)
    }

    private fun showInternal(context: Context, useAccessibilityOverlay: Boolean) {
        if (!useAccessibilityOverlay && !canDraw(context)) return
        if (overlayView != null && overlayIsAccessibility == useAccessibilityOverlay) return
        if (overlayView != null && useAccessibilityOverlay && !overlayIsAccessibility) {
            removeOverlay(context)
        }
        if (overlayView != null) return

        val appContext = context.applicationContext
        val windowContext = if (useAccessibilityOverlay) context else appContext
        val windowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val root = FrameLayout(appContext).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(0, 24, 124), Color.rgb(0, 11, 56), Color.rgb(3, 8, 18))
            )
            alpha = 1f
            isClickable = false
            isFocusable = false
            systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        val content = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val ring = FrameLayout(appContext).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(34, 255, 255, 255))
                setStroke(10, Color.rgb(255, 204, 0))
            }
        }

        val progress = ProgressBar(appContext).apply {
            isIndeterminate = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                indeterminateTintList = ColorStateList.valueOf(Color.rgb(255, 204, 0))
            }
        }
        ring.addView(
            progress,
            FrameLayout.LayoutParams(208, 208, Gravity.CENTER)
        )

        val logoHolder = FrameLayout(appContext).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(3, 8, 18))
                setStroke(2, Color.argb(80, 255, 255, 255))
            }
        }
        val logo = ImageView(appContext).apply {
            setImageResource(R.drawable.app_icon)
            adjustViewBounds = true
        }
        logoHolder.addView(
            logo,
            FrameLayout.LayoutParams(112, 112, Gravity.CENTER)
        )
        ring.addView(
            logoHolder,
            FrameLayout.LayoutParams(140, 140, Gravity.CENTER)
        )

        content.addView(
            ring,
            LinearLayout.LayoutParams(230, 230).apply { bottomMargin = 34 }
        )

        val title = TextView(appContext).apply {
            text = "Traitement en cours"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        content.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(appContext).apply {
            text = "M-REZO execute l'operation"
            setTextColor(Color.rgb(156, 168, 190))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }
        content.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val badge = TextView(appContext).apply {
            text = "Veuillez patienter"
            setTextColor(Color.rgb(8, 17, 31))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.rgb(255, 204, 0))
            }
            setPadding(28, 12, 28, 12)
        }
        content.addView(
            badge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 28 }
        )

        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val type = when {
            useAccessibilityOverlay -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        }
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            android.graphics.PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = 1f
            dimAmount = 1f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(root, params)
            overlayView = root
            overlayContext = windowContext
            overlayIsAccessibility = useAccessibilityOverlay
            safetyHideRunnable?.let(handler::removeCallbacks)
            safetyHideRunnable = Runnable { forceCloseTimedOutSession(appContext) }.also {
                handler.postDelayed(it, MAX_OVERLAY_DURATION_MS)
            }
        } catch (_: Exception) {
            overlayView = null
        }
    }

    fun isShowing(): Boolean = overlayView != null

    fun hide(context: Context) {
        safetyHideRunnable?.let(handler::removeCallbacks)
        safetyHideRunnable = null
        removeOverlay(context)
    }

    private fun forceCloseTimedOutSession(context: Context) {
        safetyHideRunnable = null
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USSD_RUNNING, false)
            .putString(KEY_USSD_RESULT_MESSAGE, "Operation arretee automatiquement : delai de 40 secondes depasse.")
            .putString(KEY_USSD_RESULT_STATUS, "error")
            .remove(KEY_USSD_STEPS)
            .remove(KEY_USSD_STEP_INDEX)
            .apply()
        removeOverlay(context)
    }

    private fun removeOverlay(context: Context) {
        val view = overlayView ?: return
        val windowContext = overlayContext ?: context.applicationContext
        val windowManager = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
            // Overlay can already be gone if Android kills or recreates the window.
        } finally {
            overlayView = null
            overlayContext = null
            overlayIsAccessibility = false
        }
    }
}