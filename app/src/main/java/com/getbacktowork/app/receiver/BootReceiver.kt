package com.getbacktowork.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.service.FocusTimerService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val sessionManager = FocusSessionManager.getInstance(context)
            if (sessionManager.isSessionActive()) {
                val serviceIntent = Intent(context, FocusTimerService::class.java).apply {
                    action = FocusTimerService.ACTION_START_TIMER
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
