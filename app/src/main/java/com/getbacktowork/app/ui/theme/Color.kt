package com.getbacktowork.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// True OLED Canvas & Tactile Zinc-Obsidian Surfaces (restraint over gradients)
val DarkBackground = Color(0xFF000000)             // Pure OLED pitch black canvas
val DarkSurface = Color(0xFF141416)                // Primary card & sheet surface
val DarkSurfaceVariant = Color(0xFF1C1C1E)         // Nested control, pill & track fill
val DarkSurfaceElevated = Color(0xFF2A2A2D)        // Active segmented tab / pressed fill
val DarkCardBorder = Color(0x16FFFFFF)             // Subtle 8.6% hairline divider
val HairlineBorder = Color(0x14FFFFFF)             // 7.8% architectural hairline stroke

// High-Contrast Primary Ink & Controls
val IvoryAction = Color(0xFFFFFFFF)                // Pure crisp white CTA & progress arc
val InkOnLight = Color(0xFF000000)                 // Pure black ink on white buttons/chips
val CrispBone = Color(0xFFFFFFFF)

// Restrained Functional Mode Accents (semantic dots, progress arcs, status tags)
val BrandPrimary = Color(0xFFF59E0B)               // Warm Focus Amber
val BrandSecondary = Color(0xFFFBBF24)             // Highlight Amber
val SignalCoral = Color(0xFFEF4444)                // Authoritative Cold Turkey Crimson
val BrandAccent = Color(0xFF34D399)                // Botanical Sage/Emerald
val EmeraldMint = Color(0xFF34D399)                // Holiday & Calm
val SkyCyan = Color(0xFF38BDF8)                    // Travel Transit Sky
val SuccessGreen = Color(0xFF34D399)
val DangerRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

// Neutral Editorial Typography Scale (high contrast on #000000 and #101114)
val TextPrimary = Color(0xFFF4F4F5)                // Soft Ivory White
val TextSecondary = Color(0xFFA1A1AA)              // Readable Zinc 400
val TextMuted = Color(0xFF787882)                  // Accessible Muted Zinc

// Minimal flat/subtle surface brushes kept cohesive
val PrimaryGradient = Brush.linearGradient(
    listOf(Color(0xFFF4F4F5), Color(0xFFE4E4E7))
)
val AmberCoralGradient = Brush.linearGradient(
    listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
)
val SurfaceCardGradient = Brush.verticalGradient(
    listOf(Color(0xFF101114), Color(0xFF101114))
)
val TopAmbientWash = Brush.verticalGradient(
    listOf(Color(0xFF141519).copy(alpha = 0.45f), Color.Transparent)
)
