package com.getbacktowork.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.DarkBackground
import com.getbacktowork.app.ui.theme.DarkSurface
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DiaryScreen(
    sessionManager: FocusSessionManager,
    onNavigateToFocus: () -> Unit
) {
    val context = LocalContext.current
    val entries by sessionManager.diaryEntriesFlow.collectAsState()
    val dailyGoal by sessionManager.dailyScreenGoalFlow.collectAsState()
    val dailyReminderEnabled by sessionManager.dailyGoalReminderFlow.collectAsState()
    val reelsOverlayEnabled by sessionManager.reelsOverlayEnabledFlow.collectAsState()
    val ultraFocusState by sessionManager.ultraFocusStateFlow.collectAsState()
    val sessionState by sessionManager.sessionState.collectAsState()
    val lifeGoals by sessionManager.lifeGoalsFlow.collectAsState()
    val blockedNotifications by sessionManager.blockedNotificationsFlow.collectAsState()
    val appTodayUsage by sessionManager.appTodayUsageFlow.collectAsState()
    val appDailyCaps by sessionManager.appDailyCapsFlow.collectAsState()
    val customAppNames by sessionManager.customAppNamesFlow.collectAsState()
    var showAllDigest by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sessionManager.getDiaryEntries()
    }

    val todayEntry = remember(entries) { sessionManager.getTodayDiaryEntry() }
    val yearProgress = remember(entries) { sessionManager.getYearProgressInfo() }
    var goal1 by remember(lifeGoals) { mutableStateOf(lifeGoals.getOrElse(0) { "" }) }
    var goal2 by remember(lifeGoals) { mutableStateOf(lifeGoals.getOrElse(1) { "" }) }
    var goal3 by remember(lifeGoals) { mutableStateOf(lifeGoals.getOrElse(2) { "" }) }
    val advice = remember(entries, dailyGoal, ultraFocusState.isEnabled, sessionState.isActive) {
        sessionManager.getDailyCoachAdvice()
    }
    val totalFocusedMins = remember(entries) { sessionManager.getTotalFocusedMinutes() }
    val streakDays = remember(entries, ultraFocusState.isEnabled, sessionState.isActive) {
        sessionManager.getCurrentStreakDays()
    }

    var noteDraft by remember(todayEntry.date) { mutableStateOf(todayEntry.note) }
    LaunchedEffect(todayEntry.note) {
        if (noteDraft.isEmpty() && todayEntry.note.isNotEmpty()) {
            noteDraft = todayEntry.note
        }
    }

    val last7Days = remember(entries) {
        val map = entries.associateBy { it.date }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val shortFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        (0 until 7).map {
            val dStr = sdf.format(cal.time)
            val label = shortFmt.format(cal.time).take(1)
            val entry = map[dStr]
            cal.add(Calendar.DAY_OF_YEAR, 1)
            Triple(label, entry?.focusedMinutes ?: 0, entry?.screenMinutes ?: 0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Minimalist Editorial Header + Streak Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Last 7 days",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Focus Diary",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = if (streakDays > 0) BrandPrimary else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${streakDays}d streak",
                    color = if (streakDays > 0) BrandPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // KPI Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DiaryMetricTile(
                label = "FOCUSED TODAY",
                value = formatMinutesCompact(todayEntry.focusedMinutes),
                accent = EmeraldMint,
                modifier = Modifier.weight(1f)
            )
            DiaryMetricTile(
                label = "SCREEN TODAY",
                value = formatMinutesCompact(todayEntry.screenMinutes),
                accent = if (todayEntry.screenMinutes >= dailyGoal) SignalCoral else SkyCyan,
                modifier = Modifier.weight(1f)
            )
            DiaryMetricTile(
                label = "ALL-TIME FOCUS",
                value = formatMinutesCompact(totalFocusedMins),
                accent = BrandPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (blockedNotifications.isNotEmpty()) {
            val digestTimeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
            val displayedDigest = if (showAllDigest) blockedNotifications else blockedNotifications.take(5)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, SkyCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = SkyCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MISSED DURING FOCUS (${blockedNotifications.size})",
                            style = MicroLabelText,
                            color = SkyCyan
                        )
                    }
                    Text(
                        text = "Clear All",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { sessionManager.clearBlockedNotifications() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                displayedDigest.forEachIndexed { idx, alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground.copy(alpha = 0.6f))
                            .clickable {
                                if (!sessionManager.shouldBlockApps()) {
                                    try {
                                        val launch = context.packageManager.getLaunchIntentForPackage(alert.packageName)
                                        if (launch != null) {
                                            launch.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(launch)
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = alert.appName,
                                    color = BrandPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = digestTimeFmt.format(Date(alert.timestamp)),
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = alert.title,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (alert.preview.isNotBlank()) {
                                Text(
                                    text = alert.preview,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { sessionManager.dismissBlockedNotification(alert.id) }
                        )
                    }
                    if (idx < displayedDigest.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                if (blockedNotifications.size > 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showAllDigest) "Show fewer" else "Show all ${blockedNotifications.size} alerts",
                        color = SkyCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showAllDigest = !showAllDigest }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        val sortedAppUsage = remember(appTodayUsage, appDailyCaps, customAppNames) {
            appTodayUsage.values
                .filter { it.opens > 0 || it.minutes > 0 }
                .sortedWith(compareByDescending<FocusSessionManager.AppTodayUsage> { it.minutes }.thenByDescending { it.opens })
                .take(8)
        }
        if (sortedAppUsage.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Text(text = "TODAY'S APP BREAKDOWN", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(10.dp))
                sortedAppUsage.forEachIndexed { idx, usage ->
                    val rawName = try {
                        val info = context.packageManager.getApplicationInfo(usage.packageName, 0)
                        context.packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) {
                        usage.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    }
                    val displayName = customAppNames[usage.packageName]?.takeIf { it.isNotBlank() } ?: rawName
                    val cap = appDailyCaps[usage.packageName]
                    val capStr = buildList {
                        if (cap != null && cap.maxMinutesPerDay > 0) add("cap ${cap.maxMinutesPerDay}m")
                        if (cap != null && cap.maxOpensPerDay > 0) add("max ${cap.maxOpensPerDay}x")
                    }.joinToString(" · ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (capStr.isNotEmpty()) {
                                Text(text = capStr, color = BrandPrimary, fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = "${usage.opens}x · ${usage.minutes}m",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                    if (idx < sortedAppUsage.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Smart Quitter Advisor + Goal Selector Card
        val advisorColor = when (advice.level) {
            FocusSessionManager.AdviceLevel.GOOD -> EmeraldMint
            FocusSessionManager.AdviceLevel.CAUTION -> BrandPrimary
            FocusSessionManager.AdviceLevel.LOCK_RECOMMENDED -> SignalCoral
        }
        val screenRatio = (todayEntry.screenMinutes.toFloat() / dailyGoal.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, advisorColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SMART QUITTER ADVISOR", style = MicroLabelText, color = advisorColor)
                Text(
                    text = "${todayEntry.screenMinutes}m / ${dailyGoal}m cap",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = advice.headline,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = advice.detail,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { screenRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = advisorColor,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "CHOOSE DAILY SCREEN LIMIT", style = SectionHeaderLabel)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (mins in listOf(30, 45, 60, 90, 120)) {
                    val isSelected = dailyGoal == mins
                    val chipText = if (mins == 60) "<1h" else "${mins}m"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) IvoryAction else DarkSurfaceVariant)
                            .clickable { sessionManager.setDailyScreenGoalMinutes(mins) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipText,
                            color = if (isSelected) InkOnLight else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (advice.level == FocusSessionManager.AdviceLevel.LOCK_RECOMMENDED && !sessionState.isActive && !ultraFocusState.isEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToFocus,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = InkOnLight, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Distractions Now", color = InkOnLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mindfulness Controls (Check-in + Cute Floating Reels Counter)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Video & Reels Counter Pill",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Floating mini banner counts Reels on Shorts & minutes on long videos.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = reelsOverlayEnabled,
                    onCheckedChange = { sessionManager.setReelsOverlayEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkOnLight,
                        checkedTrackColor = EmeraldMint,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Budget Gentle Alert",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Reminds you when total screen time hits ${dailyGoal}m.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = dailyReminderEnabled,
                    onCheckedChange = { sessionManager.setDailyGoalReminderEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkOnLight,
                        checkedTrackColor = IvoryAction,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Focus vs Screen Balance Bar Visualization
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
                Text(text = "7-DAY FOCUS VS SCREEN", style = SectionHeaderLabel)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot("Focus", EmeraldMint)
                    LegendDot("Screen", SignalCoral)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val maxMins = last7Days.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(60) ?: 60
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for ((dayLabel, fMins, sMins) in last7Days) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val focusFrac = (fMins.toFloat() / maxMins.toFloat()).coerceIn(0.05f, 1f)
                            val screenFrac = (sMins.toFloat() / maxMins.toFloat()).coerceIn(0.05f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(if (fMins == 0) 0.04f else focusFrac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (fMins > 0) EmeraldMint else DarkSurfaceVariant)
                            )
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(if (sMins == 0) 0.04f else screenFrac)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (sMins > 0) SignalCoral.copy(alpha = 0.78f) else DarkSurfaceVariant)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayLabel,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days Left in Year + Personal Life Goals Card (displayed on Minimalist Home & Cooldown screens)
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
                Text(
                    text = "${yearProgress.daysRemaining} DAYS LEFT IN ${yearProgress.year}",
                    style = MicroLabelText,
                    color = BrandPrimary
                )
                Text(
                    text = "${yearProgress.elapsedPercent}% elapsed",
                    color = TextMuted,
                    fontSize = 11.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
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

            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "PERSONAL FOCUS GOALS (SHOWN ON PAUSE & HOME)", style = SectionHeaderLabel)
            Spacer(modifier = Modifier.height(8.dp))

            listOf(
                Triple(goal1, { v: String -> goal1 = v }, "Goal 1 (Top Priority)"),
                Triple(goal2, { v: String -> goal2 = v }, "Goal 2"),
                Triple(goal3, { v: String -> goal3 = v }, "Goal 3")
            ).forEach { (value, onUpdate, hint) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    placeholder = { Text(hint, color = TextMuted, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = TextMuted,
                        unfocusedBorderColor = HairlineBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Button(
                onClick = {
                    sessionManager.setLifeGoals(listOf(goal1, goal2, goal3))
                    Toast.makeText(context, "Personal goals saved offline", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Goals", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Reflection Note Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text(text = "TODAY'S FOCUS LOG NOTE", style = SectionHeaderLabel)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp),
                placeholder = {
                    Text("What did you ship offline today?", color = TextMuted, fontSize = 13.sp)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground,
                    focusedBorderColor = TextMuted,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    sessionManager.saveDiaryNote(todayEntry.date, noteDraft)
                    Toast.makeText(context, "Saved to Focus Diary", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = InkOnLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Today's Entry", color = InkOnLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Historical Chronological Entries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "DAILY LOG HISTORY (${entries.size})", style = SectionHeaderLabel)
            Text(
                text = "Export Summary",
                color = EmeraldMint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Focus Diary Export", sessionManager.buildDiaryExportText()))
                        Toast.makeText(context, "Focus diary copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Complete your first focus block or note to start building your streak diary.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            entries.take(20).forEach { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (entry.date == todayEntry.date) "Today (${entry.date})" else entry.date,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Focus ${formatMinutesCompact(entry.focusedMinutes)}",
                                color = EmeraldMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "Screen ${formatMinutesCompact(entry.screenMinutes)}",
                                color = SkyCyan,
                                fontSize = 12.sp,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    if (entry.coldTurkeyMinutes > 0 || entry.reelsWatched > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (entry.coldTurkeyMinutes > 0) {
                                Text(
                                    text = "Cold Turkey: ${formatMinutesCompact(entry.coldTurkeyMinutes)}",
                                    color = SignalCoral,
                                    fontSize = 11.sp
                                )
                            }
                            if (entry.reelsWatched > 0) {
                                Text(
                                    text = "Reels scrolled: ${entry.reelsWatched}",
                                    color = BrandPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (entry.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entry.note,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DiaryMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(0.75.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text = value,
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MicroLabelText,
            color = TextMuted
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TextMuted, fontSize = 11.sp)
    }
}

private fun formatMinutesCompact(mins: Int): String {
    return if (mins >= 60) {
        "${mins / 60}h ${mins % 60}m"
    } else {
        "${mins}m"
    }
}
