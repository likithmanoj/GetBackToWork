package com.getbacktowork.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val sessionManager = FocusSessionManager.getInstance(context)

    suspend fun getAppsForPackages(packages: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val list = mutableListOf<AppInfo>()
        for (pkg in packages) {
            if (pkg == context.packageName) continue
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val iconBitmap = try {
                    val drawable = pm.getApplicationIcon(pkg)
                    drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888).asImageBitmap()
                } catch (e: Throwable) {
                    null
                }
                list.add(
                    AppInfo(
                        packageName = pkg,
                        appName = label,
                        isAllowed = true,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        iconBitmap = iconBitmap
                    )
                )
            } catch (e: Throwable) {
                // Package not found or uninstalled
            }
        }
        list.sortedBy { it.appName.lowercase() }
    }

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        } catch (e: Throwable) {
            emptyList()
        }

        Log.d("GetBackToWork", "queryIntentActivities found: ${resolveInfos.size} apps")

        val allowedPackages = sessionManager.getAllowedPackages()
        val list = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || seen.contains(pkg)) {
                continue
            }
            seen.add(pkg)

            val label = try {
                resolveInfo.loadLabel(pm).toString()
            } catch (e: Throwable) {
                pkg
            }

            val iconBitmap = try {
                val drawable = resolveInfo.loadIcon(pm)
                drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888).asImageBitmap()
            } catch (e: Throwable) {
                null
            }

            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Throwable) {
                false
            }

            list.add(
                AppInfo(
                    packageName = pkg,
                    appName = label,
                    isAllowed = allowedPackages.contains(pkg),
                    isSystemApp = isSystem,
                    iconBitmap = iconBitmap
                )
            )
        }

        // Fallback: If queryIntentActivities was blocked or empty, query installed applications directly
        if (list.isEmpty()) {
            Log.d("GetBackToWork", "Falling back to getInstalledApplications")
            try {
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in installedApps) {
                    val pkg = appInfo.packageName ?: continue
                    if (pkg == context.packageName || seen.contains(pkg)) continue
                    if (pm.getLaunchIntentForPackage(pkg) == null) continue // Only show launchable apps
                    seen.add(pkg)

                    val label = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Throwable) {
                        pkg
                    }

                    val iconBitmap = try {
                        val drawable = pm.getApplicationIcon(appInfo)
                        drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888).asImageBitmap()
                    } catch (e: Throwable) {
                        null
                    }

                    list.add(
                        AppInfo(
                            packageName = pkg,
                            appName = label,
                            isAllowed = allowedPackages.contains(pkg),
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            iconBitmap = iconBitmap
                        )
                    )
                }
            } catch (e: Throwable) {
                Log.e("GetBackToWork", "Error loading installed apps", e)
            }
        }

        Log.d("GetBackToWork", "Total installed apps loaded: ${list.size}")
        list.sortedBy { it.appName.lowercase() }
    }
}
