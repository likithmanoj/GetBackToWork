package com.getbacktowork.app.ui.overlay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.getbacktowork.app.data.FocusSessionManager
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
import com.getbacktowork.app.ui.theme.SuccessGreen
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

class HolidayInterrupterActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CHECKIN_MODE = "extra_checkin_mode"
        const val EXTRA_CAP_APP_NAME = "extra_cap_app_name"
        const val EXTRA_CAP_REASON = "extra_cap_reason"
        const val MODE_HOLIDAY = "HOLIDAY"
        const val MODE_TRAVEL = "TRAVEL"
        const val MODE_DAILY_LIMIT = "DAILY_LIMIT"
        const val MODE_APP_CAP = "APP_CAP"
    }

    private lateinit var sessionManager: FocusSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = FocusSessionManager.getInstance(this)
        val checkInMode = intent?.getStringExtra(EXTRA_CHECKIN_MODE) ?: MODE_HOLIDAY
        val capAppName = intent?.getStringExtra(EXTRA_CAP_APP_NAME).orEmpty()
        val capReason = intent?.getStringExtra(EXTRA_CAP_REASON).orEmpty()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (checkInMode == MODE_APP_CAP) {
                    goHome()
                } else {
                    sessionManager.snoozeHolidayReminder(30)
                    finish()
                }
            }
        })

        setContent {
            GetBackToWorkTheme {
                HolidayInterrupterContent(
                    sessionManager = sessionManager,
                    checkInMode = checkInMode,
                    capAppName = capAppName,
                    capReason = capReason,
                    onGoHome = { goHome() },
                    onDismiss = {
                        sessionManager.snoozeHolidayReminder(30)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun goHome() {
        sessionManager.snoozeHolidayReminder(30)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
private fun HolidayInterrupterContent(
    sessionManager: FocusSessionManager,
    checkInMode: String,
    capAppName: String,
    capReason: String,
    onGoHome: () -> Unit,
    onDismiss: () -> Unit
) {
    val holidayState by sessionManager.holidayStateFlow.collectAsState()
    var showBreathingDialog by remember { mutableStateOf(false) }
    var showCapUnlockPuzzleDialog by remember { mutableStateOf(false) }
    val todayDiary = remember(checkInMode) { sessionManager.getTodayDiaryEntry() }
    val dailyGoal = sessionManager.getDailyScreenGoalMinutes()

    val eyebrowText = when (checkInMode) {
        HolidayInterrupterActivity.MODE_TRAVEL -> "TRAVEL CHECK-IN"
        HolidayInterrupterActivity.MODE_DAILY_LIMIT -> "DAILY <1H ADVISOR"
        HolidayInterrupterActivity.MODE_APP_CAP -> "DAILY APP LIMIT REACHED"
        else -> "HOLIDAY CHECK-IN"
    }
    val headlineText = when (checkInMode) {
        HolidayInterrupterActivity.MODE_TRAVEL -> "There’s a Journey Outside"
        HolidayInterrupterActivity.MODE_DAILY_LIMIT -> "Reclaim Focus Under 1 Hr"
        HolidayInterrupterActivity.MODE_APP_CAP -> "${capAppName.ifEmpty { "App" }} Budget Full"
        else -> "Step Outside This Screen"
    }
    val quoteText = when (checkInMode) {
        HolidayInterrupterActivity.MODE_TRAVEL ->
            "“You’re exploring the real world right now—don’t lose the trip inside short feeds.”"
        HolidayInterrupterActivity.MODE_DAILY_LIMIT ->
            "“Your daily phone budget is full. Put the glass down and finish what actually moves your life forward.”"
        HolidayInterrupterActivity.MODE_APP_CAP ->
            "“You hit the daily limit you chose for ${capAppName.ifEmpty { "this app" }}. Protect the rest of your day.”"
        else ->
            "“Today could be a holiday—spend it with the world outside this screen.”"
    }
    val subQuoteText = when (checkInMode) {
        HolidayInterrupterActivity.MODE_TRAVEL ->
            "Keep Maps, rides, tickets, and payments ready—step away from infinite feeds until you reach your destination."
        HolidayInterrupterActivity.MODE_DAILY_LIMIT ->
            "Close infinite feeds or lock in a focused block so screen time stays below ${dailyGoal}m today."
        HolidayInterrupterActivity.MODE_APP_CAP ->
            capReason.ifEmpty { "Your per-app cap resets automatically at midnight." }
        else ->
            "Go for a brief walk, look at the sky, or talk to someone in person instead of feeding the endless scroll."
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(EmeraldMint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Step Outside",
                    tint = EmeraldMint,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = eyebrowText,
                style = MicroLabelText,
                color = EmeraldMint
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = headlineText,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = quoteText,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subQuoteText,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isReels = holidayState.limitType == FocusSessionManager.LIMIT_TYPE_REELS
            val statLabel = when (checkInMode) {
                HolidayInterrupterActivity.MODE_TRAVEL -> "Travel Reels & Screen"
                HolidayInterrupterActivity.MODE_DAILY_LIMIT -> "Screen Time Today"
                HolidayInterrupterActivity.MODE_APP_CAP -> "Daily Cap Enforced"
                else -> if (isReels) "Reels watched today" else "Leisure time used"
            }
            val statValue = when (checkInMode) {
                HolidayInterrupterActivity.MODE_TRAVEL ->
                    "${sessionManager.getTravelReelsScrolled()} Reels · ${sessionManager.getTravelScreenMinutesUsed()}m"
                HolidayInterrupterActivity.MODE_DAILY_LIMIT ->
                    "${todayDiary.screenMinutes}m / ${dailyGoal}m"
                HolidayInterrupterActivity.MODE_APP_CAP ->
                    capReason.ifEmpty { "Cap Reached" }
                else -> if (isReels) "${holidayState.reelsScrolledToday} / ${holidayState.limitValue} Reels"
                else "${holidayState.minutesUsedToday} / ${holidayState.limitValue} Mins"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statLabel,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = statValue,
                    color = EmeraldMint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NaturePeople,
                    contentDescription = null,
                    tint = InkOnLight,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Put Phone Down",
                    color = InkOnLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (checkInMode == HolidayInterrupterActivity.MODE_HOLIDAY) {
                OutlinedButton(
                    onClick = { showBreathingDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isReels) "15s Pause to Add 25 Reels" else "15s Pause to Add 15 Mins",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            if (checkInMode == HolidayInterrupterActivity.MODE_APP_CAP) {
                OutlinedButton(
                    onClick = { showCapUnlockPuzzleDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                ) {
                    Text(
                        text = "Earn +5m via Chess / Math Puzzle",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        sessionManager.snoozeHolidayReminder(30)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Acknowledge & Snooze 30m",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (showCapUnlockPuzzleDialog) {
            Dialog(
                onDismissRequest = { showCapUnlockPuzzleDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(22.dp))
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(22.dp)),
                    color = DarkSurface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        MentalClarityPuzzleSection(
                            onComplete = { reason ->
                                sessionManager.saveReflection("$capAppName daily cap earned pass: $reason")
                                sessionManager.grantEarnedScrollMinutes(5)
                                showCapUnlockPuzzleDialog = false
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }

        if (showBreathingDialog) {
            HolidayBreathingDialog(
                onDismiss = { showBreathingDialog = false },
                onExtensionGranted = {
                    val addAmount = if (holidayState.limitType == FocusSessionManager.LIMIT_TYPE_REELS) 25 else 15
                    sessionManager.grantHolidayExtension(addAmount)
                    showBreathingDialog = false
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun HolidayBreathingDialog(
    onDismiss: () -> Unit,
    onExtensionGranted: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(15) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        isFinished = true
    }

    val breathPhase = when ((15 - secondsLeft) % 12) {
        in 0..3 -> "Inhale slowly"
        in 4..7 -> "Hold gently"
        else -> "Exhale completely"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = false) {},
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "15-SECOND PAUSE",
                        style = MicroLabelText,
                        color = EmeraldMint
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isFinished) "Mind Cleared" else breathPhase,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .scale(if (isFinished) 1f else pulseScale)
                                .background(EmeraldMint.copy(alpha = 0.08f), CircleShape)
                        )

                        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            val strokeWidth = 6.dp.toPx()
                            drawCircle(
                                color = DarkSurfaceElevated,
                                style = Stroke(width = strokeWidth)
                            )
                            val progress = (15 - secondsLeft) / 15f
                            if (progress > 0f) {
                                drawArc(
                                    color = if (isFinished) SuccessGreen else EmeraldMint,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        if (isFinished) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Text(
                                text = "${secondsLeft}s",
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onExtensionGranted,
                        enabled = isFinished,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IvoryAction,
                            disabledContainerColor = DarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isFinished) "Grant Extension" else "Breathe (${secondsLeft}s)",
                            color = if (isFinished) InkOnLight else TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
