package com.getbacktowork.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.components.AppRuleConfigDialog
import com.getbacktowork.app.ui.components.WorkProfileBadge
import com.getbacktowork.app.ui.components.WorkProfileDropdownHeader
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.EmeraldMint
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.InkOnLight
import com.getbacktowork.app.ui.theme.IvoryAction
import com.getbacktowork.app.ui.theme.SectionHeaderLabel
import com.getbacktowork.app.ui.theme.SignalCoral
import com.getbacktowork.app.ui.theme.SuccessGreen
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhitelistScreen(
    sessionManager: FocusSessionManager
) {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val sessionState by sessionManager.sessionState.collectAsState()
    val ultraFocusState by sessionManager.ultraFocusStateFlow.collectAsState()
    val allowedPackages by sessionManager.allowedPackagesFlow.collectAsState()
    val savedGroups by sessionManager.savedGroupsFlow.collectAsState()
    val appDailyCaps by sessionManager.appDailyCapsFlow.collectAsState()
    val appTodayUsage by sessionManager.appTodayUsageFlow.collectAsState()
    val customAppNames by sessionManager.customAppNamesFlow.collectAsState()
    val cooldownPackages by sessionManager.cooldownPackagesFlow.collectAsState()
    val defaultDialer = sessionManager.getDefaultDialerPackage()
    val isLocked = (sessionState.isActive && sessionState.isStrictMode) || ultraFocusState.isEnabled

    val lockMessage = if (ultraFocusState.isEnabled) {
        "Whitelist locked during ${ultraFocusState.durationHours}h Cold Turkey."
    } else {
        "Whitelist locked during active Strict Focus session."
    }

    var showSaveGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) } // 0: All, 1: Allowed
    var workExpanded by remember { mutableStateOf(false) }
    var editingRuleApp by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(Unit) {
        apps = repository.getInstalledApps()
        isLoading = false
    }

    val resolvedApps = remember(apps, customAppNames) {
        apps.map {
            val resolvedName = sessionManager.resolveAppName(
                it.uniqueKey,
                sessionManager.resolveAppName(it.packageName, it.appName)
            )
            it.copy(appName = resolvedName)
        }
    }

    val filteredApps = remember(resolvedApps, searchQuery, filterMode, allowedPackages) {
        val searched = resolvedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
        if (filterMode == 1) {
            searched.filter { allowedPackages.contains(it.packageName) || it.packageName == defaultDialer }
        } else {
            searched.sortedWith(
                compareByDescending<AppInfo> { allowedPackages.contains(it.packageName) || it.packageName == defaultDialer }
                    .thenBy { it.appName.lowercase() }
            )
        }
    }
    val (personalFilteredApps, workFilteredApps) = remember(filteredApps) {
        AppRepository.partitionPersonalAndWorkApps(filteredApps)
    }
    val effectiveWorkExpanded = workExpanded || searchQuery.isNotBlank()

    if (showSaveGroupDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveGroupDialog = false
                newGroupName = ""
            },
            title = {
                Text(
                    text = "Save Profile",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Save these ${allowedPackages.size} permitted apps as a quick preset.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        placeholder = { Text("e.g. Deep Study, Commute", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = IvoryAction,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newGroupName.trim()
                        if (trimmed.isNotEmpty()) {
                            sessionManager.saveCustomGroup(trimmed, allowedPackages)
                            showSaveGroupDialog = false
                            newGroupName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(12.dp),
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Save", color = InkOnLight, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveGroupDialog = false
                        newGroupName = ""
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Editorial Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Allowed Apps",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Only selected apps can open during lock modes.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${allowedPackages.size} allowed",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isLocked) {
            Spacer(modifier = Modifier.height(12.dp))
            val bannerTint = if (ultraFocusState.isEnabled) SignalCoral else BrandPrimary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, bannerTint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = bannerTint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = lockMessage,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saved Groups Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "PROFILES", style = SectionHeaderLabel)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        newGroupName = ""
                        showSaveGroupDialog = true
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Save Profile",
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Save Preset",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(savedGroups, key = { it.name }) { group ->
                val isSelected = (allowedPackages == group.packages)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) IvoryAction else DarkSurface)
                        .border(
                            0.75.dp,
                            if (isSelected) IvoryAction else HairlineBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (isLocked) {
                                Toast.makeText(context, lockMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                sessionManager.setAllowedPackages(group.packages)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (group.name == "Calls Only") {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (isSelected) InkOnLight else SuccessGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = "${group.name} (${group.packages.size})",
                            color = if (isSelected) InkOnLight else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                        if (group.isCustom && !isLocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete Preset",
                                tint = if (isSelected) InkOnLight else TextMuted,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { sessionManager.deleteCustomGroup(group.name) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Compact Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            placeholder = { Text("Search apps", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Filter Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                0 to "All Apps (${apps.size})",
                1 to "Allowed (${allowedPackages.size})"
            ).forEach { (idx, title) ->
                val active = filterMode == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) DarkSurfaceElevated else Color.Transparent)
                        .clickable { filterMode = idx }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IvoryAction, strokeWidth = 2.dp)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterMode == 1) "No apps allowed yet.\nSelect work essentials from All Apps."
                    else if (searchQuery.isNotEmpty()) "No apps match \"$searchQuery\""
                    else "No launchable apps detected.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Single clean grouped list with expandable Work Profile dropdown when present
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface)
                    .border(0.75.dp, HairlineBorder, RoundedCornerShape(18.dp))
            ) {
                if (workFilteredApps.isNotEmpty()) {
                    item(key = "work_profile_dropdown") {
                        WorkProfileDropdownHeader(
                            count = workFilteredApps.size,
                            expanded = effectiveWorkExpanded,
                            onToggle = { workExpanded = !workExpanded },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                    if (effectiveWorkExpanded) {
                        itemsIndexed(workFilteredApps, key = { _, it -> it.uniqueKey }) { index, app ->
                            WhitelistAppRow(
                                app = app,
                                defaultDialer = defaultDialer,
                                allowedPackages = allowedPackages,
                                appDailyCaps = appDailyCaps,
                                appTodayUsage = appTodayUsage,
                                cooldownPackages = cooldownPackages,
                                searchQuery = searchQuery,
                                isLocked = isLocked,
                                lockMessage = lockMessage,
                                onToggleAllow = { sessionManager.toggleAppAllowed(app.packageName) },
                                onOpenRules = { editingRuleApp = app }
                            )
                            if (index < workFilteredApps.lastIndex || personalFilteredApps.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 66.dp)
                                        .height(0.5.dp)
                                        .background(HairlineBorder)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(personalFilteredApps, key = { _, it -> it.uniqueKey }) { index, app ->
                    WhitelistAppRow(
                        app = app,
                        defaultDialer = defaultDialer,
                        allowedPackages = allowedPackages,
                        appDailyCaps = appDailyCaps,
                        appTodayUsage = appTodayUsage,
                        cooldownPackages = cooldownPackages,
                        searchQuery = searchQuery,
                        isLocked = isLocked,
                        lockMessage = lockMessage,
                        onToggleAllow = { sessionManager.toggleAppAllowed(app.packageName) },
                        onOpenRules = { editingRuleApp = app }
                    )
                    if (index < personalFilteredApps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 66.dp)
                                .height(0.5.dp)
                                .background(HairlineBorder)
                        )
                    }
                }
            }
        }
    }

    editingRuleApp?.let { target ->
        AppRuleConfigDialog(
            app = target,
            currentAlias = (customAppNames[target.uniqueKey] ?: customAppNames[target.packageName]).orEmpty(),
            currentCap = appDailyCaps[target.packageName],
            todayUsage = appTodayUsage[target.packageName] ?: FocusSessionManager.AppTodayUsage(target.packageName, ""),
            isCooldownEnabled = cooldownPackages.contains(target.packageName),
            showPinOption = false,
            onDismiss = { editingRuleApp = null },
            onSaveRules = { newAlias, maxMins, maxOpens ->
                sessionManager.setCustomAppName(target.uniqueKey, newAlias)
                sessionManager.setAppDailyCap(target.packageName, maxMins, maxOpens)
                Toast.makeText(context, "Saved rules for ${target.appName}", Toast.LENGTH_SHORT).show()
                editingRuleApp = null
            },
            onToggleCooldown = {
                val added = sessionManager.toggleCooldownPackage(target.packageName)
                if (added && sessionManager.getCooldownSeconds() == 0) {
                    sessionManager.setCooldownSeconds(10)
                }
                Toast.makeText(
                    context,
                    if (added) "Mindful Pause active for ${target.appName}" else "Mindful Pause removed",
                    Toast.LENGTH_SHORT
                ).show()
                editingRuleApp = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhitelistAppRow(
    app: AppInfo,
    defaultDialer: String?,
    allowedPackages: Set<String>,
    appDailyCaps: Map<String, FocusSessionManager.AppDailyCap>,
    appTodayUsage: Map<String, FocusSessionManager.AppTodayUsage>,
    cooldownPackages: Set<String>,
    searchQuery: String,
    isLocked: Boolean,
    lockMessage: String,
    onToggleAllow: () -> Unit,
    onOpenRules: () -> Unit
) {
    val context = LocalContext.current
    val isDialer = !app.isWorkProfile && (
        app.packageName == defaultDialer ||
            app.packageName == "com.google.android.dialer" ||
            app.packageName == "com.samsung.android.dialer" ||
            app.packageName == "com.android.dialer" ||
            app.packageName == "com.android.phone"
        )
    val isAllowed = isDialer || allowedPackages.contains(app.packageName)
    val appCap = appDailyCaps[app.packageName]
    val usage = appTodayUsage[app.packageName]
    val hasCooldown = cooldownPackages.contains(app.packageName)
    val ruleSummary = buildList {
        if (appCap != null && appCap.maxMinutesPerDay > 0) add("${appCap.maxMinutesPerDay}m/d")
        if (appCap != null && appCap.maxOpensPerDay > 0) add("${appCap.maxOpensPerDay}x/d")
        if (hasCooldown) add("Pause")
        if (usage != null && (usage.opens > 0 || usage.minutes > 0)) {
            add("Today ${usage.opens}x/${usage.minutes}m")
        }
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = !isDialer,
                onClick = {
                    if (isLocked) {
                        Toast.makeText(context, lockMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        onToggleAllow()
                    }
                },
                onLongClick = {
                    if (!isLocked) onOpenRules()
                }
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else if (isDialer) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        color = if (isAllowed) TextPrimary else TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isWorkProfile) {
                        Spacer(modifier = Modifier.width(6.dp))
                        WorkProfileBadge()
                    }
                }
                if (isDialer) {
                    Text(
                        text = "Emergency calls always permitted",
                        color = EmeraldMint,
                        fontSize = 11.sp
                    )
                } else if (ruleSummary.isNotEmpty()) {
                    Text(
                        text = ruleSummary,
                        color = if (appCap != null || hasCooldown) BrandPrimary else TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (searchQuery.isNotBlank()) {
                    Text(
                        text = app.packageName,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!isDialer) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isLocked) {
                            Toast.makeText(context, lockMessage, Toast.LENGTH_SHORT).show()
                        } else {
                            onOpenRules()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Daily Caps & Rename",
                    tint = if (appCap != null || hasCooldown) BrandPrimary else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = isAllowed,
                enabled = !isLocked,
                onCheckedChange = {
                    if (isLocked) {
                        Toast.makeText(context, lockMessage, Toast.LENGTH_SHORT).show()
                    } else {
                        onToggleAllow()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = InkOnLight,
                    checkedTrackColor = IvoryAction,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurfaceVariant,
                    disabledCheckedThumbColor = TextMuted,
                    disabledCheckedTrackColor = DarkSurfaceElevated
                )
            )
        }
    }
}
