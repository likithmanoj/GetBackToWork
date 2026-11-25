package com.getbacktowork.app.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean = false,
    val isSystemApp: Boolean = false,
    val iconBitmap: ImageBitmap? = null
)
