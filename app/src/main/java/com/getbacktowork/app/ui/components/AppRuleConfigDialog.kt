package com.getbacktowork.app.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.getbacktowork.app.data.AppInfo
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.DarkSurface
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
 * Shared Swiss-minimal per-app customization & daily limit dialog used in
 * both Minimalist Home Launcher and Whitelist / App Rules manager.
 */
@Composable
fun AppRuleConfigDialog(
    app: AppInfo,
    currentAlias: String,
    currentCap: FocusSessionManager.AppDailyCap?,
    todayUsage: FocusSessionManager.AppTodayUsage,
    isCooldownEnabled: Boolean,
    showPinOption: Boolean = false,
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onSaveRules: (alias: String, maxMinutes: Int, maxOpens: Int) -> Unit,
    onToggleCooldown: () -> Unit,
    onTogglePin: (() -> Unit)? = null
) {
    var aliasInput by remember(app.packageName) { mutableStateOf(currentAlias.ifEmpty { app.appName }) }
    var selectedMaxMinutes by remember(app.packageName) { mutableIntStateOf(currentCap?.maxMinutesPerDay ?: 0) }
    var selectedMaxOpens by remember(app.packageName) { mutableIntStateOf(currentCap?.maxOpensPerDay ?: 0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(22.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "APP RULES & LIMITS",
                    style = MicroLabelText,
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.appName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Today: ${todayUsage.opens} open${if (todayUsage.opens == 1) "" else "s"} · ${todayUsage.minutes}m used",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "CUSTOM LABEL (ALIAS)", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = aliasInput,
                    onValueChange = { aliasInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Rename label (blank = default)", color = TextMuted, fontSize = 12.sp) },
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

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "DAILY TIME CAP (EVERYDAY MODE)", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Off", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, label) ->
                        val active = selectedMaxMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) IvoryAction else DarkSurfaceVariant)
                                .border(0.75.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedMaxMinutes = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) InkOnLight else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "DAILY OPEN LIMIT", style = SectionHeaderLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "Off", 3 to "3x", 5 to "5x", 10 to "10x", 15 to "15x").forEach { (opens, label) ->
                        val active = selectedMaxOpens == opens
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) IvoryAction else DarkSurfaceVariant)
                                .border(0.75.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedMaxOpens = opens }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) InkOnLight else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val finalAlias = if (aliasInput.trim() == app.appName && currentAlias.isEmpty()) {
                            ""
                        } else {
                            aliasInput
                        }
                        onSaveRules(finalAlias, selectedMaxMinutes, selectedMaxOpens)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save Rules & Label",
                        color = InkOnLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onToggleCooldown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                ) {
                    Text(
                        text = if (isCooldownEnabled) "Mindful Pause Delay: ON (Tap to Disable)" else "Enable Mindful Pause Delay",
                        color = if (isCooldownEnabled) EmeraldMint else TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (showPinOption && onTogglePin != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                    ) {
                        Text(
                            text = if (isPinned) "Unpin from Minimalist Home" else "Pin to Minimalist Home",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
