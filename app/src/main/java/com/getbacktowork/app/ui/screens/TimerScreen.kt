package com.getbacktowork.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.components.LauncherWallpaperDialog
import com.getbacktowork.app.ui.launcher.LauncherWallpaperStore
import com.getbacktowork.app.ui.launcher.MinimalistHomeActivity
import com.getbacktowork.app.ui.launcher.PinnedEssentialsEditorDialog
import com.getbacktowork.app.ui.launcher.resolveDefaultEssentialApps
import com.getbacktowork.app.ui.overlay.BlockingOverlayActivity
import com.getbacktowork.app.ui.overlay.EmergencyPassDialog
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.ClockDisplayText
import com.getbacktowork.app.ui.theme.CrispBone
import com.getbacktowork.app.ui.theme.DangerRed
import com.getbacktowork.app.ui.theme.DarkBackground
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.EmeraldMint
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.InkOnLight
import com.getbacktowork.app.ui.theme.IvoryAction
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SectionHeaderLabel
import com.getbacktowork.app.ui.theme.SignalCoral
import com.getbacktowork.app.ui.theme.SkyCyan
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import com.getbacktowork.app.ui.theme.TimerDisplayText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardMode {
    HIGH_FOCUS,
    ULTRA_FOCUS,
    HOLIDAY,
    TRAVEL
}

private val ColdTurkeyCrimson = Color(0xFFDC2626)

@Composable
fun TimerScreen(
    sessionManager: FocusSessionManager,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToDiary: () -> Unit = {},
    selectedMinutes: Int = 25,
    onSelectedMinutesChange: (Int) -> Unit = {},
    strictMode: Boolean = true,
    onStrictModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sessionState by sessionManager.sessionState.collectAsState()
    val holidayState by sessionManager.holidayStateFlow.collectAsState()
    val travelState by sessionManager.travelStateFlow.collectAsState()
    val ultraFocusState by sessionManager.ultraFocusStateFlow.collectAsState()
    val allowedPackages by sessionManager.allowedPackagesFlow.collectAsState()
    val blockedNotifications by sessionManager.blockedNotificationsFlow.collectAsState()
    val homePinnedOrder by sessionManager.homePinnedOrderFlow.collectAsState()
    val isHomePinnedCustomized by sessionManager.homePinnedCustomizedFlow.collectAsState()
    val customAppNames by sessionManager.customAppNamesFlow.collectAsState()
    val launcherWallpaperState by sessionManager.launcherWallpaperStateFlow.collectAsState()
    val diaryEntries by sessionManager.diaryEntriesFlow.collectAsState()
    val dailyGoal by sessionManager.dailyScreenGoalFlow.collectAsState()

    val advice = remember(diaryEntries, dailyGoal, ultraFocusState.isEnabled, sessionState.isActive) {
        sessionManager.getDailyCoachAdvice()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefaultLauncher by remember { mutableStateOf(isGetBackToWorkHomeDefault(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultLauncher = isGetBackToWorkHomeDefault(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var whitelistedAppsList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var allInstalledAppsForLauncher by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoadingLauncherApps by remember { mutableStateOf(false) }
    var showPinnedEditorDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showEmergencyPassDialog by remember { mutableStateOf(false) }
    var showRealityCheckDialog by remember { mutableStateOf(false) }
    var showReflectionsDialog by remember { mutableStateOf(false) }
    var selectedDashboardMode by remember {
        mutableStateOf(
            when {
                ultraFocusState.isEnabled && !sessionState.isActive -> DashboardMode.ULTRA_FOCUS
                travelState.isTravelActive -> DashboardMode.TRAVEL
                holidayState.isHolidayActiveToday && !sessionState.isActive -> DashboardMode.HOLIDAY
                else -> DashboardMode.HIGH_FOCUS
            }
        )
    }

    LaunchedEffect(sessionState.isActive) {
        if (sessionState.isActive) {
            selectedDashboardMode = DashboardMode.HIGH_FOCUS
        }
    }

    LaunchedEffect(allowedPackages) {
        whitelistedAppsList = AppRepository(context).getAppsForPackages(allowedPackages)
    }

    LaunchedEffect(showPinnedEditorDialog, customAppNames) {
        if (showPinnedEditorDialog) {
            isLoadingLauncherApps = allInstalledAppsForLauncher.isEmpty()
            val raw = AppRepository(context).getInstalledApps()
            allInstalledAppsForLauncher = raw
                .map {
                    val resolvedName = sessionManager.resolveAppName(
                        it.uniqueKey,
                        sessionManager.resolveAppName(it.packageName, it.appName)
                    )
                    it.copy(appName = resolvedName)
                }
                .sortedBy { it.appName.lowercase() }
            isLoadingLauncherApps = false
        }
    }

    val presets = listOf(15, 25, 45, 60, 90, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Segmented 4-Mode Switcher
        ModeSwitcherPill(
            currentMode = selectedDashboardMode,
            onSelectMode = { targetMode ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (sessionState.isActive && targetMode != DashboardMode.HIGH_FOCUS) {
                    Toast.makeText(context, "Focus session active. Mode locked until timer ends.", Toast.LENGTH_SHORT).show()
                } else {
                    selectedDashboardMode = targetMode
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = selectedDashboardMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(130))
            },
            label = "dashboardModeAnimatedContent"
        ) { mode ->
            when (mode) {
                DashboardMode.HIGH_FOCUS -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val adviceColor = when (advice.level) {
                            FocusSessionManager.AdviceLevel.GOOD -> EmeraldMint
                            FocusSessionManager.AdviceLevel.CAUTION -> BrandPrimary
                            FocusSessionManager.AdviceLevel.LOCK_RECOMMENDED -> SignalCoral
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface)
                                .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = advice.headline,
                                    color = adviceColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Screen: ${advice.screenUsedMinutes}m/${advice.screenGoalMinutes}m · Focus: ${advice.focusedMinutes}m",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = if (advice.streakDays > 0) BrandPrimary else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${advice.streakDays}d",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Minimalist Homescreen Option (Works cleanly even when no Focus/Cold Turkey mode is active)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface)
                                .border(
                                    0.75.dp,
                                    if (isDefaultLauncher) EmeraldMint.copy(alpha = 0.35f) else HairlineBorder,
                                    RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Minimalist Homescreen",
                                        tint = if (isDefaultLauncher) EmeraldMint else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isDefaultLauncher) "Minimalist Homescreen On" else "Minimalist Homescreen",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isDefaultLauncher) "Default launcher even with no mode active" else "Set as default phone home even in No Mode",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceVariant)
                                            .clickable {
                                                context.startActivity(Intent(context, MinimalistHomeActivity::class.java))
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Open",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDefaultLauncher) DarkSurfaceVariant else IvoryAction)
                                            .clickable {
                                                try {
                                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                                } catch (_: Exception) {
                                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                                }
                                            }
                                            .padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isDefaultLauncher) "Switch" else "Set Home",
                                            color = if (isDefaultLauncher) TextSecondary else InkOnLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(HairlineBorder)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isLoadingLauncherApps = allInstalledAppsForLauncher.isEmpty()
                                        showPinnedEditorDialog = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pinned Essentials",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isHomePinnedCustomized) {
                                            "${homePinnedOrder.size} apps pinned on Homescreen"
                                        } else {
                                            "Auto (${allowedPackages.size} Allowed Tools)"
                                        },
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Edit Pinned Essentials",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(HairlineBorder)
                            )
                            val hasWallpaperFile = remember(launcherWallpaperState.version, launcherWallpaperState.isEnabled) {
                                LauncherWallpaperStore.hasSavedWallpaperFile(context)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWallpaperDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Launcher Wallpaper",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (launcherWallpaperState.isEnabled && hasWallpaperFile) {
                                            "Custom Photo • ${launcherWallpaperState.dimPercent}% dim"
                                        } else {
                                            "Pure OLED Black (Default)"
                                        },
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Launcher Wallpaper",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (launcherWallpaperState.isEnabled && hasWallpaperFile) "Edit" else "Set",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (!sessionState.isActive && !ultraFocusState.isEnabled && blockedNotifications.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(0.75.dp, SkyCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable { onNavigateToDiary() }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${blockedNotifications.size} missed alert${if (blockedNotifications.size == 1) "" else "s"} held in Digest",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Review →",
                                    color = SkyCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val progress = if (sessionState.isActive && sessionState.totalDurationMinutes > 0) {
                            val totalMillis = sessionState.totalDurationMinutes * 60 * 1000f
                            (sessionState.remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
                        } else {
                            1f
                        }

                        val (timeString, subLabel) = if (sessionState.isActive) {
                            val totalSecs = (sessionState.remainingMillis / 1000).coerceAtLeast(0)
                            val mins = totalSecs / 60
                            val secs = totalSecs % 60
                            String.format(Locale.getDefault(), "%02d:%02d", mins, secs) to "Focusing"
                        } else {
                            "$selectedMinutes:00" to "Ready"
                        }

                        HeroFocusDial(
                            progress = progress,
                            timeText = timeString,
                            subLabel = subLabel,
                            isSessionActive = sessionState.isActive,
                            accentColor = BrandPrimary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (!sessionState.isActive) {
                            // 6-step Duration Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (minutes in presets) {
                                    val isSelected = selectedMinutes == minutes
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) IvoryAction else DarkSurface)
                                            .border(
                                                0.75.dp,
                                                if (isSelected) IvoryAction else HairlineBorder,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSelectedMinutesChange(minutes)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${minutes}m",
                                            color = if (isSelected) InkOnLight else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Grouped Configuration Surface
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(DarkSurface)
                                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Strict Mode",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Strict Lock",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Prevents exiting or editing apps mid-session",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = strictMode,
                                        onCheckedChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onStrictModeChange(it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = InkOnLight,
                                            checkedTrackColor = IvoryAction,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurfaceVariant
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 46.dp)
                                        .height(0.5.dp)
                                        .background(HairlineBorder)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onNavigateToWhitelist()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Allowed Apps",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Allowed Apps",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Accessible in your Minimalist Launcher",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${whitelistedAppsList.size}",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // High-Contrast Primary CTA
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sessionManager.startSession(selectedMinutes, strictMode)
                                    val intent = Intent(context, BlockingOverlayActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = InkOnLight,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start $selectedMinutes-Min Focus",
                                    color = InkOnLight,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            AutoFocusSchedulesCard(sessionManager = sessionManager)
                        } else {
                            // Active Session State
                            if (sessionState.isEmergencyPassActive) {
                                val passSecs = (sessionState.emergencyPassRemainingMillis / 1000).coerceAtLeast(0)
                                val passTimeStr = String.format(
                                    Locale.getDefault(),
                                    "%02d:%02d",
                                    passSecs / 60,
                                    passSecs % 60
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(DarkSurface)
                                        .border(0.75.dp, EmeraldMint.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "EMERGENCY PASS ACTIVE · $passTimeStr",
                                        color = EmeraldMint,
                                        style = MicroLabelText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Apps are temporarily unlocked. Shield resumes automatically when time expires.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        sessionManager.endEmergencyPassEarly()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = "Resume Focus Shield Now",
                                        color = InkOnLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(DarkSurface)
                                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (sessionState.isStrictMode) "STRICT LOCK ACTIVE" else "FOCUS SHIELD ACTIVE",
                                        color = BrandPrimary,
                                        style = MicroLabelText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Distracting apps are blocked. Your phone defaults to the Minimalist Launcher.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val intent = Intent(context, BlockingOverlayActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Open Minimalist Launcher",
                                        color = InkOnLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val remainingPasses = sessionState.remainingPassesToday
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showEmergencyPassDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = if (remainingPasses > 0) TextPrimary else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Emergency Pass ($remainingPasses left today)",
                                        color = if (remainingPasses > 0) TextPrimary else TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                DashboardMode.ULTRA_FOCUS -> {
                    UltraFocusDashboard(
                        sessionManager = sessionManager,
                        ultraFocusState = ultraFocusState,
                        allowedAppsCount = allowedPackages.size,
                        onNavigateToWhitelist = onNavigateToWhitelist,
                        onOpenRealityCheck = { showRealityCheckDialog = true }
                    )
                }
                DashboardMode.HOLIDAY -> {
                    HolidayDashboard(
                        sessionManager = sessionManager,
                        holidayState = holidayState
                    )
                }
                DashboardMode.TRAVEL -> {
                    TravelDashboard(
                        sessionManager = sessionManager,
                        travelState = travelState,
                        sessionState = sessionState,
                        ultraFocusState = ultraFocusState
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val reflectionsCount = remember(sessionState) { sessionManager.getReflections().size }
        if (reflectionsCount > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showReflectionsDialog = true
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Journal",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Intentions Journal ($reflectionsCount)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEmergencyPassDialog) {
        EmergencyPassDialog(
            sessionManager = sessionManager,
            onDismiss = { showEmergencyPassDialog = false },
            onPassActivated = { showEmergencyPassDialog = false }
        )
    }

    if (showRealityCheckDialog) {
        UltraFocusRealityCheckDialog(
            sessionManager = sessionManager,
            onDismiss = { showRealityCheckDialog = false },
            onTravelPassActivated = {
                showRealityCheckDialog = false
                selectedDashboardMode = DashboardMode.TRAVEL
            }
        )
    }

    if (showReflectionsDialog) {
        ReflectionsJournalDialog(
            sessionManager = sessionManager,
            onDismiss = { showReflectionsDialog = false }
        )
    }

    if (showPinnedEditorDialog) {
        val allowedResolved = remember(whitelistedAppsList, customAppNames) {
            whitelistedAppsList
                .map {
                    val resolvedName = sessionManager.resolveAppName(
                        it.uniqueKey,
                        sessionManager.resolveAppName(it.packageName, it.appName)
                    )
                    it.copy(appName = resolvedName)
                }
                .sortedBy { it.appName.lowercase() }
        }
        val initialOrder = if (isHomePinnedCustomized) {
            homePinnedOrder
        } else {
            resolveDefaultEssentialApps(
                installedApps = allInstalledAppsForLauncher,
                allowedApps = allowedResolved
            ).map { it.uniqueKey }
        }
        PinnedEssentialsEditorDialog(
            installedApps = allInstalledAppsForLauncher,
            isLoadingApps = isLoadingLauncherApps,
            initialPinnedOrder = initialOrder,
            allowedPackages = allowedPackages,
            onDismiss = { showPinnedEditorDialog = false },
            onSave = { updatedOrder ->
                sessionManager.setHomePinnedPackages(updatedOrder)
                showPinnedEditorDialog = false
                Toast.makeText(
                    context,
                    "Updated Pinned Essentials (${updatedOrder.size})",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onResetToAuto = {
                sessionManager.resetHomePinnedToAuto()
                showPinnedEditorDialog = false
                Toast.makeText(context, "Reset Pinned Essentials to auto", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showWallpaperDialog) {
        LauncherWallpaperDialog(
            sessionManager = sessionManager,
            onDismiss = { showWallpaperDialog = false }
        )
    }
}

/**
 * Restrained Swiss Precision Focus Dial:
 * - Clean track and solid semantic arc (zero neon halo or multi-color sweep slop)
 * - 24 subtle architectural tick indices
 * - Proportional tabular numerals so width stays rock steady
 */
@Composable
fun HeroFocusDial(
    progress: Float,
    timeText: String,
    subLabel: String,
    isSessionActive: Boolean = false,
    accentColor: Color = BrandPrimary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "heroDialProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "subtleRingPulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSessionActive) 0.72f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeRingAlpha"
    )

    Box(
        modifier = modifier.size(252.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Clean minimalist track ring
            drawCircle(
                color = Color(0xFF1A1A1C),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Crisp active arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = accentColor.copy(alpha = ringAlpha),
                    startAngle = -90f,
                    sweepAngle = (360f * animatedProgress).coerceIn(1f, 360f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = TimerDisplayText.copy(fontSize = 60.sp, lineHeight = 64.sp),
                color = Color.White,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSessionActive) accentColor else TextMuted
            )
        }
    }
}

@Composable
fun ModeSwitcherPill(
    currentMode: DashboardMode,
    onSelectMode: (DashboardMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeSegment(
            label = "Focus",
            selected = currentMode == DashboardMode.HIGH_FOCUS,
            dotColor = BrandPrimary,
            onClick = { onSelectMode(DashboardMode.HIGH_FOCUS) },
            modifier = Modifier.weight(1f)
        )
        ModeSegment(
            label = "Ultra",
            selected = currentMode == DashboardMode.ULTRA_FOCUS,
            dotColor = SignalCoral,
            onClick = { onSelectMode(DashboardMode.ULTRA_FOCUS) },
            modifier = Modifier.weight(1f)
        )
        ModeSegment(
            label = "Holiday",
            selected = currentMode == DashboardMode.HOLIDAY,
            dotColor = EmeraldMint,
            onClick = { onSelectMode(DashboardMode.HOLIDAY) },
            modifier = Modifier.weight(1f)
        )
        ModeSegment(
            label = "Travel",
            selected = currentMode == DashboardMode.TRAVEL,
            dotColor = SkyCyan,
            onClick = { onSelectMode(DashboardMode.TRAVEL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    dotColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) DarkSurfaceElevated else Color.Transparent,
        animationSpec = tween(160),
        label = "segBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextMuted,
        animationSpec = tween(160),
        label = "segText"
    )

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ReflectionsJournalDialog(
    sessionManager: FocusSessionManager,
    onDismiss: () -> Unit
) {
    val entries = remember { sessionManager.getReflections() }
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(22.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Intentions Journal",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (entries.isEmpty()) {
                    Text(
                        text = "No reflections logged yet.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = if (entry.timestamp > 0) sdf.format(Date(entry.timestamp)) else entry.date,
                                    color = BrandPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.reason,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolidayDashboard(
    sessionManager: FocusSessionManager,
    holidayState: FocusSessionManager.HolidayState
) {
    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
    val calDays = listOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Master & Scheduling Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(
                    0.75.dp,
                    if (holidayState.isMasterEnabled) EmeraldMint.copy(alpha = 0.35f) else HairlineBorder,
                    RoundedCornerShape(18.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Holiday Mode",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            !holidayState.isMasterEnabled -> "Off — zero interruptions on your days off"
                            holidayState.isHolidayActiveToday -> "Active today · Gentle leisure guardrails on"
                            else -> "Standby · Active on scheduled days"
                        },
                        color = if (holidayState.isHolidayActiveToday) EmeraldMint else TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = holidayState.isMasterEnabled,
                    onCheckedChange = { sessionManager.setHolidayMasterEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkOnLight,
                        checkedTrackColor = EmeraldMint,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            if (holidayState.isMasterEnabled) {
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Today", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Manual override for days off or leave", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = holidayState.isManualHolidayEnabled,
                        onCheckedChange = { sessionManager.setHolidayManualEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = InkOnLight,
                            checkedTrackColor = EmeraldMint,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Weekly Schedule", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Runs automatically on selected days", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = holidayState.isAutoScheduleEnabled,
                            onCheckedChange = { sessionManager.setHolidayAutoScheduleEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = InkOnLight,
                                checkedTrackColor = EmeraldMint,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    if (holidayState.isAutoScheduleEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            calDays.forEachIndexed { index, dayCal ->
                                val isSelected = holidayState.scheduledDays.contains(dayCal)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) EmeraldMint else DarkSurfaceVariant)
                                        .clickable { sessionManager.toggleHolidayScheduledDay(dayCal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNames[index],
                                        color = if (isSelected) InkOnLight else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!holidayState.isMasterEnabled) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { sessionManager.setHolidayMasterEnabled(true) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Enable Holiday Mode", color = InkOnLight, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))

            // Budget Limiter Card (Slider driven cleanly from baseLimitValue)
            val isReels = holidayState.limitType == FocusSessionManager.LIMIT_TYPE_REELS
            val extension = (holidayState.limitValue - holidayState.baseLimitValue).coerceAtLeast(0)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Text(text = "LEISURE LIMITER", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        true to Pair(Icons.Default.VideoLibrary, "Reels & Shorts"),
                        false to Pair(Icons.Default.Timer, "Screen Minutes")
                    ).forEach { (reelsOption, iconAndLabel) ->
                        val active = isReels == reelsOption
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) EmeraldMint else Color.Transparent)
                                .clickable {
                                    sessionManager.setHolidayLimitType(
                                        if (reelsOption) FocusSessionManager.LIMIT_TYPE_REELS
                                        else FocusSessionManager.LIMIT_TYPE_MINUTES
                                    )
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconAndLabel.first,
                                contentDescription = null,
                                tint = if (active) InkOnLight else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = iconAndLabel.second,
                                color = if (active) InkOnLight else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (isReels) "Base Daily Threshold" else "Base Time Allowance",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isReels) "${holidayState.baseLimitValue} Reels" else "${holidayState.baseLimitValue} Mins",
                        color = EmeraldMint,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isReels) {
                    Slider(
                        value = holidayState.baseLimitValue.toFloat().coerceIn(20f, 200f),
                        onValueChange = { sessionManager.setHolidayLimitValue(((it / 10).toInt() * 10).coerceIn(20, 200)) },
                        valueRange = 20f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = IvoryAction,
                            activeTrackColor = EmeraldMint,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                } else {
                    Slider(
                        value = holidayState.baseLimitValue.toFloat().coerceIn(10f, 120f),
                        onValueChange = { sessionManager.setHolidayLimitValue(((it / 5).toInt() * 5).coerceIn(10, 120)) },
                        valueRange = 10f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = IvoryAction,
                            activeTrackColor = EmeraldMint,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))
                Spacer(modifier = Modifier.height(12.dp))

                val currentUsage = if (isReels) holidayState.reelsScrolledToday else holidayState.minutesUsedToday
                val totalLimit = holidayState.limitValue.coerceAtLeast(1)
                val usageProgress = (currentUsage.toFloat() / totalLimit.toFloat()).coerceIn(0f, 1f)
                val extNote = if (extension > 0) " (+$extension ext)" else ""
                val unit = if (isReels) "Reels" else "Mins"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "TODAY'S USAGE", style = SectionHeaderLabel)
                    Text(
                        text = "$currentUsage / ${holidayState.limitValue} $unit$extNote",
                        color = if (holidayState.isLimitReached) BrandPrimary else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { usageProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (holidayState.isLimitReached) BrandPrimary else EmeraldMint,
                    trackColor = DarkSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { sessionManager.resetHolidayCounters() }) {
                        Text(text = "Reset Counter", color = TextMuted, fontSize = 11.sp)
                    }
                    val overlayOn by sessionManager.reelsOverlayEnabledFlow.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sessionManager.setReelsOverlayEnabled(!overlayOn) }
                    ) {
                        Text(
                            text = if (overlayOn) "Floating Counter: On" else "Floating Counter: Off",
                            color = if (overlayOn) EmeraldMint else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TravelDashboard(
    sessionManager: FocusSessionManager,
    travelState: FocusSessionManager.TravelState,
    sessionState: FocusSessionManager.SessionState,
    ultraFocusState: FocusSessionManager.UltraFocusState
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedHours by remember { mutableIntStateOf(if (travelState.durationHours > 0) travelState.durationHours else 2) }
    val isTravelActive = travelState.isTravelActive
    val isBlockedBySession = sessionState.isActive
    val isBlockedByUltra = ultraFocusState.isEnabled
    val isActivationBlocked = isBlockedBySession || isBlockedByUltra

    LaunchedEffect(isTravelActive, lifecycleOwner) {
        if (isTravelActive) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    sessionManager.refreshTravelState()
                    delay(15_000L)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(
                0.75.dp,
                if (isTravelActive) SkyCyan.copy(alpha = 0.45f) else HairlineBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(SkyCyan.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = "Travel Mode",
                tint = SkyCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isTravelActive) "Travel Window Active" else "Travel Mode",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isTravelActive)
                "Holiday limits and blocks are temporarily suspended while you commute or travel."
            else
                "Suspend distraction blocks cleanly for flights, trains, or urgent travel.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isTravelActive) {
            val totalMins = (travelState.remainingMillis / 60000L).coerceAtLeast(0)
            val hours = totalMins / 60
            val mins = totalMins % 60
            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = timeStr, color = SkyCyan, style = ClockDisplayText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "REMAINING • COMMUTE & LIFE OUTSIDE SCREEN",
                    style = MicroLabelText,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val travelReels = remember(travelState, sessionState) { sessionManager.getTravelReelsScrolled() }
            val travelMins = remember(travelState, sessionState) { sessionManager.getTravelScreenMinutesUsed() }
            Text(
                text = "Travel Reels: $travelReels · Screen: ${travelMins}m · Gentle outdoor reminder triggers at 30m / 25 Reels.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { sessionManager.endTravelMode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, DangerRed.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "End Travel Window Early",
                    color = DangerRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(text = "TRANSIT DURATION", style = SectionHeaderLabel)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (h in listOf(1, 2, 4, 8)) {
                    val isSelected = selectedHours == h
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) IvoryAction else DarkSurfaceVariant)
                            .clickable { selectedHours = h }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${h}h",
                            color = if (isSelected) InkOnLight else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isActivationBlocked) {
                Text(
                    text = if (isBlockedByUltra) {
                        "Travel Mode cannot bypass 24h Cold Turkey. Use your 1-per-day 30m emergency lifeline in Ultra if urgent."
                    } else {
                        "Cannot activate Travel Mode while a Focus session is active."
                    },
                    color = DangerRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val activated = sessionManager.activateTravelMode(selectedHours)
                    if (!activated) {
                        Toast.makeText(
                            context,
                            if (isBlockedByUltra) "Blocked during 24h Cold Turkey." else "Cannot activate during Focus session.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = !isActivationBlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyCyan,
                    disabledContainerColor = DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flight,
                    contentDescription = null,
                    tint = if (!isActivationBlocked) InkOnLight else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isBlockedByUltra -> "Locked by Cold Turkey"
                        isBlockedBySession -> "Focus Session Active"
                        else -> "Start ${selectedHours}h Travel Window"
                    },
                    color = if (!isActivationBlocked) InkOnLight else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun UltraFocusDashboard(
    sessionManager: FocusSessionManager,
    ultraFocusState: FocusSessionManager.UltraFocusState,
    allowedAppsCount: Int,
    onNavigateToWhitelist: () -> Unit,
    onOpenRealityCheck: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val isEnabled = ultraFocusState.isEnabled
    val isEmergency30mActive = ultraFocusState.isEmergency30mActive
    val isTransitActive = ultraFocusState.isTransitPassActive
    val hasUsedToday = ultraFocusState.hasUsedEmergency30mToday
    val committedHours = ultraFocusState.durationHours.coerceAtLeast(1)
    var selectedCommitHours by remember(ultraFocusState.durationHours) {
        mutableIntStateOf(committedHours)
    }

    var tickNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isEnabled, lifecycleOwner) {
        if (isEnabled) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    tickNow = System.currentTimeMillis()
                    sessionManager.refreshUltraFocusState()
                    delay(1000L)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Primary Cold Turkey Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(
                    0.75.dp,
                    if (isEnabled) SignalCoral.copy(alpha = 0.45f) else HairlineBorder,
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isEnabled) SignalCoral.copy(alpha = 0.14f) else DarkSurfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Cold Turkey",
                    tint = if (isEnabled) SignalCoral else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    !isEnabled -> "Cold Turkey Mode"
                    isEmergency30mActive -> "30-Min Emergency Pass Active"
                    isTransitActive -> "${committedHours}h Lock + Transit Lifeline"
                    else -> "${committedHours}h Cold Turkey Lock"
                },
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    !isEnabled -> "Choose commitment duration (${selectedCommitHours}h). Only whitelisted apps work. If you commute unexpectedly, Transit Lifeline opens Maps/Rides/Pay while social media stays blocked."
                    isEmergency30mActive -> "Phone is temporarily open for urgent tasks. Cold Turkey resumes automatically when this window finishes."
                    isTransitActive -> "Navigation, ride-hailing, rail, airlines & payments allowed cleanly. Social media, Reels, YouTube & browsers remain sealed."
                    else -> "Non-whitelisted apps and settings are sealed. Zero exit before ${committedHours}h completes."
                },
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (!isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "COMMITMENT DURATION", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (h in listOf(4, 8, 12, 24)) {
                        val isSelected = selectedCommitHours == h
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) IvoryAction else DarkSurfaceVariant)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCommitHours = h
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${h}h",
                                color = if (isSelected) InkOnLight else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(18.dp))

                val remainingLockMillis = remember(tickNow, ultraFocusState) {
                    sessionManager.getUltraFocus24hRemainingMillis(tickNow)
                }
                val isCommitmentDone = remainingLockMillis == 0L

                // Live Countdown Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isEmergency30mActive) {
                        val secsLeft = (ultraFocusState.emergency30mRemainingMillis / 1000L).coerceAtLeast(0L)
                        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", secsLeft / 60, secsLeft % 60)
                        Text(text = timeStr, color = EmeraldMint, style = ClockDisplayText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "EMERGENCY PASS REMAINING",
                            style = MicroLabelText,
                            color = TextMuted
                        )
                    } else {
                        val totalSecs = (remainingLockMillis / 1000L).coerceAtLeast(0L)
                        val hours = totalSecs / 3600L
                        val mins = (totalSecs % 3600L) / 60L
                        val secs = totalSecs % 60L
                        val timeStr = if (hours > 0) "${hours}h ${mins}m" else String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                        Text(
                            text = if (isCommitmentDone) "Complete" else timeStr,
                            color = if (isCommitmentDone) EmeraldMint else SignalCoral,
                            style = ClockDisplayText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val totalDurationMillis = (committedHours * 60f * 60f * 1000f).coerceAtLeast(1f)
                        val elapsedProgress = (1f - (remainingLockMillis / totalDurationMillis)).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { elapsedProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = if (isCommitmentDone) EmeraldMint else SignalCoral,
                            trackColor = DarkSurfaceElevated
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isCommitmentDone) "${committedHours}H COMMITMENT FULFILLED" else "REMAINING IN ${committedHours}H LOCK",
                            style = MicroLabelText,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transit Lifeline Button (Maps/Rides/Rail/Pay only during commute while blocking social media)
                if (!isEmergency30mActive) {
                    val transitMins = (ultraFocusState.transitPassRemainingMillis / 60000L).coerceAtLeast(1L)
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isTransitActive) {
                                sessionManager.endUltraFocusTransitMode()
                                Toast.makeText(context, "Transit Lifeline ended.", Toast.LENGTH_SHORT).show()
                            } else {
                                sessionManager.activateUltraFocusTransitMode(120)
                                Toast.makeText(context, "Transit Lifeline ON (2h): Maps, Rides, Rail & Payments open.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isTransitActive) SkyCyan.copy(alpha = 0.14f) else DarkSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.75.dp,
                            if (isTransitActive) SkyCyan else HairlineBorder
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Transit Lifeline",
                            tint = if (isTransitActive) SkyCyan else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTransitActive) "Transit Lifeline On (${transitMins}m left) · Tap to End"
                            else "Transit Lifeline · Maps, Rides & Pay Only",
                            color = if (isTransitActive) SkyCyan else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isEmergency30mActive) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sessionManager.endTravelMode()
                            Toast.makeText(context, "Emergency pass ended. Cold Turkey lock resumed.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("End 30-Min Pass & Resume Lock", color = InkOnLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    // 30-min Daily Emergency Lifeline Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenRealityCheck()
                        },
                        enabled = !hasUsedToday,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyCyan,
                            disabledContainerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = "Emergency 30m Pass",
                            tint = if (!hasUsedToday) InkOnLight else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasUsedToday) "Daily 30m Emergency Pass Used Today" else "Emergency 30-Min Full Pass",
                            color = if (!hasUsedToday) InkOnLight else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val canQuit = ultraFocusState.canQuitNormally
                Button(
                    onClick = {
                        if (canQuit) {
                            if (sessionManager.setUltraFocusEnabled(false)) {
                                Toast.makeText(context, "Cold Turkey completed and turned off.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Cold Turkey cannot be exited before ${committedHours}h completes.", Toast.LENGTH_SHORT).show()
                            onOpenRealityCheck()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canQuit) IvoryAction else DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.75.dp,
                        if (canQuit) IvoryAction else HairlineBorder
                    )
                ) {
                    Icon(
                        imageVector = if (canQuit) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (canQuit) InkOnLight else TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canQuit) "${committedHours}h Complete · Turn Off Mode" else "Locked · Cannot Exit Early",
                        color = if (canQuit) InkOnLight else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!canQuit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onOpenRealityCheck) {
                        Text(
                            text = "Open Reality Check Quote",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Whitelist Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .clickable { onNavigateToWhitelist() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "WHITELISTED WORK APPS", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isEnabled) "$allowedAppsCount apps locked in Minimalist Launcher"
                    else "$allowedAppsCount apps selected · Configure before starting",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit Whitelist",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        if (!isEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sessionManager.startUltraFocusNow(selectedCommitHours)
                    val intent = Intent(context, BlockingOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColdTurkeyCrimson),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = CrispBone,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start ${selectedCommitHours}h Cold Turkey Now",
                    color = CrispBone,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun UltraFocusRealityCheckDialog(
    sessionManager: FocusSessionManager,
    onDismiss: () -> Unit,
    onTravelPassActivated: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val ultraFocusState by sessionManager.ultraFocusStateFlow.collectAsState()
    val hasUsedToday = ultraFocusState.hasUsedEmergency30mToday
    val quotes = FocusSessionManager.REALITY_CHECK_QUOTES
    var quoteIndex by remember { mutableIntStateOf((quotes.indices).random()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(22.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SignalCoral.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Reality Check",
                        tint = SignalCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Moment of Truth",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "COLD TURKEY ACTIVE · NO EXIT PERMITTED",
                    style = MicroLabelText,
                    color = SignalCoral
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "\"${quotes[quoteIndex]}\"",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { quoteIndex = (quoteIndex + 1) % quotes.size }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Next Quote",
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Another quote", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Cold Turkey cannot be exited before your ${ultraFocusState.durationHours}h lock completes. Commuting unexpectedly? Enable Transit Lifeline (Maps, Rides, Rail & Pay only), or use your 1-per-day 30m emergency pass.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                val isTransitOn = ultraFocusState.isTransitPassActive
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isTransitOn) {
                            sessionManager.endUltraFocusTransitMode()
                            Toast.makeText(context, "Transit Lifeline ended.", Toast.LENGTH_SHORT).show()
                        } else {
                            sessionManager.activateUltraFocusTransitMode(120)
                            Toast.makeText(context, "Transit Lifeline ON (2h): Maps, Rides, Rail & Payments open.", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isTransitOn) SkyCyan.copy(alpha = 0.14f) else DarkSurfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, if (isTransitOn) SkyCyan else HairlineBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = if (isTransitOn) SkyCyan else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTransitOn) "End Transit Lifeline" else "Transit Lifeline (Maps/Rides/Pay Only)",
                        color = if (isTransitOn) SkyCyan else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val activated = sessionManager.activateEmergency30mUsage()
                        if (activated) {
                            Toast.makeText(context, "30-Minute Emergency Pass started.", Toast.LENGTH_SHORT).show()
                            onTravelPassActivated()
                        } else {
                            Toast.makeText(context, "Daily 30m emergency pass is unavailable right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !hasUsedToday && !ultraFocusState.isEmergency30mActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkyCyan,
                        disabledContainerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Emergency 30m Pass",
                        tint = if (!hasUsedToday && !ultraFocusState.isEmergency30mActive) InkOnLight else TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasUsedToday) "Daily 30m Emergency Pass Used Today"
                        else "Start 30-Min Emergency Pass",
                        color = if (!hasUsedToday && !ultraFocusState.isEmergency30mActive) InkOnLight else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Back to Work",
                        color = InkOnLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun isGetBackToWorkHomeDefault(context: Context): Boolean {
    return try {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == context.packageName
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun AutoFocusSchedulesCard(sessionManager: FocusSessionManager) {
    val schedule by sessionManager.scheduleConfigFlow.collectAsState()
    val workPresets = listOf(
        Triple("08–12", 8 * 60, 12 * 60),
        Triple("09–17", 9 * 60, 17 * 60),
        Triple("10–18", 10 * 60, 18 * 60),
        Triple("14–18", 14 * 60, 18 * 60)
    )
    val sleepPresets = listOf(
        Triple("22–06", 22 * 60, 6 * 60),
        Triple("23–07", 23 * 60, 7 * 60),
        Triple("00–07", 0, 7 * 60)
    )
    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
    val calDays = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "AUTO-FOCUS SCHEDULES", style = SectionHeaderLabel)
            if (schedule.isWorkActiveNow || schedule.isSleepActiveNow) {
                Text(
                    text = if (schedule.isSleepActiveNow) "SLEEP ACTIVE" else "WORK ACTIVE",
                    style = MicroLabelText,
                    color = EmeraldMint
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Workday Deep Work Schedule
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Workday Auto-Lock", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Blocks non-whitelisted apps during work hours", color = TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = schedule.workEnabled,
                onCheckedChange = { sessionManager.setWorkSchedule(enabled = it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = InkOnLight,
                    checkedTrackColor = IvoryAction,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }

        if (schedule.workEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                workPresets.forEach { (label, sMin, eMin) ->
                    val selected = schedule.workStartMinute == sMin && schedule.workEndMinute == eMin
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) IvoryAction else DarkSurfaceVariant)
                            .clickable { sessionManager.setWorkSchedule(true, sMin, eMin, schedule.workDays) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) InkOnLight else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                calDays.forEachIndexed { index, calDay ->
                    val active = schedule.workDays.contains(calDay)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (active) EmeraldMint else DarkSurfaceVariant)
                            .clickable {
                                val next = schedule.workDays.toMutableSet()
                                if (active) next.remove(calDay) else next.add(calDay)
                                sessionManager.setWorkSchedule(true, days = next)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLetters[index],
                            color = if (active) InkOnLight else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))
        Spacer(modifier = Modifier.height(12.dp))

        // Sleep Wind-Down Schedule
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sleep Wind-Down Lock", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Locks late-night doomscrolling while you rest", color = TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = schedule.sleepEnabled,
                onCheckedChange = { sessionManager.setSleepSchedule(enabled = it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = InkOnLight,
                    checkedTrackColor = EmeraldMint,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceVariant
                )
            )
        }

        if (schedule.sleepEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sleepPresets.forEach { (label, sMin, eMin) ->
                    val selected = schedule.sleepStartMinute == sMin && schedule.sleepEndMinute == eMin
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) EmeraldMint else DarkSurfaceVariant)
                            .clickable { sessionManager.setSleepSchedule(true, sMin, eMin) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) InkOnLight else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
