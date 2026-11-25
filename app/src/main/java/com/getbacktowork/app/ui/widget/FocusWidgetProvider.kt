package com.getbacktowork.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.getbacktowork.app.R
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.MainActivity
import com.getbacktowork.app.ui.overlay.BlockingOverlayActivity

/**
 * Glanceable Android Home Screen Widget:
 * Shows current daily streak, focus/screen time budget, and provides a 1-tap 25m Deep Focus launch.
 */
class FocusWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_START_QUICK_FOCUS = "com.getbacktowork.focus.ACTION_WIDGET_START_25M"

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, FocusWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateAppWidget(context, manager, id)
                }
            }
        }

        private fun updateAppWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val sessionManager = FocusSessionManager.getInstance(context)
            val today = sessionManager.getTodayDiaryEntry()
            val goal = sessionManager.getDailyScreenGoalMinutes()
            val streak = sessionManager.getCurrentStreakDays()

            val views = RemoteViews(context.packageName, R.layout.focus_widget)
            views.setTextViewText(R.id.widget_streak_text, "${streak}d streak")
            views.setTextViewText(R.id.widget_screen_stat, "Screen ${today.screenMinutes}m / ${goal}m")
            views.setTextViewText(R.id.widget_focus_stat, "Focus ${today.focusedMinutes}m")

            val status = when {
                sessionManager.isUltraFocusActive() -> "Cold Turkey Locked"
                sessionManager.isSessionActive() -> {
                    val mins = (sessionManager.getRemainingMillis() / 60_000L).coerceAtLeast(1)
                    "Focusing · ${mins}m left"
                }
                sessionManager.isScheduledFocusActiveNow() -> "Scheduled Focus Active"
                else -> "Ready · Stay under ${goal}m"
            }
            views.setTextViewText(R.id.widget_status_text, status)

            val ctaLabel = if (sessionManager.shouldBlockApps()) "Open Launcher" else "25m Focus"
            views.setTextViewText(R.id.widget_action_btn, ctaLabel)

            val rootIntent = Intent(context, MainActivity::class.java)
            val rootPending = PendingIntent.getActivity(
                context,
                10,
                rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, rootPending)

            val actionIntent = Intent(context, FocusWidgetProvider::class.java).apply {
                action = ACTION_START_QUICK_FOCUS
            }
            val actionPending = PendingIntent.getBroadcast(
                context,
                11,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_btn, actionPending)

            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_START_QUICK_FOCUS) {
            val sessionManager = FocusSessionManager.getInstance(context)
            if (!sessionManager.shouldBlockApps()) {
                sessionManager.startSession(25, strictMode = true)
            }
            val overlayIntent = Intent(context, BlockingOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(overlayIntent)
            refreshAllWidgets(context)
        }
    }
}
