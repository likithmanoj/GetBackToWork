package com.getbacktowork.app.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean = false,
    val isSystemApp: Boolean = false,
    val iconBitmap: ImageBitmap? = null,
    val isWorkProfile: Boolean = false,
    val profileSerial: Long = 0L,
    val componentClassName: String? = null
) {
    /** Stable key separating Personal (user 0) and Work Profile copies of the same package. */
    val uniqueKey: String
        get() = AppRepository.buildUniqueAppKey(packageName, isWorkProfile, profileSerial)
}
