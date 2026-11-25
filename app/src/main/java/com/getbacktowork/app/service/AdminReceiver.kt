package com.getbacktowork.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.getbacktowork.app.data.FocusSessionManager

class AdminReceiver : DeviceAdminReceiver() {

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, AdminReceiver::class.java)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Anti-Uninstall Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val sessionManager = FocusSessionManager.getInstance(context)
        return if (sessionManager.isSessionActive()) {
            "CAUTION: A focus session is currently in progress! Deactivating Device Admin breaks your commitment to GetBackToWork."
        } else {
            "Disabling Device Admin will remove Anti-Uninstall protection for GetBackToWork."
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Anti-Uninstall Protection Disabled", Toast.LENGTH_SHORT).show()
    }
}
