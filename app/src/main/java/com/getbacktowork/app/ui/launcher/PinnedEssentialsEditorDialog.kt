package com.getbacktowork.app.ui.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.AppRepository
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
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SectionHeaderLabel
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary

/**
 * Resolves fallback apps shown under Pinned Essentials before the user explicitly
 * customizes their Minimalist Home list: Allowed Tools first, else essential system tools.
 */
fun resolveDefaultEssentialApps(
    installedApps: List<AppInfo>,
    allowedApps: List<AppInfo>
): List<AppInfo> {
    if (allowedApps.isNotEmpty()) return allowedApps
    return installedApps.filter {
        val p = it.packageName.lowercase()
        p.contains("dialer") || p.contains("contacts") || p.contains("calendar") ||
            p.contains("notes") || p.contains("keep") || p.contains("deskclock") ||
            p.contains("clock") || p.contains("camera") || p.contains("maps")
    }.take(8)
}

/**
 * Shared Swiss-minimal editor dialog for selecting and ordering apps pinned to
 * the Minimalist Homescreen's Pinned Essentials list.
 */
@Composable
fun PinnedEssentialsEditorDialog(
    installedApps: List<AppInfo>,
    isLoadingApps: Boolean = false,
    initialPinnedOrder: List<String>,
    allowedPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    onResetToAuto: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var workExpanded by remember { mutableStateOf(false) }
    var hasUserEdited by remember { mutableStateOf(false) }
    var draftOrder by remember { mutableStateOf(initialPinnedOrder.distinct()) }

    val appByKey = remember(installedApps) { installedApps.associateBy { it.uniqueKey } }
    val appByPersonalPkg = remember(installedApps) {
        installedApps.filterNot { it.isWorkProfile }.associateBy { it.packageName }
    }

    LaunchedEffect(installedApps, isLoadingApps, initialPinnedOrder) {
        if (!isLoadingApps && !hasUserEdited) {
            draftOrder = if (appByKey.isNotEmpty()) {
                initialPinnedOrder.mapNotNull { key ->
                    (appByKey[key] ?: appByPersonalPkg[key])?.uniqueKey
                }.distinct()
            } else {
                initialPinnedOrder.distinct()
            }
        }
    }

    val unpinnedApps = remember(installedApps, draftOrder) {
        val draftSet = draftOrder.toSet()
        installedApps.filterNot { draftSet.contains(it.uniqueKey) }
    }
    val (unpinnedPersonal, unpinnedWork) = remember(unpinnedApps) {
        AppRepository.partitionPersonalAndWorkApps(unpinnedApps)
    }

    val searchMatches = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 28.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(22.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PINNED ESSENTIALS",
                            style = MicroLabelText,
                            color = BrandPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Edit Home Screen Apps",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${draftOrder.size} pinned",
                                color = if (draftOrder.isNotEmpty()) EmeraldMint else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose and reorder the distraction-free apps shown on your Minimalist Home.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    placeholder = {
                        Text("Search installed apps...", color = TextMuted, fontSize = 12.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    },
                    singleLine = true,
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

                Spacer(modifier = Modifier.height(10.dp))

                // Quick helper presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorQuickChip(
                        label = "Sync Allowed",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hasUserEdited = true
                            draftOrder = installedApps
                                .filter { allowedPackages.contains(it.packageName) || allowedPackages.contains(it.uniqueKey) }
                                .map { it.uniqueKey }
                        }
                    )
                    EditorQuickChip(
                        label = "Clear All",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hasUserEdited = true
                            draftOrder = emptyList()
                        }
                    )
                    EditorQuickChip(
                        label = "Reset Auto",
                        modifier = Modifier.weight(1f),
                        onClick = onResetToAuto
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 380.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.45f))
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
                ) {
                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = BrandPrimary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            if (searchQuery.isBlank()) {
                                if (draftOrder.isNotEmpty()) {
                                    item(key = "header_pinned") {
                                        Text(
                                            text = "PINNED ORDER (${draftOrder.size})",
                                            style = SectionHeaderLabel,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                    itemsIndexed(
                                        items = draftOrder,
                                        key = { _, key -> "pinned_$key" }
                                    ) { index, key ->
                                        val app = appByKey[key] ?: appByPersonalPkg[key] ?: return@itemsIndexed
                                        PinnedOrderRow(
                                            app = app,
                                            canMoveUp = index > 0,
                                            canMoveDown = index < draftOrder.lastIndex,
                                            onMoveUp = {
                                                hasUserEdited = true
                                                val mutable = draftOrder.toMutableList()
                                                mutable[index] = mutable[index - 1].also {
                                                    mutable[index - 1] = mutable[index]
                                                }
                                                draftOrder = mutable
                                            },
                                            onMoveDown = {
                                                hasUserEdited = true
                                                val mutable = draftOrder.toMutableList()
                                                mutable[index] = mutable[index + 1].also {
                                                    mutable[index + 1] = mutable[index]
                                                }
                                                draftOrder = mutable
                                            },
                                            onRemove = {
                                                hasUserEdited = true
                                                draftOrder = draftOrder.filterNot { it == key }
                                            }
                                        )
                                    }
                                }

                                item(key = "header_available") {
                                    Text(
                                        text = if (draftOrder.isEmpty()) {
                                            "TAP TO PIN APPS"
                                        } else {
                                            "ADD MORE APPS"
                                        },
                                        style = SectionHeaderLabel,
                                        modifier = Modifier.padding(
                                            start = 14.dp,
                                            end = 14.dp,
                                            top = if (draftOrder.isNotEmpty()) 12.dp else 6.dp,
                                            bottom = 4.dp
                                        )
                                    )
                                }
                                if (unpinnedWork.isNotEmpty()) {
                                    item(key = "work_dropdown_header") {
                                        WorkProfileDropdownHeader(
                                            count = unpinnedWork.size,
                                            expanded = workExpanded,
                                            onToggle = { workExpanded = !workExpanded },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (workExpanded) {
                                        items(
                                            items = unpinnedWork,
                                            key = { "unpinned_work_${it.uniqueKey}" }
                                        ) { app ->
                                            SelectableAppRow(
                                                app = app,
                                                isSelected = false,
                                                isAllowedTool = allowedPackages.contains(app.packageName),
                                                onToggle = {
                                                    hasUserEdited = true
                                                    draftOrder = draftOrder + app.uniqueKey
                                                }
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = unpinnedPersonal,
                                    key = { "unpinned_${it.uniqueKey}" }
                                ) { app ->
                                    SelectableAppRow(
                                        app = app,
                                        isSelected = false,
                                        isAllowedTool = allowedPackages.contains(app.packageName),
                                        onToggle = {
                                            hasUserEdited = true
                                            draftOrder = draftOrder + app.uniqueKey
                                        }
                                    )
                                }
                            } else if (searchMatches.isEmpty()) {
                                item(key = "empty_search") {
                                    Text(
                                        text = "No apps match \"$searchQuery\"",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            } else {
                                items(
                                    items = searchMatches,
                                    key = { "search_${it.uniqueKey}" }
                                ) { app ->
                                    val selected = draftOrder.contains(app.uniqueKey)
                                    SelectableAppRow(
                                        app = app,
                                        isSelected = selected,
                                        isAllowedTool = allowedPackages.contains(app.packageName),
                                        onToggle = {
                                            hasUserEdited = true
                                            draftOrder = if (selected) {
                                                draftOrder.filterNot { it == app.uniqueKey }
                                            } else {
                                                draftOrder + app.uniqueKey
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(13.dp),
                        border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { onSave(draftOrder) },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text(
                            text = "Save (${draftOrder.size})",
                            color = InkOnLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorQuickChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(0.75.dp, HairlineBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PinnedOrderRow(
    app: AppInfo,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppRowIconBadge(app = app)
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = app.appName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (app.isWorkProfile) {
                Spacer(modifier = Modifier.width(6.dp))
                WorkProfileBadge()
            }
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = canMoveUp, onClick = onMoveUp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Move Up",
                tint = if (canMoveUp) TextSecondary else TextMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = canMoveDown, onClick = onMoveDown),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Move Down",
                tint = if (canMoveDown) TextSecondary else TextMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Unpin ${app.appName}",
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SelectableAppRow(
    app: AppInfo,
    isSelected: Boolean,
    isAllowedTool: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppRowIconBadge(app = app)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (app.isWorkProfile) {
                    Spacer(modifier = Modifier.width(6.dp))
                    WorkProfileBadge()
                }
            }
            if (isAllowedTool) {
                Text(
                    text = "Allowed Tool",
                    color = EmeraldMint.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) EmeraldMint.copy(alpha = 0.18f) else DarkSurfaceElevated)
                .border(
                    0.75.dp,
                    if (isSelected) EmeraldMint.copy(alpha = 0.55f) else HairlineBorder,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (isSelected) "Pinned" else "Pin ${app.appName}",
                tint = if (isSelected) EmeraldMint else TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AppRowIconBadge(app: AppInfo) {
    val bmp = app.iconBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
