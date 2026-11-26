package com.getbacktowork.app.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import com.getbacktowork.app.service.AppBlockerAccessibilityService
import com.getbacktowork.app.ui.launcher.MinimalistHomeActivity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.service.AdminReceiver
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.EmeraldMint
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.InkOnLight
import com.getbacktowork.app.ui.theme.IvoryAction
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SectionHeaderLabel
import com.getbacktowork.app.ui.theme.SkyCyan
import com.getbacktowork.app.ui.theme.SuccessGreen
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PermissionsScreen(
    sessionManager: FocusSessionManager,
    onReplayOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccessibility by remember { mutableStateOf(checkAccessibility(context)) }
    var hasOverlay by remember { mutableStateOf(checkOverlay(context)) }
    var hasNotificationListener by remember { mutableStateOf(checkNotificationListener(context)) }
    var hasBatteryExemption by remember { mutableStateOf(checkBatteryExemption(context)) }
    var hasDeviceAdmin by remember { mutableStateOf(checkDeviceAdmin(context)) }
    var isDefaultLauncher by remember { mutableStateOf(checkDefaultHomeLauncher(context)) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    val blockIncognito by sessionManager.blockIncognitoFlow.collectAsState()
    val adultFilter by sessionManager.adultFilterFlow.collectAsState()
    val blockShortsInFocus by sessionManager.blockShortsInFocusFlow.collectAsState()
    val blockYtHomeFeed by sessionManager.blockYtHomeFeedFlow.collectAsState()
    val blockIgExplore by sessionManager.blockIgExploreFlow.collectAsState()
    val blockSocialFeeds by sessionManager.blockSocialFeedsFlow.collectAsState()
    val blockedDomains by sessionManager.blockedDomainsFlow.collectAsState()
    val alwaysBlockDomains by sessionManager.alwaysBlockDomainsFlow.collectAsState()
    val cooldownSeconds by sessionManager.cooldownSecondsFlow.collectAsState()
    val cooldownPackages by sessionManager.cooldownPackagesFlow.collectAsState()
    val shieldDiagnostics by sessionManager.shieldDiagnosticsFlow.collectAsState()
    var showCooldownAppsDialog by remember { mutableStateOf(false) }
    var showDiagnosticsPanel by remember { mutableStateOf(false) }
    var domainInput by remember { mutableStateOf("") }

    fun refreshAll() {
        hasAccessibility = checkAccessibility(context)
        hasOverlay = checkOverlay(context)
        hasNotificationListener = checkNotificationListener(context)
        hasBatteryExemption = checkBatteryExemption(context)
        hasDeviceAdmin = checkDeviceAdmin(context)
        isDefaultLauncher = checkDefaultHomeLauncher(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            title = {
                Text(
                    text = "Accessibility Service Disclosure",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "GetBackToWork uses Android's AccessibilityService strictly on-device to detect when blocked apps, Reels/Shorts feeds, or Settings-tamper attempts open during an active Focus or Cold Turkey lock.\n\nThis app operates 100% offline with zero data collection, analytics, or internet transmission.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDisclosure = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Agree & Open Settings",
                        color = InkOnLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDisclosure = false }) {
                    Text(text = "Not Now", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    val grantedCount = listOf(
        hasAccessibility,
        hasOverlay,
        hasNotificationListener,
        hasBatteryExemption,
        hasDeviceAdmin
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Shield",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hardware-level guards for unbreakable sessions.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            val allArmed = grantedCount == 5
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (allArmed) EmeraldMint.copy(alpha = 0.14f) else DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (allArmed) "All Armed" else "$grantedCount / 5 Armed",
                    color = if (allArmed) EmeraldMint else BrandPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "CORE ENFORCEMENT PERMISSIONS", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

        // Unified grouped list for system permissions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
        ) {
            ShieldPermissionRow(
                title = "App Blocker & Anti-Tamper",
                description = "Detects blocked apps, Reels/Shorts, and Settings escape attempts.",
                isGranted = hasAccessibility,
                icon = Icons.Default.Shield,
                onGrant = { showAccessibilityDisclosure = true }
            )
            HairlineDivider()
            ShieldPermissionRow(
                title = "Display Over Other Apps",
                description = "Renders the Minimalist Launcher overlay over blocked screens.",
                isGranted = hasOverlay,
                icon = Icons.Default.Visibility,
                onGrant = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
            HairlineDivider()
            ShieldPermissionRow(
                title = "Notification Silencer",
                description = "Suppresses distracting alerts while preserving calls and work tools.",
                isGranted = hasNotificationListener,
                icon = Icons.Default.NotificationsOff,
                onGrant = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
            HairlineDivider()
            ShieldPermissionRow(
                title = "Battery Persistence",
                description = "Keeps timers accurate when the display is off.",
                isGranted = hasBatteryExemption,
                icon = Icons.Default.BatteryChargingFull,
                onGrant = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
            HairlineDivider()
            ShieldPermissionRow(
                title = "Anti-Uninstall Shield",
                description = "Prevents app removal or force-stop while locked in.",
                isGranted = hasDeviceAdmin,
                icon = Icons.Default.Security,
                onGrant = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.getComponentName(context))
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Enables anti-uninstall protection during strict focus sessions."
                        )
                    }
                    context.startActivity(intent)
                }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(text = "PHONE QUITTER HOME LAUNCHER", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(
                    0.75.dp,
                    if (isDefaultLauncher) EmeraldMint.copy(alpha = 0.4f) else HairlineBorder,
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isDefaultLauncher) EmeraldMint.copy(alpha = 0.14f) else DarkSurfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = if (isDefaultLauncher) EmeraldMint else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDefaultLauncher) "Minimalist Home Active" else "Optional Phone Quitter Launcher",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isDefaultLauncher) {
                            "Your home screen shows calm typography, live <1h advisor, and zero badges."
                        } else {
                            "Replace distracting icon grids with a clean launcher that coaches you under 1h/day. You can also keep Focus-only auto-launch instead."
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDefaultLauncher) DarkSurfaceVariant else IvoryAction
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isDefaultLauncher) "Switch Home" else "Set as Homescreen",
                        color = if (isDefaultLauncher) TextPrimary else InkOnLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        context.startActivity(Intent(context, MinimalistHomeActivity::class.java))
                    },
                    modifier = Modifier
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Preview",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(text = "BROWSER FILTERS", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Block Incognito Tabs",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Closes private browsing in Chrome, Samsung Internet & Brave.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = blockIncognito,
                    onCheckedChange = { sessionManager.setBlockIncognitoEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkOnLight,
                        checkedTrackColor = IvoryAction,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            HairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Explicit Content Shield",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "On-device check blocks adult URLs directly inside browsers.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = adultFilter,
                    onCheckedChange = { sessionManager.setAdultFilterEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkOnLight,
                        checkedTrackColor = IvoryAction,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(text = "ZERO-BYPASS PRIVATE DNS", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(DarkSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = EmeraldMint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Cloudflare Family DNS",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Zero-battery system-wide explicit content filter",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Copyable DNS hostname pill
            val dnsHostname = "family.cloudflare-dns.com"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("Private DNS Hostname", dnsHostname))
                        Toast.makeText(context, "Copied: $dnsHostname", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dnsHostname,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy DNS hostname",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Open Connections & Private DNS",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(text = "GRANULAR SURGICAL FEED BLOCKERS", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
        ) {
            SurgicalToggleRow(
                title = "YouTube Shorts & IG Reels",
                subtitle = "Blocks short-form feeds while keeping study videos & DMs open during Focus.",
                checked = blockShortsInFocus,
                onCheckedChange = { sessionManager.setBlockShortsInFocus(it) }
            )
            HairlineDivider()
            SurgicalToggleRow(
                title = "YouTube Home Feed",
                subtitle = "Hides homepage recommendations while keeping Search, Watch & Subscriptions.",
                checked = blockYtHomeFeed,
                onCheckedChange = { sessionManager.setBlockYtHomeFeed(it) }
            )
            HairlineDivider()
            SurgicalToggleRow(
                title = "Instagram Explore Grid",
                subtitle = "Blocks algorithmic discovery grid while preserving Direct Messages.",
                checked = blockIgExplore,
                onCheckedChange = { sessionManager.setBlockIgExplore(it) }
            )
            HairlineDivider()
            SurgicalToggleRow(
                title = "Clean LinkedIn & X Feeds",
                subtitle = "Closes infinite feeds on LinkedIn Home and X/Twitter 'For you'.",
                checked = blockSocialFeeds,
                onCheckedChange = { sessionManager.setBlockSocialAlgoFeeds(it) }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(text = "SCREENZEN MINDFUL OPEN DELAY", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Pause Before Opening Selected Apps",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Adds an intentional breathing wait (backed by your goals) whenever opening distraction-prone apps in everyday mode.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Off", 10 to "10s", 15 to "15s", 30 to "30s").forEach { (secs, label) ->
                    val selected = cooldownSeconds == secs
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) IvoryAction else DarkSurfaceVariant)
                            .clickable { sessionManager.setCooldownSeconds(secs) }
                            .padding(vertical = 8.dp),
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
            OutlinedButton(
                onClick = { showCooldownAppsDialog = true },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Text(
                    text = "Select Cooldown Apps (${cooldownPackages.size} selected)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(text = "CUSTOM BLOCKED WEBSITES", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("e.g. reddit.com", color = TextMuted, fontSize = 12.sp) },
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
                Button(
                    onClick = {
                        if (sessionManager.addBlockedDomain(domainInput)) {
                            domainInput = ""
                        } else {
                            Toast.makeText(context, "Enter a valid domain like reddit.com", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add domain", tint = InkOnLight, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                blockedDomains.sorted().forEach { domain ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { sessionManager.removeBlockedDomain(domain) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = domain, color = TextPrimary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Block Always (Outside Focus Too)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Also enforces custom domain list outside active Focus.", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = alwaysBlockDomains,
                    onCheckedChange = { sessionManager.setAlwaysBlockDomainsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = InkOnLight,
                        checkedTrackColor = IvoryAction,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // OEM Battery Killer Survival Guide + On-Device Signal Debugger (Beta Testers)
        Text(text = "OEM SURVIVAL & SIGNAL DEBUGGER", style = SectionHeaderLabel)
        Spacer(modifier = Modifier.height(8.dp))

        val manufacturer = remember { Build.MANUFACTURER.orEmpty() }
        val oemChecklist = remember(manufacturer) {
            val lower = manufacturer.lowercase()
            when {
                lower.contains("samsung") ->
                    "Samsung One UI: App Info -> Battery -> Unrestricted, and add GetBackToWork to Device Care -> Background usage limits -> Never auto sleeping apps."
                lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco") ->
                    "Xiaomi MIUI/HyperOS: App Info -> Autostart ON, Battery saver -> No restrictions, and lock GetBackToWork in Recent Apps."
                lower.contains("oneplus") || lower.contains("oppo") || lower.contains("realme") ->
                    "ColorOS/OxygenOS: App Info -> Battery usage -> Allow background activity + Allow auto launch."
                lower.contains("vivo") || lower.contains("iqoo") ->
                    "Funtouch/OriginOS: Enable High background power consumption & Autostart inside iManager."
                else ->
                    "Android: Set Battery usage to Unrestricted. If sideloaded via APK, tap App Info -> ⋮ -> Allow restricted settings to grant Accessibility."
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(DarkSurfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${manufacturer.ifEmpty { "Device" }} Background Guide",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (hasBatteryExemption) "Battery exemption granted" else "Tap below to prevent OS killing Accessibility",
                        color = if (hasBatteryExemption) EmeraldMint else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = oemChecklist,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
            ) {
                Text("Open App Info (Battery & Restricted Settings)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(HairlineBorder))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDiagnosticsPanel = !showDiagnosticsPanel },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = SkyCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shield Event Debugger (${shieldDiagnostics.size})",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (showDiagnosticsPanel) "Hide" else "Inspect",
                    color = SkyCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (showDiagnosticsPanel) {
                Spacer(modifier = Modifier.height(10.dp))
                if (shieldDiagnostics.isEmpty()) {
                    Text(
                        text = "No enforcement events recorded yet. Test Focus lock, Shorts block, PiP, or Cooldown to populate live telemetry locally.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                } else {
                    val diagFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    shieldDiagnostics.take(15).forEach { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${diagFmt.format(Date(ev.timestamp))} [${ev.category}] ${ev.packageName.substringAfterLast(".")}",
                                color = BrandPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = ev.detail,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Shield Diagnostics", sessionManager.buildShieldDiagnosticsText()))
                                Toast.makeText(context, "Diagnostics copied offline", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                        ) {
                            Text("Copy Log", color = TextPrimary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { sessionManager.clearShieldDiagnostics() },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                        ) {
                            Text("Clear", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                sessionManager.setOnboardingCompleted(false)
                onReplayOnboarding()
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
        ) {
            Text("Open 3-Step Setup Guide", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showCooldownAppsDialog) {
        CooldownAppPickerDialog(
            sessionManager = sessionManager,
            selectedPackages = cooldownPackages,
            onDismiss = { showCooldownAppsDialog = false }
        )
    }
}

@Composable
private fun SurgicalToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = InkOnLight,
                checkedTrackColor = EmeraldMint,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun CooldownAppPickerDialog(
    sessionManager: FocusSessionManager,
    selectedPackages: Set<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        apps = AppRepository(context).getInstalledApps()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Mindful Pause Apps", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.height(340.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = selectedPackages.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sessionManager.toggleCooldownPackage(app.packageName) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = app.appName, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Checkbox(checked = checked, onCheckedChange = { sessionManager.toggleCooldownPackage(app.packageName) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = InkOnLight, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ShieldPermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) { onGrant() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (isGranted) SuccessGreen.copy(alpha = 0.14f) else DarkSurfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) SuccessGreen else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = SuccessGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(IvoryAction)
                    .clickable { onGrant() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Enable",
                    color = InkOnLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 62.dp)
            .height(0.5.dp)
            .background(HairlineBorder)
    )
}

private fun checkAccessibility(context: Context): Boolean {
    return AppBlockerAccessibilityService.isAccessibilityServiceEnabled(context)
}

private fun checkOverlay(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
}

private fun checkNotificationListener(context: Context): Boolean {
    val listeners = NotificationManagerCompat.getEnabledListenerPackages(context)
    return listeners.contains(context.packageName)
}

private fun checkBatteryExemption(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true
}

private fun checkDeviceAdmin(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
    return dpm.isAdminActive(AdminReceiver.getComponentName(context))
}

private fun checkDefaultHomeLauncher(context: Context): Boolean {
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
