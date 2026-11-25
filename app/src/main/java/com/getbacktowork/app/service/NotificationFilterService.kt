package com.getbacktowork.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getbacktowork.app.data.FocusSessionManager

class NotificationFilterService : NotificationListenerService() {

    private lateinit var sessionManager: FocusSessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // If focus blocking is not active (or user is in an Emergency Pass / Travel window), allow all notifications
        if (!sessionManager.shouldBlockApps()) return

        val packageName = sbn.packageName ?: return

        // Always allow our own notifications (countdown timer)
        if (packageName == applicationContext.packageName) return

        // Always allow phone calls and emergency communications
        if (isEmergencyOrCall(sbn)) return

        // Allow notifications from explicitly whitelisted work/study apps
        if (sessionManager.isAppAllowed(packageName)) return

        // Silence and dismiss the distracting notification after capturing a local Digest preview
        captureNotificationDigest(sbn, packageName)
        try {
            cancelNotification(sbn.key)
        } catch (e: Exception) {
            // Ignored if notification cannot be cancelled
        }
    }

    private fun captureNotificationDigest(sbn: StatusBarNotification, packageName: String) {
        val notification = sbn.notification ?: return
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isGroupSummary || isOngoing) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val preview = if (bigText.isNotBlank()) bigText else text
        if (title.isBlank() && preview.isBlank()) return

        val rawLabel = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast(".")
        }
        val resolvedApp = sessionManager.resolveAppName(packageName, rawLabel)
        sessionManager.recordBlockedNotification(
            packageName = packageName,
            appName = resolvedApp,
            title = title,
            preview = preview
        )
    }

    private fun isEmergencyOrCall(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return false

        // Check notification category for phone calls
        if (notification.category == Notification.CATEGORY_CALL ||
            notification.category == Notification.CATEGORY_ALARM ||
            notification.category == Notification.CATEGORY_EVENT
        ) {
            return true
        }

        // Check if package is the dialer or telecom system
        val pkg = sbn.packageName
        val dialer = sessionManager.getDefaultDialerPackage()
        if (pkg == dialer) return true

        val telecomPackages = setOf(
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.dialer",
            "com.android.phone",
            "com.android.server.telecom"
        )
        return telecomPackages.contains(pkg)
    }
}
