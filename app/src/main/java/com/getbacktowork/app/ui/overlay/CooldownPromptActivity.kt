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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.theme.BrandPrimary
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

/**
 * ScreenZen-style Mindful App Opening Cooldown:
 * Inserts an intentional breathing wait (10-30s) paired with the user's personal goals
 * and year countdown before opening distraction-prone apps in everyday usage.
 */
class CooldownPromptActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
    }

    private lateinit var sessionManager: FocusSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = FocusSessionManager.getInstance(this)
        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()

        val rawLabel = try {
            val info = packageManager.getApplicationInfo(targetPackage, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            targetPackage.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
        val appName = sessionManager.resolveAppName(targetPackage, rawLabel)
        val todayUsage = sessionManager.getAppTodayUsage(targetPackage)
        val dailyCap = sessionManager.getAppDailyCap(targetPackage)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })

        setContent {
            GetBackToWorkTheme {
                CooldownPromptContent(
                    appName = appName,
                    waitSeconds = sessionManager.getCooldownSeconds().coerceAtLeast(5),
                    opensToday = todayUsage.opens,
                    minutesToday = todayUsage.minutes,
                    dailyCap = dailyCap,
                    yearProgress = sessionManager.getYearProgressInfo(),
                    lifeGoals = sessionManager.getLifeGoals(),
                    onPutPhoneDown = { goHome() },
                    onContinueToApp = { chosenMinutes, intention ->
                        if (intention.isNotBlank()) {
                            sessionManager.saveReflection("$appName (${chosenMinutes}m): ${intention.trim()}")
                        }
                        sessionManager.recordAppOpen(targetPackage)
                        sessionManager.grantCooldownWindow(targetPackage, chosenMinutes)
                        BlockingOverlayActivity.lastCooldownGrantPackage = targetPackage
                        BlockingOverlayActivity.lastCooldownGrantTime = System.currentTimeMillis()
                        BlockingOverlayActivity.lastLaunchedAllowedTime = System.currentTimeMillis()
                        try {
                            val launch = packageManager.getLaunchIntentForPackage(targetPackage)
                            if (launch != null) {
                                launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(launch)
                            }
                        } catch (_: Exception) {
                        }
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BlockingOverlayActivity.isCooldownPromptVisible = true
    }

    override fun onPause() {
        super.onPause()
        BlockingOverlayActivity.isCooldownPromptVisible = false
    }

    override fun onDestroy() {
        BlockingOverlayActivity.isCooldownPromptVisible = false
        super.onDestroy()
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
private fun CooldownPromptContent(
    appName: String,
    waitSeconds: Int,
    opensToday: Int,
    minutesToday: Int,
    dailyCap: FocusSessionManager.AppDailyCap?,
    yearProgress: FocusSessionManager.YearProgressInfo,
    lifeGoals: List<String>,
    onPutPhoneDown: () -> Unit,
    onContinueToApp: (Int, String) -> Unit
) {
    var secondsLeft by remember(waitSeconds) { mutableIntStateOf(waitSeconds) }
    var selectedMinutes by remember { mutableIntStateOf(5) }
    var intentionNote by remember { mutableStateOf("") }
    val isReady = secondsLeft <= 0

    LaunchedEffect(waitSeconds) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cooldownPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cooldownScale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MINDFUL PAUSE",
                style = MicroLabelText,
                color = EmeraldMint
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Is $appName intentional?",
                color = TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            val capSummary = buildString {
                append("Open #${opensToday + 1} today")
                if (minutesToday > 0) append(" · ${minutesToday}m used")
                if (dailyCap != null && dailyCap.maxOpensPerDay > 0) {
                    append(" · Max ${dailyCap.maxOpensPerDay}/d")
                } else if (dailyCap != null && dailyCap.maxMinutesPerDay > 0) {
                    append(" · Cap ${dailyCap.maxMinutesPerDay}m/d")
                }
            }
            Text(
                text = capSummary,
                color = if (opensToday >= 4) BrandPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(134.dp)
                        .scale(if (isReady) 1f else pulseScale)
                        .background(EmeraldMint.copy(alpha = 0.10f), CircleShape)
                )
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val stroke = 5.dp.toPx()
                    drawCircle(color = DarkSurfaceElevated, style = Stroke(width = stroke))
                    val progress = (waitSeconds - secondsLeft).toFloat() / waitSeconds.coerceAtLeast(1).toFloat()
                    if (progress > 0f) {
                        drawArc(
                            color = if (isReady) SuccessGreen else EmeraldMint,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isReady) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Ready",
                            tint = SuccessGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Text(
                            text = "${secondsLeft}s",
                            color = TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Year Progress & Top Personal Goals anchor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${yearProgress.daysRemaining} DAYS LEFT IN ${yearProgress.year}",
                        style = MicroLabelText,
                        color = BrandPrimary
                    )
                    Text(
                        text = "${yearProgress.elapsedPercent}% elapsed",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (yearProgress.elapsedPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = BrandPrimary,
                    trackColor = DarkSurfaceVariant
                )

                if (lifeGoals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "REMEMBER YOUR PRIORITIES",
                        style = MicroLabelText,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    lifeGoals.forEach { goal ->
                        Text(
                            text = "• $goal",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intentional time budget & quick reason before opening
            Text(
                text = "CHOOSE WINDOW DURATION",
                style = MicroLabelText,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2 to "2m Reply", 5 to "5m Task", 10 to "10m Browse").forEach { (mins, label) ->
                    val chosen = selectedMinutes == mins
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chosen) IvoryAction else DarkSurface)
                            .border(0.75.dp, HairlineBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedMinutes = mins }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (chosen) InkOnLight else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = intentionNote,
                onValueChange = { intentionNote = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("Optional intention (e.g. reply to Sam)", color = TextMuted, fontSize = 12.sp)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = TextMuted,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPutPhoneDown,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NaturePeople,
                    contentDescription = null,
                    tint = InkOnLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Put Phone Down",
                    color = InkOnLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onContinueToApp(selectedMinutes, intentionNote) },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DarkSurface,
                    disabledContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Text(
                    text = if (isReady) "Open $appName (${selectedMinutes}m)" else "Breathe (${secondsLeft}s)...",
                    color = if (isReady) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
