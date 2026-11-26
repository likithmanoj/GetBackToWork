package com.getbacktowork.app.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.getbacktowork.app.data.FocusSessionManager
import com.getbacktowork.app.ui.launcher.LauncherWallpaperStore
import com.getbacktowork.app.ui.theme.BrandPrimary
import com.getbacktowork.app.ui.theme.DarkBackground
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.DarkSurfaceVariant
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.InkOnLight
import com.getbacktowork.app.ui.theme.IvoryAction
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SignalCoral
import com.getbacktowork.app.ui.theme.TextMuted
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Shared opt-in wallpaper picker & dim-scrim configurator used by both
 * the Dashboard Launcher card and MinimalistHomeActivity.
 */
@Composable
fun LauncherWallpaperDialog(
    sessionManager: FocusSessionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpaperState by sessionManager.launcherWallpaperStateFlow.collectAsState()

    val previewBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        wallpaperState.version,
        wallpaperState.isEnabled
    ) {
        value = if (LauncherWallpaperStore.hasSavedWallpaperFile(context)) {
            LauncherWallpaperStore.loadWallpaperBitmap(context)
        } else {
            null
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = LauncherWallpaperStore.saveWallpaperFromUri(context, uri, sessionManager)
                Toast.makeText(
                    context,
                    if (ok) "Launcher wallpaper updated" else "Could not load selected image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val launchPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(0.75.dp, HairlineBorder, RoundedCornerShape(22.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LAUNCHER APPEARANCE",
                            style = MicroLabelText,
                            color = BrandPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Homescreen Wallpaper",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onDismiss)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stored strictly on-device using Android Photo Picker (zero storage permissions). Pure OLED Black keeps distraction & battery drain lowest.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mode selector cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isBlackSelected = !wallpaperState.isEnabled || previewBitmap == null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isBlackSelected) DarkSurfaceElevated else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isBlackSelected) IvoryAction else HairlineBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                sessionManager.setLauncherWallpaperEnabled(false)
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (isBlackSelected) IvoryAction else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pure OLED Black",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Recommended default",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    val isCustomSelected = wallpaperState.isEnabled && previewBitmap != null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCustomSelected) DarkSurfaceElevated else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isCustomSelected) BrandPrimary else HairlineBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (previewBitmap != null && !wallpaperState.isEnabled) {
                                    sessionManager.setLauncherWallpaperEnabled(true)
                                } else {
                                    launchPicker()
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (isCustomSelected) BrandPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (previewBitmap != null) "Custom Photo" else "Pick Photo",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isCustomSelected) "${wallpaperState.dimPercent}% dark scrim" else "Choose any image",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Readability Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkBackground)
                        .border(0.75.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .padding(0.dp)
                ) {
                    if (wallpaperState.isEnabled && previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!,
                            contentDescription = "Wallpaper preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(
                                        alpha = wallpaperState.effectiveDimAlpha(isLockActive = false)
                                    )
                                )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "9:41 AM",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Phone",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Light
                            )
                            Text(
                                text = "Messages",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }

                if (wallpaperState.isEnabled && previewBitmap != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Readability Dim Overlay",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${wallpaperState.dimPercent}%",
                            color = BrandPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Slider(
                        value = wallpaperState.dimPercent.toFloat(),
                        onValueChange = { sessionManager.setLauncherWallpaperDimPercent(it.roundToInt()) },
                        valueRange = 0f..FocusSessionManager.MAX_WALLPAPER_DIM_PERCENT.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = BrandPrimary,
                            activeTrackColor = BrandPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    Text(
                        text = "Tip: Keep dim between 55%–75% for crisp app names. Focus & Cold Turkey modes automatically enforce ≥${FocusSessionManager.LOCK_MODE_MIN_WALLPAPER_DIM_PERCENT}% dim.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = launchPicker,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (previewBitmap != null) "Replace Photo" else "Choose Photo",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (previewBitmap != null) {
                        OutlinedButton(
                            onClick = {
                                LauncherWallpaperStore.clearCustomWallpaper(context, sessionManager)
                                Toast.makeText(context, "Removed custom wallpaper", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.75.dp, HairlineBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove wallpaper",
                                tint = SignalCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IvoryAction),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Done",
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
