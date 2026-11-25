package com.getbacktowork.app.ui.overlay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.EmeraldMint
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.InkOnLight
import com.getbacktowork.app.ui.theme.IvoryAction
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SuccessGreen
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun EmergencyPassDialog(
    sessionManager: FocusSessionManager,
    onDismiss: () -> Unit,
    onPassActivated: () -> Unit
) {
    val remainingPasses = sessionManager.getRemainingEmergencyPasses()
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .background(DarkSurfaceElevated, CircleShape)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DarkSurfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Emergency Pass",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$remainingPasses of 2 passes remaining today",
                                    color = if (remainingPasses > 0) EmeraldMint else TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (remainingPasses <= 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Daily Limit Reached",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You have used both daily emergency passes. Finish this block before returning to other apps.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        // Segmented 4-tab selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            listOf(
                                Triple(0, Icons.Default.Spa, "Breathe"),
                                Triple(1, Icons.Default.Create, "Words"),
                                Triple(2, Icons.Default.FitnessCenter, "Squats"),
                                Triple(3, Icons.Default.Psychology, "Puzzle")
                            ).forEach { (idx, icon, label) ->
                                val active = selectedTab == idx
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (active) IvoryAction else Color.Transparent)
                                        .clickable { selectedTab = idx },
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (active) InkOnLight else TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = label,
                                        color = if (active) InkOnLight else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        when (selectedTab) {
                            0 -> BreathingCooldownSection(
                                onComplete = {
                                    sessionManager.saveReflection("Completed 60-second physiological box-breathing cooldown")
                                    sessionManager.activateEmergencyPass(5)
                                    onPassActivated()
                                    onDismiss()
                                }
                            )
                            1 -> IntentionReflectionSection(
                                onComplete = { reason ->
                                    sessionManager.saveReflection(reason)
                                    sessionManager.activateEmergencyPass(5)
                                    onPassActivated()
                                    onDismiss()
                                }
                            )
                            2 -> PhysicalWorkoutSection(
                                onComplete = {
                                    sessionManager.saveReflection("Completed 15 physical squats to reset dopamine and earn emergency pass")
                                    sessionManager.activateEmergencyPass(5)
                                    onPassActivated()
                                    onDismiss()
                                }
                            )
                            3 -> MentalClarityPuzzleSection(
                                onComplete = { summary ->
                                    sessionManager.saveReflection(summary)
                                    sessionManager.activateEmergencyPass(5)
                                    onPassActivated()
                                    onDismiss()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun BreathingCooldownSection(
    onComplete: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(60) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
        isFinished = true
    }

    val cycleSecond = (60 - secondsLeft) % 12
    val breathPhase = when {
        cycleSecond < 4 -> "Inhale slowly"
        cycleSecond < 8 -> "Hold gently"
        else -> "Exhale completely"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PHYSIOLOGICAL COOLING",
            style = MicroLabelText,
            color = EmeraldMint
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isFinished) "Urge Dissolved" else breathPhase,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(22.dp))

        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .scale(if (isFinished) 1f else pulseScale)
                    .background(EmeraldMint.copy(alpha = 0.08f), CircleShape)
            )

            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val strokeWidth = 6.dp.toPx()
                drawCircle(
                    color = DarkSurfaceElevated,
                    style = Stroke(width = strokeWidth)
                )
                val progress = (60 - secondsLeft) / 60f
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isFinished) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = SuccessGreen,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Text(
                        text = "${secondsLeft}s",
                        color = TextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "REMAINING",
                        style = MicroLabelText,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Impulses crest and fade within 60 seconds when observed calmly.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onComplete,
            enabled = isFinished,
            colors = ButtonDefaults.buttonColors(
                containerColor = IvoryAction,
                disabledContainerColor = DarkSurfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (isFinished) "Unlock 5-Minute Pass" else "Breathe (${secondsLeft}s remaining)",
                color = if (isFinished) InkOnLight else TextMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun IntentionReflectionSection(
    onComplete: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var pasteWarning by remember { mutableStateOf(false) }

    val words = remember(text) {
        text.trim().split(Regex("[\\s,;.:!?]+")).filter { it.isNotBlank() }
    }
    val wordCount = words.size
    val uniqueWordCount = words.map { it.lowercase() }.toSet().size
    val avgLength = if (words.isNotEmpty()) words.sumOf { it.length }.toFloat() / words.size else 0f

    val has50Words = wordCount >= 50
    val hasUniqueWords = uniqueWordCount >= 20
    val hasAvgLength = avgLength >= 3.5f
    val isComplete = has50Words && hasUniqueWords && hasAvgLength

    val progress = (wordCount / 50f).coerceIn(0f, 1f)

    val buttonText = when {
        isComplete -> "Save Intention & Unlock (5 Min)"
        wordCount < 50 -> "Write ${50 - wordCount} more words"
        else -> "Use 20+ distinct words (avg 3.5+ chars)"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "INTENTION GATE",
            style = MicroLabelText,
            color = BrandPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "State your specific reason",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Write 50+ meaningful words explaining why you must pause focus right now.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                if (newText.length - text.length > 25) {
                    pasteWarning = true
                } else {
                    pasteWarning = false
                    text = newText
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp),
            placeholder = {
                Text(
                    text = "I need to open an app right now because...",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = if (isComplete) SuccessGreen else TextMuted,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (pasteWarning) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Clipboard paste blocked — please type your intention manually.",
                color = BrandPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isComplete) SuccessGreen else IvoryAction,
                trackColor = DarkSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isComplete) "$wordCount / 50 ✓" else "$wordCount / 50",
                color = if (isComplete) SuccessGreen else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Distinct: $uniqueWordCount/20" + if (hasUniqueWords) " ✓" else "",
                color = if (hasUniqueWords) SuccessGreen else TextMuted,
                fontSize = 11.sp
            )
            Text(
                text = "Avg length: ${String.format(Locale.getDefault(), "%.1f", avgLength)} (min 3.5)" + if (hasAvgLength) " ✓" else "",
                color = if (hasAvgLength) SuccessGreen else TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = { onComplete(text) },
            enabled = isComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = IvoryAction,
                disabledContainerColor = DarkSurfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = buttonText,
                color = if (isComplete) InkOnLight else TextMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PhysicalWorkoutSection(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var repsCount by remember { mutableIntStateOf(0) }
    val targetReps = 15
    val isFinished = repsCount >= targetReps
    var isDipping by remember { mutableStateOf(false) }

    val hasAccelerometer = remember {
        (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)
            ?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || repsCount >= targetReps) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                if (magnitude < 7.2f) {
                    isDipping = true
                } else if (isDipping && magnitude > 12.2f) {
                    isDipping = false
                    repsCount += 1

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(70)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PHYSICAL RESET",
            style = MicroLabelText,
            color = BrandPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "15 Deep Squats",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasAccelerometer) {
                "Hold your phone securely and perform 15 full squats. Accelerometer motion sensors verify each rep."
            } else {
                "Motion sensor unavailable on this device. Tap below for each completed squat."
            },
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val strokeWidth = 7.dp.toPx()
                drawCircle(
                    color = DarkSurfaceElevated,
                    style = Stroke(width = strokeWidth)
                )
                val progress = (repsCount.toFloat() / targetReps).coerceIn(0f, 1f)
                if (progress > 0f) {
                    drawArc(
                        color = if (isFinished) SuccessGreen else BrandPrimary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isFinished) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = SuccessGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "COMPLETE",
                        style = MicroLabelText,
                        color = SuccessGreen
                    )
                } else {
                    Text(
                        text = "$repsCount / $targetReps",
                        color = TextPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = if (isDipping) "PUSH UP" else "SQUAT DOWN",
                        style = MicroLabelText,
                        color = if (isDipping) BrandPrimary else TextMuted
                    )
                }
            }
        }

        // Strict Anti-Cheat: Manual rep counter ONLY exposed when device lacks an accelerometer
        if (!isFinished && !hasAccelerometer) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { if (repsCount < targetReps) repsCount += 1 },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Tap to Count Rep Manually (+1)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onComplete,
            enabled = isFinished,
            colors = ButtonDefaults.buttonColors(
                containerColor = IvoryAction,
                disabledContainerColor = DarkSurfaceVariant
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (isFinished) "Unlock 5-Minute Pass" else "${targetReps - repsCount} More Squats",
                color = if (isFinished) InkOnLight else TextMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

data class TacticalMatePuzzle(
    val title: String,
    val prompt: String,
    val pieces: Map<String, String>,
    val winningSquare: String,
    val winningNotation: String
)

object ClarityPuzzleBank {
    val chessMates: List<TacticalMatePuzzle> = listOf(
        TacticalMatePuzzle(
            title = "Back-Rank Rook Mate",
            prompt = "White Rook Re1 to move — tap the square delivering Checkmate:",
            pieces = mapOf(
                "g8" to "♚", "f7" to "♟", "g7" to "♟", "h7" to "♟",
                "e1" to "♖", "g1" to "♔"
            ),
            winningSquare = "e8",
            winningNotation = "Re8#"
        ),
        TacticalMatePuzzle(
            title = "Queen-Bishop Battery",
            prompt = "White Queen Qd3 to move — tap the square delivering Checkmate:",
            pieces = mapOf(
                "g8" to "♚", "f8" to "♜", "f7" to "♟", "g7" to "♟",
                "d3" to "♕", "b1" to "♗", "g1" to "♔"
            ),
            winningSquare = "h7",
            winningNotation = "Qh7#"
        ),
        TacticalMatePuzzle(
            title = "Arabian Mate",
            prompt = "White Rook Ra7 to move — tap the square delivering Checkmate:",
            pieces = mapOf(
                "h8" to "♚", "g7" to "♟",
                "a7" to "♖", "f6" to "♘", "g1" to "♔"
            ),
            winningSquare = "h7",
            winningNotation = "Rh7#"
        ),
        TacticalMatePuzzle(
            title = "Classical Bishop + Queen",
            prompt = "White Queen Qh5 to move — tap the square delivering Checkmate:",
            pieces = mapOf(
                "e8" to "♚", "d8" to "♛", "c8" to "♝", "f8" to "♝", "d7" to "♟", "e5" to "♟", "f7" to "♟",
                "h5" to "♕", "c4" to "♗", "e1" to "♔"
            ),
            winningSquare = "f7",
            winningNotation = "Qxf7#"
        )
    )
}

@Composable
internal fun MentalClarityPuzzleSection(
    onComplete: (String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Chess Mate-in-1, 1 = Arithmetic Chain
    var puzzleIndex by remember { mutableIntStateOf(ClarityPuzzleBank.chessMates.indices.random()) }
    val puzzle = ClarityPuzzleBank.chessMates[puzzleIndex]
    var wrongSquare by remember { mutableStateOf<String?>(null) }
    var chessSolved by remember { mutableStateOf(false) }

    // 3-step Mental Arithmetic Chain state
    var mathStep by remember { mutableIntStateOf(0) }
    var a by remember { mutableIntStateOf((13..24).random()) }
    var b by remember { mutableIntStateOf((6..9).random()) }
    var c by remember { mutableIntStateOf((11..29).random()) }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }
    val mathSolved = mathStep >= 3

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COGNITIVE RESET PUZZLE",
            style = MicroLabelText,
            color = EmeraldMint
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(0 to "Chess Mate-in-1", 1 to "Mental Math (3x)").forEach { (idx, title) ->
                val selected = mode == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) IvoryAction else Color.Transparent)
                        .clickable { mode = idx }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selected) InkOnLight else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (mode == 0) {
            Text(
                text = puzzle.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (chessSolved) "Checkmate (${puzzle.winningNotation}) verified!" else puzzle.prompt,
                color = if (chessSolved) SuccessGreen else TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            val files = listOf("a", "b", "c", "d", "e", "f", "g", "h")
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(12.dp))
            ) {
                for (rank in 8 downTo 1) {
                    Row {
                        for (fileIdx in 0..7) {
                            val sq = "${files[fileIdx]}$rank"
                            val isLightSquare = (fileIdx + rank) % 2 == 1
                            val piece = puzzle.pieces[sq]
                            val bg = when {
                                chessSolved && sq == puzzle.winningSquare -> SuccessGreen.copy(alpha = 0.55f)
                                wrongSquare == sq -> BrandPrimary.copy(alpha = 0.45f)
                                isLightSquare -> Color(0xFF282B33)
                                else -> Color(0xFF14161B)
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(bg)
                                    .clickable(enabled = !chessSolved) {
                                        if (sq == puzzle.winningSquare) {
                                            wrongSquare = null
                                            chessSolved = true
                                        } else {
                                            wrongSquare = sq
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (piece != null) {
                                    Text(
                                        text = piece,
                                        color = Color(0xFFF4F4F5),
                                        fontSize = 18.sp
                                    )
                                } else if (rank == 1) {
                                    Text(text = files[fileIdx], color = TextMuted.copy(alpha = 0.45f), fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (wrongSquare != null && !chessSolved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$wrongSquare is not checkmate. Find the mating square.",
                    color = BrandPrimary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onComplete("Solved tactical mate-in-1 chess puzzle (${puzzle.winningNotation})") },
                enabled = chessSolved,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IvoryAction,
                    disabledContainerColor = DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (chessSolved) "Unlock 5-Minute Pass" else "Tap the Mating Square",
                    color = if (chessSolved) InkOnLight else TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        } else {
            val expected = a * b + c
            Text(
                text = if (mathSolved) "3 / 3 Verified" else "Question ${mathStep + 1} of 3",
                color = if (mathSolved) SuccessGreen else BrandPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (mathSolved) "Clarity Restored" else "$a × $b + $c = ?",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (!mathSolved) {
                OutlinedTextField(
                    value = mathInput,
                    onValueChange = {
                        mathInput = it.filter { ch -> ch.isDigit() || ch == '-' }
                        mathError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter answer", color = TextMuted, fontSize = 13.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = TextMuted,
                        unfocusedBorderColor = HairlineBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                if (mathError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Recalculate carefully.", color = BrandPrimary, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val parsed = mathInput.toIntOrNull()
                        if (parsed == expected) {
                            mathInput = ""
                            mathError = false
                            mathStep += 1
                            a = (13..26).random()
                            b = (6..9).random()
                            c = (11..35).random()
                        } else {
                            mathError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Verify Answer", color = InkOnLight, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { onComplete("Solved 3-step arithmetic mental clarity gate") },
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Unlock 5-Minute Pass", color = InkOnLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
