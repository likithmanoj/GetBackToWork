package com.getbacktowork.app.ui.overlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.components.WorkProfileBadge
import com.getbacktowork.app.ui.components.WorkProfileDropdownHeader
import com.getbacktowork.app.ui.screens.UltraFocusRealityCheckDialog
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.ClockDisplayText
import com.getbacktowork.app.ui.theme.DarkBackground
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.GetBackToWorkTheme
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SectionHeaderLabel
import com.getbacktowork.app.ui.theme.SignalCoral
import com.getbacktowork.app.ui.theme.SkyCyan
import com.getbacktowork.app.ui.theme.SuccessGreen
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import com.getbacktowork.app.ui.theme.TimerDisplayText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlockingOverlayActivity : ComponentActivity() {

    private lateinit var sessionManager: FocusSessionManager
    var onNewIntentAction: (() -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onNewIntentAction?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = FocusSessionManager.getInstance(this)

        // Fallback anti-cheat back interception before Compose BackHandler attaches
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (sessionManager.shouldBlockApps()) {
                    if (sessionManager.isStrictMode() || sessionManager.isUltraFocusActive()) {
                        return
                    }
                } else {
                    finish()
                }
            }
        })

        setContent {
            GetBackToWorkTheme {
                FocusOverlayContent(
                    sessionManager = sessionManager,
                    onOpenPhone = { openPhoneApp() },
                    onOpenAllowedApp = { app -> AppRepository.launchApp(this, app) },
                    onSessionFinished = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.refreshState()
        if (!sessionManager.shouldBlockApps()) {
            finish()
        }
    }

    private fun openPhoneApp() {
        try {
            lastLaunchedAllowedTime = System.currentTimeMillis()
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_IS_SETTINGS = "extra_is_settings"

        @Volatile
        var lastLaunchedAllowedTime: Long = 0L

        @Volatile
        var lastCooldownGrantPackage: String? = null

        @Volatile
        var lastCooldownGrantTime: Long = 0L

        @Volatile
        var isCooldownPromptVisible: Boolean = false
    }
}

@Composable
fun FocusOverlayContent(
    sessionManager: FocusSessionManager,
    onOpenPhone: () -> Unit,
    onOpenAllowedApp: (AppInfo) -> Unit,
    onSessionFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by sessionManager.sessionState.collectAsState()
    val ultraFocusState by sessionManager.ultraFocusStateFlow.collectAsState()
    val allowedPackages by sessionManager.allowedPackagesFlow.collectAsState()
    val lifeGoals by sessionManager.lifeGoalsFlow.collectAsState()
    val customNames by sessionManager.customAppNamesFlow.collectAsState()
    val scheduleConfig by sessionManager.scheduleConfigFlow.collectAsState()

    var remainingMillis by remember { mutableLongStateOf(sessionManager.getRemainingMillis()) }
    var tickNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val yearProgress = remember(tickNow) { sessionManager.getYearProgressInfo() }
    var allowedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var showEmergencyPassDialog by remember { mutableStateOf(false) }
    var showRealityCheckDialog by remember { mutableStateOf(false) }

    val isUltraFocus = ultraFocusState.isEnabled && !sessionState.isActive
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    LaunchedEffect(Unit) {
        (context as? BlockingOverlayActivity)?.onNewIntentAction = {
            coroutineScope.launch { pagerState.scrollToPage(0) }
        }
    }

    BackHandler(enabled = true) {
        if (!isUltraFocus && pagerState.currentPage != 0) {
            coroutineScope.launch { pagerState.animateScrollToPage(0) }
        } else if (sessionManager.shouldBlockApps() && (sessionManager.isStrictMode() || ultraFocusState.isEnabled)) {
            // Locked inside Minimalist Launcher
        } else if (!sessionManager.shouldBlockApps()) {
            onSessionFinished()
        }
    }

    LaunchedEffect(allowedPackages, ultraFocusState.isTransitPassActive, customNames) {
        val repo = AppRepository(context)
        val rawList = if (ultraFocusState.isTransitPassActive) {
            repo.getInstalledApps()
                .filter { allowedPackages.contains(it.packageName) || sessionManager.isTransitEssentialApp(it.packageName) }
        } else {
            repo.getAppsForPackages(allowedPackages)
        }
        allowedApps = rawList
            .map {
                val resolvedName = sessionManager.resolveAppName(
                    it.uniqueKey,
                    sessionManager.resolveAppName(it.packageName, it.appName)
                )
                it.copy(appName = resolvedName)
            }
            .sortedBy { it.appName.lowercase() }
        isLoadingApps = false
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                tickNow = System.currentTimeMillis()
                remainingMillis = sessionManager.getRemainingMillis()
                sessionManager.refreshUltraFocusState()
                sessionManager.refreshScheduleState()
                if (!sessionManager.shouldBlockApps()) {
                    onSessionFinished()
                    break
                }
                delay(1000L)
            }
        }
    }

    val remaining24h = remember(tickNow, ultraFocusState) {
        sessionManager.getUltraFocus24hRemainingMillis(tickNow)
    }
    val totalSecs = if (isUltraFocus) {
        (remaining24h / 1000L).coerceAtLeast(0L)
    } else {
        (remainingMillis / 1000L).coerceAtLeast(0L)
    }
    val mins = totalSecs / 60L
    val secs = totalSecs % 60L
    val formattedTime = if (isUltraFocus) {
        val h = mins / 60L
        val m = mins % 60L
        String.format(Locale.getDefault(), "%02dh %02dm", h, m)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val progress = if (isUltraFocus) {
        val totalUltraMillis = ultraFocusState.durationHours.coerceAtLeast(1) * 60f * 60f * 1000f
        (remaining24h / totalUltraMillis).coerceIn(0f, 1f)
    } else if (sessionState.totalDurationMinutes > 0) {
        val totalMillis = sessionState.totalDurationMinutes * 60 * 1000f
        (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
    } else {
        1f
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (isUltraFocus) {
                // In Cold Turkey mode, the pure Minimalist Launcher is the entire home experience (no Focus Timer page needed)
                    MinimalistLauncherPage(
                        allowedApps = allowedApps,
                        formattedTime = formattedTime,
                        tickNow = tickNow,
                        isUltraFocus = true,
                        isTransitPassActive = ultraFocusState.isTransitPassActive,
                        hasUsedEmergency30mToday = ultraFocusState.hasUsedEmergency30mToday,
                        yearDaysLeft = "${yearProgress.daysRemaining}d left in ${yearProgress.year}",
                        topLifeGoal = lifeGoals.firstOrNull().orEmpty(),
                        scheduleLabel = if (!sessionState.isActive && !ultraFocusState.isEnabled && (scheduleConfig.isWorkActiveNow || scheduleConfig.isSleepActiveNow)) "Schedule" else null,
                        onToggleTransitPass = {
                            if (ultraFocusState.isTransitPassActive) {
                                sessionManager.endUltraFocusTransitMode()
                            } else {
                                sessionManager.activateUltraFocusTransitMode(120)
                            }
                        },
                        onOpenAllowedApp = onOpenAllowedApp,
                        onOpenPhone = onOpenPhone,
                        onOpenRealityCheck = { showRealityCheckDialog = true },
                        onSwipeToTimer = null
                    )
            } else {
                // Timed Focus Session: Minimalist Launcher (default Page 0) + Focus Timer (Page 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurface)
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val page0Active = pagerState.currentPage == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (page0Active) DarkSurfaceElevated else Color.Transparent)
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Minimalist Launcher",
                            color = if (page0Active) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (page0Active) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }

                    val page1Active = pagerState.currentPage == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (page1Active) DarkSurfaceElevated else Color.Transparent)
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Focus Timer",
                            color = if (page1Active) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (page1Active) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    if (page == 0) {
                        MinimalistLauncherPage(
                            allowedApps = allowedApps,
                            formattedTime = if (sessionState.isActive) formattedTime else "Scheduled",
                            tickNow = tickNow,
                            isUltraFocus = false,
                            isTransitPassActive = false,
                            hasUsedEmergency30mToday = false,
                            yearDaysLeft = "${yearProgress.daysRemaining}d left in ${yearProgress.year}",
                            topLifeGoal = lifeGoals.firstOrNull().orEmpty(),
                            scheduleLabel = if (!sessionState.isActive && (scheduleConfig.isWorkActiveNow || scheduleConfig.isSleepActiveNow)) {
                                if (scheduleConfig.isSleepActiveNow) "Sleep Wind-Down Active" else "Work Schedule Active"
                            } else null,
                            onToggleTransitPass = {},
                            onOpenAllowedApp = onOpenAllowedApp,
                            onOpenPhone = onOpenPhone,
                            onOpenRealityCheck = {},
                            onSwipeToTimer = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                        )
                    } else {
                        FocusTimerPage(
                            formattedTime = formattedTime,
                            progress = progress,
                            allowedApps = allowedApps,
                            isLoadingApps = isLoadingApps,
                            sessionManager = sessionManager,
                            onOpenPhone = onOpenPhone,
                            onOpenAllowedApp = onOpenAllowedApp,
                            onOpenPassDialog = { showEmergencyPassDialog = true },
                            onSwipeToLauncher = { coroutineScope.launch { pagerState.animateScrollToPage(0) } }
                        )
                    }
                }
            }
        }

        if (showEmergencyPassDialog) {
            EmergencyPassDialog(
                sessionManager = sessionManager,
                onDismiss = { showEmergencyPassDialog = false },
                onPassActivated = {
                    showEmergencyPassDialog = false
                    onSessionFinished()
                }
            )
        }

        if (showRealityCheckDialog) {
            UltraFocusRealityCheckDialog(
                sessionManager = sessionManager,
                onDismiss = { showRealityCheckDialog = false },
                onTravelPassActivated = {
                    showRealityCheckDialog = false
                    onSessionFinished()
                }
            )
        }
    }
}

@Composable
private fun FocusTimerPage(
    formattedTime: String,
    progress: Float,
    allowedApps: List<AppInfo>,
    isLoadingApps: Boolean,
    sessionManager: FocusSessionManager,
    onOpenPhone: () -> Unit,
    onOpenAllowedApp: (AppInfo) -> Unit,
    onOpenPassDialog: () -> Unit,
    onSwipeToLauncher: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(BrandPrimary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = BrandPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GET BACK TO WORK",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HeroOverlayDial(
            progress = progress,
            timeText = formattedTime,
            subLabel = "Time Remaining",
            accentColor = BrandPrimary
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenPhone,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = SuccessGreen,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Phone Calls",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            val remainingPasses = sessionManager.getRemainingEmergencyPasses()
            OutlinedButton(
                onClick = onOpenPassDialog,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Emergency Pass",
                    tint = if (remainingPasses > 0) BrandPrimary else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pass ($remainingPasses left)",
                    color = if (remainingPasses > 0) TextPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ALLOWED APPS (${allowedApps.size})",
                style = SectionHeaderLabel
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSwipeToLauncher() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Launcher",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (allowedApps.isEmpty() && !isLoadingApps) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Deep Focus Mode",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No apps whitelisted. Set your phone down and execute.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allowedApps, key = { it.uniqueKey }) { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                            .clickable { onOpenAllowedApp(app) }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (app.iconBitmap != null) {
                            Image(
                                bitmap = app.iconBitmap,
                                contentDescription = app.appName,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        if (app.isWorkProfile) {
                            Spacer(modifier = Modifier.height(3.dp))
                            WorkProfileBadge()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun HeroOverlayDial(
    progress: Float,
    timeText: String,
    subLabel: String,
    accentColor: Color = BrandPrimary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "heroOverlayDialProgress"
    )

    Box(
        modifier = modifier.size(226.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 7.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            val tickCount = 24
            val innerR = radius - strokeWidth / 2f - 8.dp.toPx()
            val outerR = radius - strokeWidth / 2f - 4.dp.toPx()
            for (i in 0 until tickCount) {
                val angleRad = Math.toRadians((i * (360f / tickCount)) - 90.0)
                val isQuarter = i % 6 == 0
                drawLine(
                    color = Color.White.copy(alpha = if (isQuarter) 0.25f else 0.08f),
                    start = Offset(
                        center.x + (innerR * Math.cos(angleRad)).toFloat(),
                        center.y + (innerR * Math.sin(angleRad)).toFloat()
                    ),
                    end = Offset(
                        center.x + (outerR * Math.cos(angleRad)).toFloat(),
                        center.y + (outerR * Math.sin(angleRad)).toFloat()
                    ),
                    strokeWidth = if (isQuarter) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = DarkSurfaceVariant,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = (360f * animatedProgress).coerceIn(1f, 360f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = timeText,
                style = TimerDisplayText,
                fontSize = if (timeText.length > 5) 36.sp else 52.sp,
                lineHeight = if (timeText.length > 5) 40.sp else 56.sp,
                maxLines = 1,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel.uppercase(),
                style = MicroLabelText,
                color = accentColor
            )
        }
    }
}

/**
 * Serene Minimalist Phone Launcher:
 * Acts as the primary distraction-free phone home during Focus and Cold Turkey sessions.
 */
@Composable
private fun MinimalistLauncherPage(
    allowedApps: List<AppInfo>,
    formattedTime: String,
    tickNow: Long,
    isUltraFocus: Boolean,
    isTransitPassActive: Boolean,
    hasUsedEmergency30mToday: Boolean,
    yearDaysLeft: String,
    topLifeGoal: String,
    scheduleLabel: String?,
    onToggleTransitPass: () -> Unit,
    onOpenAllowedApp: (AppInfo) -> Unit,
    onOpenPhone: () -> Unit,
    onOpenRealityCheck: () -> Unit,
    onSwipeToTimer: (() -> Unit)?
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var workExpanded by remember { mutableStateOf(false) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val currentTime = remember(tickNow) { timeFormatter.format(Date(tickNow)) }
    val currentDate = remember(tickNow) { dateFormatter.format(Date(tickNow)) }

    val filteredApps = remember(searchQuery, allowedApps) {
        if (searchQuery.isBlank()) {
            allowedApps.sortedBy { it.appName.lowercase() }
        } else {
            allowedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
                .sortedBy { it.appName.lowercase() }
        }
    }
    val (personalAllowed, workAllowed) = remember(filteredApps) {
        AppRepository.partitionPersonalAndWorkApps(filteredApps)
    }
    val effectiveWorkExpanded = workExpanded || searchQuery.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Header: Crisp Clock & Date on Left, Search Toggle on Right
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

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSearchOpen) DarkSurfaceElevated else DarkSurface)
                    .border(0.75.dp, HairlineBorder, CircleShape)
                    .clickable {
                        isSearchOpen = !isSearchOpen
                        if (!isSearchOpen) searchQuery = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Session / Cold Turkey Status Line
        val badgeColor = if (isUltraFocus) SignalCoral else BrandPrimary
        val badgeText = when {
            isUltraFocus && isTransitPassActive -> "Cold Turkey + Transit • $formattedTime left"
            isUltraFocus -> "Cold Turkey • $formattedTime left"
            scheduleLabel != null -> scheduleLabel
            else -> "Focus • $formattedTime remaining"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badgeText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
            Text(
                text = yearDaysLeft,
                color = TextMuted,
                fontSize = 11.sp,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }

        if (topLifeGoal.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Focus Priority: $topLifeGoal",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(visible = isSearchOpen) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    placeholder = {
                        Text(
                            text = "Filter allowed apps",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
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
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Distraction-Free Typographic App List
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (allowedApps.isEmpty()) "No apps whitelisted.\nPut the phone down and focus."
                    else "No matching apps.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (workAllowed.isNotEmpty()) {
                    item(key = "work_dropdown_focus") {
                        WorkProfileDropdownHeader(
                            count = workAllowed.size,
                            expanded = effectiveWorkExpanded,
                            onToggle = { workExpanded = !workExpanded }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (effectiveWorkExpanded) {
                        items(workAllowed, key = { "work_lock_${it.uniqueKey}" }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenAllowedApp(app) }
                                    .padding(horizontal = 2.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.appName,
                                    color = TextPrimary,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 0.2.sp
                                )
                                WorkProfileBadge()
                            }
                        }
                    }
                    item(key = "work_dropdown_spacer") {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                items(personalAllowed, key = { it.uniqueKey }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenAllowedApp(app) }
                            .padding(horizontal = 2.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isUltraFocus) {
            // Quiet docked utility bar in Cold Turkey mode (no Focus Timer page needed)
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

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleTransitPass() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBus,
                        contentDescription = "Transit Lifeline",
                        tint = if (isTransitPassActive) SkyCyan else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isTransitPassActive) "Transit On" else "Transit",
                        color = if (isTransitPassActive) SkyCyan else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenRealityCheck() }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Emergency 30m Pass",
                        tint = if (hasUsedEmergency30mToday) TextMuted else SkyCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasUsedEmergency30mToday) "30m Used" else "30m Pass",
                        color = if (hasUsedEmergency30mToday) TextMuted else SkyCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (onSwipeToTimer != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwipeToTimer() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Swipe left for Focus Timer & Pass",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
