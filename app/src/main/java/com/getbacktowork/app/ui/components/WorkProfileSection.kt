package com.getbacktowork.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getbacktowork.app.ui.theme.DarkSurface
import com.getbacktowork.app.ui.theme.DarkSurfaceElevated
import com.getbacktowork.app.ui.theme.HairlineBorder
import com.getbacktowork.app.ui.theme.MicroLabelText
import com.getbacktowork.app.ui.theme.SkyCyan
import com.getbacktowork.app.ui.theme.TextPrimary
import com.getbacktowork.app.ui.theme.TextSecondary

/**
 * Shared Swiss-minimal expandable dropdown bar for Android Work Profile apps
 * shown in both launchers and app pickers whenever a managed Work Profile exists.
 */
@Composable
fun WorkProfileDropdownHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (expanded) DarkSurfaceElevated else DarkSurface)
            .border(
                0.75.dp,
                if (expanded) SkyCyan.copy(alpha = 0.45f) else HairlineBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WorkOutline,
                contentDescription = "Work Profile",
                tint = SkyCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WORK PROFILE ($count)",
                style = MicroLabelText,
                color = if (expanded) TextPrimary else SkyCyan
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (expanded) "Hide" else "Open",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse Work Profile" else "Expand Work Profile",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WorkProfileBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SkyCyan.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.WorkOutline,
            contentDescription = "Work Profile",
            tint = SkyCyan,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "WORK",
            style = MicroLabelText,
            color = SkyCyan,
            fontSize = 9.sp
        )
    }
}
