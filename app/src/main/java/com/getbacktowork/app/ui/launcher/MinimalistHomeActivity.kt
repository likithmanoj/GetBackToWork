package com.getbacktowork.app.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.service.AppBlockerAccessibilityService
import com.getbacktowork.app.ui.MainActivity
import com.getbacktowork.app.ui.components.AppRuleConfigDialog
import com.getbacktowork.app.ui.components.LauncherWallpaperDialog
import com.getbacktowork.app.ui.components.WorkProfileBadge
import com.getbacktowork.app.ui.components.WorkProfileDropdownHeader
import com.getbacktowork.app.ui.overlay.BlockingOverlayActivity
import com.getbacktowork.app.ui.overlay.EmergencyPassDialog
import com.getbacktowork.app.ui.screens.UltraFocusRealityCheckDialog
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.ClockDisplayText
import com.getbacktowork.app.ui.theme.DarkBackground
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.EmeraldMint
import com.getbacktowork.app.ui.theme.GetBackToWorkTheme
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.InkOnLight
import com.getbacktowork.app.ui.theme.IvoryAction
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SectionHeaderLabel
import com.getbacktowork.app.ui.theme.SignalCoral
import com.getbacktowork.app.ui.theme.SkyCyan
import com.getbacktowork.app.ui.theme.SuccessGreen
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Optional Default Minimalist Phone Launcher (`CATEGORY_HOME`).
 * Unifies everyday Phone-Quitter minimalism (smart <1h screen advisor, quick Focus trigger,
 * mindful pause before distracting apps) with distraction-free Focus & Cold Turkey lock modes.
 */
class MinimalistHomeActivity : ComponentActivity() {

    private lateinit var sessionManager: FocusSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = FocusSessionManager.getInstance(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Home launcher absorbs Back press at root
            }
        })

        setContent {
            GetBackToWorkTheme {
                MinimalistHomeContent(
                    sessionManager = sessionManager,
                    onOpenDashboard = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    onOpenPhone = { openPhoneApp() },
                    onOpenCamera = { openCameraApp() },
                    onLaunchApp = { app -> AppRepository.launchApp(this, app) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.refreshState()
        sessionManager.refreshUltraFocusState()
        sessionManager.refreshTravelState()
    }

    private fun openPhoneApp() {
        try {
            BlockingOverlayActivity.lastLaunchedAllowedTime = System.currentTimeMillis()
            startActivity(Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {
        }
    }

    private fun openCameraApp() {
        try {
            BlockingOverlayActivity.lastLaunchedAllowedTime = System.currentTimeMillis()
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {
        }
    }
}

@Composable
private fun MinimalistHomeContent(
    sessionManager: FocusSessionManager,
    onOpenDashboard: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenCamera: () -> Unit,
    onLaunchApp: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val sessionState by sessionManager.sessionState.collectAsState()
    val ultraFocusState by sessionManager.ultraFocusStateFlow.collectAsState()
    val holidayState by sessionManager.holidayStateFlow.collectAsState()
    val travelState by sessionManager.travelStateFlow.collectAsState()
    val allowedPackages by sessionManager.allowedPackagesFlow.collectAsState()
    val homePinnedPackages by sessionManager.homePinnedPackagesFlow.collectAsState()
    val homePinnedOrder by sessionManager.homePinnedOrderFlow.collectAsState()
    val isHomePinnedCustomized by sessionManager.homePinnedCustomizedFlow.collectAsState()
    val diaryEntries by sessionManager.diaryEntriesFlow.collectAsState()
    val dailyGoal by sessionManager.dailyScreenGoalFlow.collectAsState()
    val customAppNames by sessionManager.customAppNamesFlow.collectAsState()
    val lifeGoals by sessionManager.lifeGoalsFlow.collectAsState()
    val scheduleConfig by sessionManager.scheduleConfigFlow.collectAsState()
    val cooldownPackages by sessionManager.cooldownPackagesFlow.collectAsState()
    val appDailyCaps by sessionManager.appDailyCapsFlow.collectAsState()
    val appTodayUsage by sessionManager.appTodayUsageFlow.collectAsState()
    val earnedScrollUntil by sessionManager.earnedScrollUntilFlow.collectAsState()
    val wallpaperState by sessionManager.launcherWallpaperStateFlow.collectAsState()
    val pausedForBanking by sessionManager.accessibilityPausedForBankingFlow.collectAsState()

    var isAccessibilityEnabled by remember {
        mutableStateOf(AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val enabledNow = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context)
                isAccessibilityEnabled = enabledNow
                if (enabledNow && sessionManager.isAccessibilityPausedForBanking()) {
                    sessionManager.setAccessibilityPausedForBanking(false)
                    AppBlockerAccessibilityService.cancelBankingPauseNotification(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val wallpaperBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        wallpaperState.isEnabled,
        wallpaperState.version
    ) {
        value = if (wallpaperState.isEnabled && LauncherWallpaperStore.hasSavedWallpaperFile(context)) {
            LauncherWallpaperStore.loadWallpaperBitmap(context)
        } else {
            null
        }
    }

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var allowedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var tickNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showEmergencyPassDialog by remember { mutableStateOf(false) }
    var showRealityCheckDialog by remember { mutableStateOf(false) }
    var showEditPinnedDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var pausedTargetApp by remember { mutableStateOf<AppInfo?>(null) }
    var actionTargetApp by remember { mutableStateOf<AppInfo?>(null) }

    val scheduleBlocking = (scheduleConfig.isWorkActiveNow || scheduleConfig.isSleepActiveNow) &&
        !sessionState.isEmergencyPassActive && !travelState.isTravelActive && earnedScrollUntil <= tickNow
    val shouldBlock = (sessionState.isActive && !sessionState.isEmergencyPassActive) ||
        (ultraFocusState.isEnabled && !ultraFocusState.isEmergency30mActive) ||
        scheduleBlocking
    val isUltraFocus = ultraFocusState.isEnabled && !sessionState.isActive
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    LaunchedEffect(shouldBlock) {
        if (shouldBlock && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(allowedPackages, ultraFocusState.isTransitPassActive, customAppNames) {
        val repo = AppRepository(context)
        val allRaw = repo.getInstalledApps()
        val all = allRaw.map {
            val resolvedName = sessionManager.resolveAppName(
                it.uniqueKey,
                sessionManager.resolveAppName(it.packageName, it.appName)
            )
            it.copy(appName = resolvedName)
        }.sortedBy { it.appName.lowercase() }
        installedApps = all
        val rawAllowed = if (ultraFocusState.isTransitPassActive) {
            allRaw.filter { allowedPackages.contains(it.packageName) || sessionManager.isTransitEssentialApp(it.packageName) }
        } else {
            repo.getAppsForPackages(allowedPackages)
        }
        allowedApps = rawAllowed.map {
            val resolvedName = sessionManager.resolveAppName(
                it.uniqueKey,
                sessionManager.resolveAppName(it.packageName, it.appName)
            )
            it.copy(appName = resolvedName)
        }.sortedBy { it.appName.lowercase() }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                tickNow = System.currentTimeMillis()
                sessionManager.refreshState()
                sessionManager.refreshUltraFocusState()
                sessionManager.refreshTravelState()
                sessionManager.refreshScheduleState()
                isAccessibilityEnabled = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context)
                delay(1000L)
            }
        }
    }

    val advice = remember(diaryEntries, dailyGoal, ultraFocusState.isEnabled) {
        sessionManager.getDailyCoachAdvice()
    }
    val yearProgress = remember(tickNow) { sessionManager.getYearProgressInfo() }

    val fallbackHomeApps = remember(allowedApps, installedApps) {
        resolveDefaultEssentialApps(installedApps = installedApps, allowedApps = allowedApps)
    }
    val allWorkApps = remember(installedApps) {
        AppRepository.partitionPersonalAndWorkApps(installedApps).second
    }
    val homeVisibleApps = remember(
        allowedApps,
        installedApps,
        homePinnedOrder,
        isHomePinnedCustomized,
        shouldBlock,
        fallbackHomeApps
    ) {
        when {
            shouldBlock -> allowedApps
            isHomePinnedCustomized -> AppRepository.resolvePinnedSelection(installedApps, homePinnedOrder)
            else -> fallbackHomeApps
        }
    }
    val effectivePinnedPackages = remember(homePinnedPackages, isHomePinnedCustomized, fallbackHomeApps) {
        if (isHomePinnedCustomized) homePinnedPackages else fallbackHomeApps.map { it.uniqueKey }.toSet()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (wallpaperState.isEnabled && wallpaperBitmap != null) {
                Image(
                    bitmap = wallpaperBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = wallpaperState.effectiveDimAlpha(shouldBlock)
                            )
                        )
                )
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !shouldBlock,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) { page ->
                if (page == 0) {
                    MinimalistHomeMainPage(
                        tickNow = tickNow,
                        shouldBlock = shouldBlock,
                        isUltraFocus = isUltraFocus,
                        sessionState = sessionState,
                        ultraFocusState = ultraFocusState,
                        holidayState = holidayState,
                        travelState = travelState,
                        advice = advice,
                        yearDaysLeft = "${yearProgress.daysRemaining}d left in ${yearProgress.year}",
                        topLifeGoal = lifeGoals.firstOrNull().orEmpty(),
                        visibleApps = homeVisibleApps,
                        workDropdownApps = if (shouldBlock) {
                            AppRepository.partitionPersonalAndWorkApps(allowedApps).second
                        } else {
                            allWorkApps
                        },
                        sessionManager = sessionManager,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isAccessibilityPausedForBanking = pausedForBanking,
                        onPauseAccessibilityForBanking = {
                            val paused = AppBlockerAccessibilityService.pauseForBanking(context)
                            isAccessibilityEnabled = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context)
                            Toast.makeText(
                                context,
                                if (paused) "Shield paused — UPI/banking apps unlocked. Tap banner to re-enable."
                                else "Cannot pause shield while Focus or Cold Turkey lock is active.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onEnableAccessibility = {
                            AppBlockerAccessibilityService.openAccessibilitySettings(context)
                        },
                        onOpenDashboard = onOpenDashboard,
                        onOpenWallpaper = { showWallpaperDialog = true },
                        onOpenEditPinned = { showEditPinnedDialog = true },
                        onStartQuickFocus = { sessionManager.startSession(25, strictMode = true) },
                        onOpenPhone = onOpenPhone,
                        onOpenCamera = onOpenCamera,
                        onOpenApp = { app ->
                            if (shouldBlock) {
                                onLaunchApp(app)
                            } else {
                                handleIdleAppTap(
                                    app = app,
                                    sessionManager = sessionManager,
                                    onImmediateLaunch = { onLaunchApp(app) },
                                    onRequirePause = { pausedTargetApp = app }
                                )
                            }
                        },
                        onOpenAppOptions = { app -> actionTargetApp = app },
                        onOpenPassOrReality = {
                            if (isUltraFocus) showRealityCheckDialog = true
                            else showEmergencyPassDialog = true
                        },
                        onOpenAllApps = {
                            if (!shouldBlock) {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                        }
                    )
                } else {
                    MinimalistAllAppsPage(
                        allApps = installedApps,
                        homePinnedPackages = effectivePinnedPackages,
                        onTogglePin = { app ->
                            val fallbackKeys = if (!isHomePinnedCustomized) {
                                fallbackHomeApps.map { it.uniqueKey }
                            } else {
                                emptyList()
                            }
                            val pinned = sessionManager.toggleHomePinnedPackage(
                                packageName = app.uniqueKey,
                                initialFallbackPackages = fallbackKeys
                            )
                            Toast.makeText(
                                context,
                                if (pinned) "Pinned ${app.appName} to Homescreen" else "Unpinned ${app.appName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onOpenAppOptions = { app -> actionTargetApp = app },
                        onBackHome = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        onSelectApp = { app ->
                            handleIdleAppTap(
                                app = app,
                                sessionManager = sessionManager,
                                onImmediateLaunch = { onLaunchApp(app) },
                                onRequirePause = { pausedTargetApp = app }
                            )
                        }
                    )
                }
            }
        }
    }

    actionTargetApp?.let { target ->
        AppRuleConfigDialog(
            app = target,
            currentAlias = (customAppNames[target.uniqueKey] ?: customAppNames[target.packageName]).orEmpty(),
            currentCap = appDailyCaps[target.packageName],
            todayUsage = appTodayUsage[target.packageName] ?: FocusSessionManager.AppTodayUsage(target.packageName, ""),
            isCooldownEnabled = cooldownPackages.contains(target.packageName),
            showPinOption = true,
            isPinned = effectivePinnedPackages.contains(target.uniqueKey) ||
                (!target.isWorkProfile && effectivePinnedPackages.contains(target.packageName)),
            onDismiss = { actionTargetApp = null },
            onSaveRules = { newAlias, maxMins, maxOpens ->
                sessionManager.setCustomAppName(target.uniqueKey, newAlias)
                sessionManager.setAppDailyCap(target.packageName, maxMins, maxOpens)
                Toast.makeText(context, "Saved rules for ${target.appName}", Toast.LENGTH_SHORT).show()
                actionTargetApp = null
            },
            onTogglePin = {
                val fallbackKeys = if (!isHomePinnedCustomized) {
                    fallbackHomeApps.map { it.uniqueKey }
                } else {
                    emptyList()
                }
                val pinned = sessionManager.toggleHomePinnedPackage(
                    packageName = target.uniqueKey,
                    initialFallbackPackages = fallbackKeys
                )
                Toast.makeText(context, if (pinned) "Pinned" else "Unpinned", Toast.LENGTH_SHORT).show()
                actionTargetApp = null
            },
            onToggleCooldown = {
                val added = sessionManager.toggleCooldownPackage(target.packageName)
                if (added && sessionManager.getCooldownSeconds() == 0) {
                    sessionManager.setCooldownSeconds(10)
                }
                Toast.makeText(
                    context,
                    if (added) "Mindful Pause active for app" else "Mindful Pause removed",
                    Toast.LENGTH_SHORT
                ).show()
                actionTargetApp = null
            }
        )
    }

    pausedTargetApp?.let { targetApp ->
        MindfulAppPauseDialog(
            app = targetApp,
            screenUsedMinutes = advice.screenUsedMinutes,
            screenGoalMinutes = advice.screenGoalMinutes,
            onDismiss = { pausedTargetApp = null },
            onProceed = {
                pausedTargetApp = null
                onLaunchApp(targetApp)
            }
        )
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
            onTravelPassActivated = { showRealityCheckDialog = false }
        )
    }

    if (showEditPinnedDialog) {
        PinnedEssentialsEditorDialog(
            installedApps = installedApps,
            initialPinnedOrder = if (isHomePinnedCustomized) {
                homePinnedOrder
            } else {
                fallbackHomeApps.map { it.uniqueKey }
            },
            allowedPackages = allowedPackages,
            onDismiss = { showEditPinnedDialog = false },
            onSave = { updatedOrder ->
                sessionManager.setHomePinnedPackages(updatedOrder)
                showEditPinnedDialog = false
                Toast.makeText(
                    context,
                    "Updated Pinned Essentials (${updatedOrder.size})",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onResetToAuto = {
                sessionManager.resetHomePinnedToAuto()
                showEditPinnedDialog = false
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

private fun handleIdleAppTap(
    app: AppInfo,
    sessionManager: FocusSessionManager,
    onImmediateLaunch: () -> Unit,
    onRequirePause: () -> Unit
) {
    val isPrivileged = sessionManager.isAppAllowed(app.packageName) ||
        sessionManager.isTransitEssentialApp(app.packageName)
    val shouldPause = !isPrivileged &&
        sessionManager.isDailyScreenGoalExceeded() &&
        !sessionManager.isHolidayToday() &&
        !sessionManager.isTravelModeActive()
    if (shouldPause) {
        onRequirePause()
    } else {
        onImmediateLaunch()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalistHomeMainPage(
    tickNow: Long,
    shouldBlock: Boolean,
    isUltraFocus: Boolean,
    sessionState: FocusSessionManager.SessionState,
    ultraFocusState: FocusSessionManager.UltraFocusState,
    holidayState: FocusSessionManager.HolidayState,
    travelState: FocusSessionManager.TravelState,
    advice: FocusSessionManager.CoachAdvice,
    yearDaysLeft: String,
    topLifeGoal: String,
    visibleApps: List<AppInfo>,
    workDropdownApps: List<AppInfo>,
    sessionManager: FocusSessionManager,
    isAccessibilityEnabled: Boolean,
    isAccessibilityPausedForBanking: Boolean,
    onPauseAccessibilityForBanking: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenEditPinned: () -> Unit,
    onStartQuickFocus: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenApp: (AppInfo) -> Unit,
    onOpenAppOptions: (AppInfo) -> Unit,
    onOpenPassOrReality: () -> Unit,
    onOpenAllApps: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val currentTime = remember(tickNow) { timeFormatter.format(Date(tickNow)) }
    val currentDate = remember(tickNow) { dateFormatter.format(Date(tickNow)) }
    var workExpanded by remember { mutableStateOf(false) }

    val primaryVisibleApps = remember(visibleApps, shouldBlock) {
        if (shouldBlock) AppRepository.partitionPersonalAndWorkApps(visibleApps).first
        else visibleApps
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Header Clock + Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = currentTime,
                    color = TextPrimary,
                    style = ClockDisplayText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentDate.uppercase(),
                    style = MicroLabelText,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!shouldBlock) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(0.75.dp, HairlineBorder, CircleShape)
                            .clickable { onOpenWallpaper() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Launcher Wallpaper",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(0.75.dp, HairlineBorder, CircleShape)
                            .clickable { onOpenAllApps() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "All Apps",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(0.75.dp, HairlineBorder, CircleShape)
                        .clickable { onOpenDashboard() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open GetBackToWork",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Mode or Smart Quitter Advisor Banner
        if (shouldBlock) {
            val remaining24h = sessionManager.getUltraFocus24hRemainingMillis(tickNow)
            val totalSecs = if (isUltraFocus) {
                (remaining24h / 1000L).coerceAtLeast(0L)
            } else {
                (sessionState.remainingMillis / 1000L).coerceAtLeast(0L)
            }
            val mins = totalSecs / 60L
            val statusStr = if (isUltraFocus) {
                String.format(Locale.getDefault(), "%02dh %02dm", mins / 60L, mins % 60L)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", mins, totalSecs % 60L)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(
                        0.75.dp,
                        if (isUltraFocus) SignalCoral.copy(alpha = 0.45f) else BrandPrimary.copy(alpha = 0.45f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUltraFocus) Icons.Default.Whatshot else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUltraFocus) SignalCoral else BrandPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUltraFocus) {
                            if (ultraFocusState.isTransitPassActive) "Cold Turkey + Transit" else "Cold Turkey Active"
                        } else {
                            "Deep Focus Active"
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = statusStr,
                    color = if (isUltraFocus) SignalCoral else BrandPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        } else {
            val accentColor = when (advice.level) {
                FocusSessionManager.AdviceLevel.GOOD -> EmeraldMint
                FocusSessionManager.AdviceLevel.CAUTION -> BrandPrimary
                FocusSessionManager.AdviceLevel.LOCK_RECOMMENDED -> SignalCoral
            }
            val budgetProgress = (advice.screenUsedMinutes.toFloat() / advice.screenGoalMinutes.coerceAtLeast(1).toFloat())
                .coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .clickable { onOpenDashboard() }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = advice.headline,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${advice.streakDays}d streak",
                        color = EmeraldMint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice.detail,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = accentColor,
                    trackColor = DarkSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Screen: ${advice.screenUsedMinutes}m / ${advice.screenGoalMinutes}m",
                        color = TextMuted,
                        fontSize = 11.sp,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "Focus: ${advice.focusedMinutes}m",
                        color = TextMuted,
                        fontSize = 11.sp,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
                if (topLifeGoal.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$yearDaysLeft • $topLifeGoal",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Phone-Quitter Triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IvoryAction)
                        .clickable { onStartQuickFocus() }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = InkOnLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Focus 25m",
                        color = InkOnLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenDashboard() }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            travelState.isTravelActive -> "Travel Active"
                            holidayState.isHolidayActiveToday -> "Holiday (${holidayState.reelsScrolledToday} Reels)"
                            else -> "Modes & Diary"
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Single-tap Banking / UPI Accessibility Pause & Re-enable Strip
        if (!isAccessibilityEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(
                        0.75.dp,
                        if (isAccessibilityPausedForBanking) BrandPrimary.copy(alpha = 0.55f) else SignalCoral.copy(alpha = 0.55f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onEnableAccessibility() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isAccessibilityPausedForBanking) BrandPrimary else SignalCoral,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isAccessibilityPausedForBanking) "Shield Paused for UPI/Banking" else "Accessibility Shield Off",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to re-enable GetBackToWork in Settings",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(IvoryAction)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Enable",
                        color = InkOnLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else if (!shouldBlock) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldMint)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accessibility Shield On",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Pause for UPI",
                    color = BrandPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(0.5.dp, BrandPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable { onPauseAccessibilityForBanking() }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (shouldBlock) {
                    if (ultraFocusState.isTransitPassActive) "ALLOWED + TRANSIT APPS" else "ALLOWED TOOLS"
                } else {
                    "PINNED ESSENTIALS"
                },
                style = SectionHeaderLabel
            )
            if (!shouldBlock) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(10.dp))
                        .clickable { onOpenEditPinned() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Change Pinned Essentials",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Edit",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Typographic Distraction-Free App Rows + Optional Work Profile Dropdown
        if (!shouldBlock && primaryVisibleApps.isEmpty() && workDropdownApps.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenEditPinned() }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Pinned Essentials",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap Edit or the Settings icon to pick your essential home apps",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!shouldBlock && primaryVisibleApps.isEmpty()) {
                    item(key = "empty_pinned_hint") {
                        Text(
                            text = "No pinned essentials yet — tap Edit above or open Work Profile below.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(primaryVisibleApps, key = { it.uniqueKey }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = { onOpenApp(app) },
                                onLongClick = if (!shouldBlock) {
                                    { onOpenAppOptions(app) }
                                } else {
                                    null
                                }
                            )
                            .padding(horizontal = 2.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.appName,
                                color = TextPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 0.2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (app.isWorkProfile) {
                                Spacer(modifier = Modifier.width(8.dp))
                                WorkProfileBadge()
                            }
                        }
                        if (ultraFocusState.isTransitPassActive && sessionManager.isTransitEssentialApp(app.packageName)) {
                            Text(
                                text = "TRANSIT",
                                style = MicroLabelText,
                                color = SkyCyan
                            )
                        }
                    }
                }
            }
        }

        // Docked Bottom Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(HairlineBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenPhone() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = SuccessGreen,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Phone",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (shouldBlock) {
                if (isUltraFocus) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (ultraFocusState.isTransitPassActive) {
                                    sessionManager.endUltraFocusTransitMode()
                                } else {
                                    sessionManager.activateUltraFocusTransitMode(120)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Transit Pass",
                            tint = if (ultraFocusState.isTransitPassActive) SkyCyan else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (ultraFocusState.isTransitPassActive) "Transit On" else "Transit",
                            color = if (ultraFocusState.isTransitPassActive) SkyCyan else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenPassOrReality() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Emergency Pass",
                        tint = if (isUltraFocus && ultraFocusState.hasUsedEmergency30mToday) TextMuted else SkyCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUltraFocus) {
                            if (ultraFocusState.hasUsedEmergency30mToday) "30m Used" else "30m Pass"
                        } else {
                            "Emergency Pass"
                        },
                        color = if (isUltraFocus && ultraFocusState.hasUsedEmergency30mToday) TextMuted else SkyCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenCamera() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Camera",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAllApps() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Apps",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "All Apps",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalistAllAppsPage(
    allApps: List<AppInfo>,
    homePinnedPackages: Set<String>,
    onTogglePin: (AppInfo) -> Unit,
    onOpenAppOptions: (AppInfo) -> Unit,
    onBackHome: () -> Unit,
    onSelectApp: (AppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var workExpanded by remember { mutableStateOf(false) }
    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }
    val (personalApps, workApps) = remember(filteredApps) {
        AppRepository.partitionPersonalAndWorkApps(filteredApps)
    }
    val effectiveWorkExpanded = workExpanded || searchQuery.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "All Apps (${filteredApps.size})",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Hold any app or tap Pin for your home screen",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "Home",
                color = BrandPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBackHome() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            placeholder = {
                Text("Search apps deliberately", color = TextMuted, fontSize = 13.sp)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
            },
            trailingIcon = {
                AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(17.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = TextMuted,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (workApps.isNotEmpty()) {
                item(key = "all_apps_work_dropdown") {
                    WorkProfileDropdownHeader(
                        count = workApps.size,
                        expanded = effectiveWorkExpanded,
                        onToggle = { workExpanded = !workExpanded }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (effectiveWorkExpanded) {
                    items(workApps, key = { "all_work_${it.uniqueKey}" }) { app ->
                        MinimalistAllAppRow(
                            app = app,
                            isPinned = homePinnedPackages.contains(app.uniqueKey),
                            onSelectApp = { onSelectApp(app) },
                            onOpenAppOptions = { onOpenAppOptions(app) },
                            onTogglePin = { onTogglePin(app) }
                        )
                    }
                }
                item(key = "all_apps_work_spacer") {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            items(personalApps, key = { it.uniqueKey }) { app ->
                MinimalistAllAppRow(
                    app = app,
                    isPinned = homePinnedPackages.contains(app.uniqueKey) ||
                        (!app.isWorkProfile && homePinnedPackages.contains(app.packageName)),
                    onSelectApp = { onSelectApp(app) },
                    onOpenAppOptions = { onOpenAppOptions(app) },
                    onTogglePin = { onTogglePin(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalistAllAppRow(
    app: AppInfo,
    isPinned: Boolean,
    onSelectApp: () -> Unit,
    onOpenAppOptions: () -> Unit,
    onTogglePin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onSelectApp,
                onLongClick = onOpenAppOptions
            )
            .padding(horizontal = 2.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = app.appName,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (app.isWorkProfile) {
                Spacer(modifier = Modifier.width(8.dp))
                WorkProfileBadge()
            }
        }
        Text(
            text = if (isPinned) "PINNED" else "PIN",
            style = MicroLabelText,
            color = if (isPinned) EmeraldMint else TextMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onTogglePin)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MindfulAppPauseDialog(
    app: AppInfo,
    screenUsedMinutes: Int,
    screenGoalMinutes: Int,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(5) }
    LaunchedEffect(app.packageName) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                Text(
                    text = "BUDGET CHECK",
                    style = MicroLabelText,
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Open ${app.appName}?",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've used ${screenUsedMinutes}m today (cap: ${screenGoalMinutes}m). Ask yourself if this app is essential right now.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Put Phone Down",
                        color = InkOnLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onProceed,
                    enabled = secondsLeft == 0,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                ) {
                    Text(
                        text = if (secondsLeft > 0) "Pause (${secondsLeft}s)..." else "Open ${app.appName}",
                        color = if (secondsLeft > 0) TextMuted else TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
