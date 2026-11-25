package com.getbacktowork.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Process
import android.os.UserManager
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.getbacktowork.app.ui.overlay.BlockingOverlayActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val sessionManager = FocusSessionManager.getInstance(context)

    companion object {
        fun buildUniqueAppKey(
            packageName: String,
            isWorkProfile: Boolean,
            profileSerial: Long
        ): String = if (isWorkProfile) "work:${profileSerial}:$packageName" else packageName

        fun partitionPersonalAndWorkApps(apps: List<AppInfo>): Pair<List<AppInfo>, List<AppInfo>> {
            val personal = mutableListOf<AppInfo>()
            val work = mutableListOf<AppInfo>()
            for (app in apps) {
                if (app.isWorkProfile) work.add(app) else personal.add(app)
            }
            return personal to work
        }

        fun resolvePinnedSelection(
            apps: List<AppInfo>,
            orderedKeys: List<String>
        ): List<AppInfo> {
            val byKey = apps.associateBy { it.uniqueKey }
            val byPersonalPkg = apps.filterNot { it.isWorkProfile }.associateBy { it.packageName }
            return orderedKeys.mapNotNull { key -> byKey[key] ?: byPersonalPkg[key] }
        }

        /**
         * Launches either a Personal profile app or a Work Profile / Managed Profile app
         * via Android's cross-profile [LauncherApps] service.
         */
        fun launchApp(context: Context, app: AppInfo): Boolean {
            BlockingOverlayActivity.lastLaunchedAllowedTime = System.currentTimeMillis()
            if (!app.isWorkProfile) {
                return try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                        true
                    } else {
                        false
                    }
                } catch (e: Throwable) {
                    Log.w("GetBackToWork", "Failed launching ${app.packageName}", e)
                    false
                }
            }

            return try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                if (launcherApps == null || userManager == null) return false

                val userHandle = userManager.getUserForSerialNumber(app.profileSerial)
                    ?: userManager.userProfiles.firstOrNull {
                        userManager.getSerialNumberForUser(it) == app.profileSerial
                    } ?: return false

                val component = app.componentClassName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { ComponentName(app.packageName, it) }
                    ?: launcherApps.getActivityList(app.packageName, userHandle).firstOrNull()?.componentName
                    ?: return false

                launcherApps.startMainActivity(component, userHandle, null, null)
                true
            } catch (e: Throwable) {
                Log.w("GetBackToWork", "Failed launching work app ${app.packageName}", e)
                false
            }
        }
    }

    suspend fun getAppsForPackages(packages: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val list = mutableListOf<AppInfo>()
        for (pkg in packages) {
            if (pkg == context.packageName || pkg.startsWith("work:")) continue
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
                // Package not found in personal profile
            }
        }

        val workApps = loadWorkProfileApps(allowedPackages = packages)
            .filter { packages.contains(it.packageName) || packages.contains(it.uniqueKey) }
            .map { it.copy(isAllowed = true) }

        (list + workApps).sortedBy { it.appName.lowercase() }
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
                    if (pm.getLaunchIntentForPackage(pkg) == null) continue
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

        val workApps = loadWorkProfileApps(allowedPackages = allowedPackages)
        val combined = list + workApps
        Log.d("GetBackToWork", "Total installed apps loaded: ${combined.size} (work=${workApps.size})")
        combined.sortedBy { it.appName.lowercase() }
    }

    private fun loadWorkProfileApps(allowedPackages: Set<String>): List<AppInfo> {
        val workList = mutableListOf<AppInfo>()
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return emptyList()
            val myHandle = Process.myUserHandle()

            for (profileHandle in userManager.userProfiles) {
                if (profileHandle == myHandle) continue
                val serial = userManager.getSerialNumberForUser(profileHandle)
                if (serial < 0L) continue

                val activities = try {
                    launcherApps.getActivityList(null, profileHandle)
                } catch (_: Throwable) {
                    emptyList()
                }
                val seenPackagesInProfile = mutableSetOf<String>()
                for (activityInfo in activities) {
                    val appInfo = activityInfo.applicationInfo ?: continue
                    val pkg = appInfo.packageName ?: continue
                    if (pkg == context.packageName || seenPackagesInProfile.contains(pkg)) continue
                    seenPackagesInProfile.add(pkg)

                    val label = try {
                        activityInfo.label?.toString()?.trim()?.ifEmpty { pkg } ?: pkg
                    } catch (_: Throwable) {
                        pkg
                    }

                    val iconBitmap = try {
                        val drawable = activityInfo.getBadgedIcon(0)
                        drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888).asImageBitmap()
                    } catch (_: Throwable) {
                        null
                    }

                    workList.add(
                        AppInfo(
                            packageName = pkg,
                            appName = label,
                            isAllowed = allowedPackages.contains(pkg),
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                            iconBitmap = iconBitmap,
                            isWorkProfile = true,
                            profileSerial = serial,
                            componentClassName = activityInfo.componentName?.className
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w("GetBackToWork", "Work profile query skipped or unavailable", e)
        }
        return workList
    }
}
