package com.getbacktowork.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.getbacktowork.app.R
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.MainActivity
import java.util.Locale

class FocusTimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private lateinit var sessionManager: FocusSessionManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager

    companion object {
        const val ACTION_START_TIMER = "com.getbacktowork.app.START_TIMER"
        const val ACTION_STOP_TIMER = "com.getbacktowork.app.STOP_TIMER"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        private const val NOTIFICATION_ID = 1001
        private const val DIGEST_NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "focus_session_channel"
        private const val DIGEST_CHANNEL_ID = "focus_digest_channel"
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val remainingMillis = sessionManager.getRemainingMillis()
                if (remainingMillis > 0) {
                    startForegroundCompat(remainingMillis)
                    startTimer(remainingMillis)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP_TIMER -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                if (sessionManager.isSessionActive()) {
                    val remaining = sessionManager.getRemainingMillis()
                    startForegroundCompat(remaining)
                    startTimer(remaining)
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(remainingMillis: Long) {
        val notification = buildNotification(remainingMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer(durationMillis: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(durationMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (powerManager.isInteractive) {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(millisUntilFinished))
                    sessionManager.refreshState()
                }
            }

            override fun onFinish() {
                sessionManager.stopSession(force = true)
                sessionManager.refreshState()
                postMissedDigestSummaryIfNeeded()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun buildNotification(remainingMillis: Long): Notification {
        val totalSeconds = remainingMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPass = sessionManager.isEmergencyPassActive()
        val contentText = if (isPass) {
            val passSecs = (sessionManager.getEmergencyPassRemainingMillis() / 1000).coerceAtLeast(0)
            val pMins = passSecs / 60
            val pSecs = passSecs % 60
            String.format(Locale.getDefault(), "Emergency Pass: %02d:%02d left (Focus: %s)", pMins, pSecs, timeFormatted)
        } else {
            "Focus session active: $timeFormatted remaining"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GET BACK TO WORK")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification_focus)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun postMissedDigestSummaryIfNeeded() {
        val missed = sessionManager.getBlockedNotifications()
        if (missed.isEmpty()) return
        val openDiaryIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TAB, 2)
        }
        val pi = PendingIntent.getActivity(
            this,
            102,
            openDiaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val previewApps = missed.map { it.appName }.distinct().take(3).joinToString(", ")
        val summary = NotificationCompat.Builder(this, DIGEST_CHANNEL_ID)
            .setContentTitle("Focus Complete · ${missed.size} Missed Alert${if (missed.size == 1) "" else "s"}")
            .setContentText("Held quietly from $previewApps. Tap to inspect Digest.")
            .setSmallIcon(R.drawable.ic_notification_focus)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            notificationManager.notify(DIGEST_NOTIFICATION_ID, summary)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS optional on Android 13+
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val digestChannel = NotificationChannel(
                DIGEST_CHANNEL_ID,
                "Focus Notification Digest",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Summary of alerts held quietly during completed Focus sessions"
            }
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(digestChannel)
        }
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
