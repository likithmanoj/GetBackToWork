package com.getbacktowork.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.overlay.BlockingOverlayActivity

class FocusQuickTileService : TileService() {

    private lateinit var sessionManager: FocusSessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = FocusSessionManager.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        if (sessionManager.isSessionActive()) {
            if (sessionManager.isStrictMode()) {
                Toast.makeText(
                    this,
                    "⚡ Strict Mode active! Open app to use Emergency Pass.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                sessionManager.stopSession()
                Toast.makeText(this, "Focus session stopped.", Toast.LENGTH_SHORT).show()
            }
        } else if (sessionManager.isUltraFocusActive()) {
            val commitHrs = sessionManager.getUltraFocusDurationHours()
            Toast.makeText(
                this,
                "🔒 Cold Turkey active! It cannot be turned off until ${commitHrs}h completes.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Instant 1-tap start 25-minute Pomodoro focus
            sessionManager.startSession(25, strictMode = true)
            Toast.makeText(this, "⚡ 25-Minute Focus Mode Activated!", Toast.LENGTH_SHORT).show()

            // Immediately launch the full-screen Focus Workspace overlay & collapse QS panel
            val overlayIntent = Intent(this, BlockingOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    overlayIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(overlayIntent)
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = sessionManager.isSessionActive()

        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            val remainingMins = (sessionManager.getRemainingMillis() / 60000L).coerceAtLeast(1)
            tile.label = "Focus: Active"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "${remainingMins}m remaining"
            }
        } else if (sessionManager.isUltraFocusActive()) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Ultra Focus: Active"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (sessionManager.isTravelModeActive()) "30m Pass Active" else "Cold Turkey Active"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Focus Mode"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap for 25m focus"
            }
        }
        tile.updateTile()
    }
}
