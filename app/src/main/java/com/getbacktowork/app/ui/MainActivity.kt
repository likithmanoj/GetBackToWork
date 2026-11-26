package com.getbacktowork.app.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.service.AdminReceiver
import com.getbacktowork.app.service.AppBlockerAccessibilityService
import com.getbacktowork.app.ui.screens.DiaryScreen
import com.getbacktowork.app.ui.screens.PermissionsScreen
import com.getbacktowork.app.ui.screens.TimerScreen
import com.getbacktowork.app.ui.screens.WhitelistScreen
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
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
    }

    private lateinit var sessionManager: FocusSessionManager
    private var requestedTabState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = FocusSessionManager.getInstance(this)
        val initialTab = intent?.getIntExtra(EXTRA_OPEN_TAB, 0)?.coerceIn(0, 3) ?: 0
        requestedTabState.intValue = initialTab

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        if (intent?.action == "ACTION_DEACTIVATE_ADMIN" || intent?.getBooleanExtra("deactivate_admin", false) == true) {
            // Only allow admin removal when whitelist / strict lock is NOT enforced
            if (!sessionManager.isWhitelistLocked()) {
                dpm?.removeActiveAdmin(AdminReceiver.getComponentName(this))
            }
            finish()
            return
        }

        setContent {
            GetBackToWorkTheme {
                MainAppScaffold(
                    sessionManager = sessionManager,
                    requestedTab = requestedTabState.intValue
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra(EXTRA_OPEN_TAB)) {
            requestedTabState.intValue = intent.getIntExtra(EXTRA_OPEN_TAB, 0).coerceIn(0, 3)
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.refreshState()
    }
}

@Composable
fun MainAppScaffold(
    sessionManager: FocusSessionManager,
    requestedTab: Int = 0
) {
    var selectedTab by remember { mutableIntStateOf(requestedTab) }
    androidx.compose.runtime.LaunchedEffect(requestedTab) {
        if (requestedTab in 0..3 && requestedTab != selectedTab) {
            selectedTab = requestedTab
        }
    }
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var strictMode by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(sessionManager.shouldShowOnboarding()) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 78.dp)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180, easing = LinearEasing)) +
                            slideIntoContainer(
                                if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left
                                else AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )) togetherWith
                            (fadeOut(animationSpec = tween(140, easing = LinearEasing)) +
                                    slideOutOfContainer(
                                        if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left
                                        else AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ))
                },
                label = "mainTabTransition"
            ) { tab ->
                when (tab) {
                    0 -> TimerScreen(
                        sessionManager = sessionManager,
                        onNavigateToWhitelist = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 1
                        },
                        onNavigateToDiary = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 2
                        },
                        selectedMinutes = selectedMinutes,
                        onSelectedMinutesChange = { selectedMinutes = it },
                        strictMode = strictMode,
                        onStrictModeChange = { strictMode = it }
                    )
                    1 -> WhitelistScreen(
                        sessionManager = sessionManager
                    )
                    2 -> DiaryScreen(
                        sessionManager = sessionManager,
                        onNavigateToFocus = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = 0
                        }
                    )
                    3 -> PermissionsScreen(
                        sessionManager = sessionManager,
                        onReplayOnboarding = { showOnboarding = true }
                    )
                }
            }
        }

        // Fixed equal-slot floating dock: zero sideways layout jitter when switching tabs
        FloatingPillDock(
            selectedTab = selectedTab,
            onSelectTab = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selectedTab = it
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }

    if (showOnboarding) {
        FirstRunOnboardingDialog(
            sessionManager = sessionManager,
            onGoToShield = {
                showOnboarding = false
                selectedTab = 3
            },
            onDismiss = {
                sessionManager.setOnboardingCompleted(true)
                showOnboarding = false
            }
        )
    }
}

@Composable
private fun FirstRunOnboardingDialog(
    sessionManager: FocusSessionManager,
    onGoToShield: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var selectedGoalMins by remember { mutableIntStateOf(sessionManager.getDailyScreenGoalMinutes()) }
    var priorityInput by remember { mutableStateOf(sessionManager.getLifeGoals().firstOrNull().orEmpty()) }

    val hasAccessibility = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context)
    val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(24.dp)),
            color = DarkSurface
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETUP STEP ${step + 1} OF 3",
                        style = MicroLabelText,
                        color = EmeraldMint
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = TextMuted, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                when (step) {
                    0 -> {
                        Text(
                            text = "Take Back Your Attention",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "100% offline. Zero cloud tracking or subscriptions. Pick your daily phone cap & #1 goal:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30 to "30m", 45 to "45m", 60 to "<1h", 90 to "90m").forEach { (mins, label) ->
                                val selected = selectedGoalMins == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) IvoryAction else DarkSurfaceVariant)
                                        .clickable {
                                            selectedGoalMins = mins
                                            sessionManager.setDailyScreenGoalMinutes(mins)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) InkOnLight else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = priorityInput,
                            onValueChange = { priorityInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Your #1 focus priority", color = TextMuted, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedBorderColor = TextMuted,
                                unfocusedBorderColor = HairlineBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                if (priorityInput.isNotBlank()) {
                                    val rest = sessionManager.getLifeGoals().drop(1)
                                    sessionManager.setLifeGoals(listOf(priorityInput) + rest)
                                }
                                step = 1
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Continue", color = InkOnLight, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    1 -> {
                        Text(
                            text = "Enable Local Shield",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "App blocking and Reels/Video counters run entirely on-device via Accessibility & Overlay.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (hasAccessibility) "✓ Accessibility Service Active" else "• Accessibility Service Required",
                            color = if (hasAccessibility) EmeraldMint else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasOverlay) "✓ Overlay Permission Active" else "• Display Over Other Apps Required",
                            color = if (hasOverlay) EmeraldMint else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!hasAccessibility || !hasOverlay) {
                            OutlinedButton(
                                onClick = {
                                    sessionManager.setOnboardingCompleted(true)
                                    onGoToShield()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                            ) {
                                Text("Open Shield Permissions", color = EmeraldMint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Next: Homescreen", color = InkOnLight, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    else -> {
                        Text(
                            text = "Minimalist Homescreen",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Replace distracting app grids with clean typography, daily priorities, and mindful pauses—even when no timer is active.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                        ) {
                            Text("Choose Default Home App", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Start Focusing", color = InkOnLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingPillDock(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(DarkSurface)
            .border(0.75.dp, HairlineBorder, RoundedCornerShape(30.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem(
            selected = selectedTab == 0,
            icon = Icons.Default.HourglassTop,
            label = "Focus",
            onClick = { onSelectTab(0) },
            modifier = Modifier.weight(1f)
        )
        DockItem(
            selected = selectedTab == 1,
            icon = Icons.Default.Apps,
            label = "Apps",
            onClick = { onSelectTab(1) },
            modifier = Modifier.weight(1f)
        )
        DockItem(
            selected = selectedTab == 2,
            icon = Icons.Default.Book,
            label = "Diary",
            onClick = { onSelectTab(2) },
            modifier = Modifier.weight(1f)
        )
        DockItem(
            selected = selectedTab == 3,
            icon = Icons.Default.Shield,
            label = "Shield",
            onClick = { onSelectTab(3) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (selected) DarkSurfaceElevated else Color.Transparent,
        animationSpec = tween(180),
        label = "dockBg"
    )
    val contentTint by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextMuted,
        animationSpec = tween(180),
        label = "dockTint"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = contentTint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
