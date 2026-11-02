package com.polarrun.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.polarrun.engine.Kind

object Pal {
    val Bg = Color(0xFF0A0B0E)
    val Card = Color(0xFF14161B)
    val CardHi = Color(0xFF1B1E25)
    val Line = Color(0xFF2A2E38)
    val Text = Color(0xFFF1F2F4)
    val Sub = Color(0xFF8E94A3)
    val Lime = Color(0xFFC8FF3D)
    val Easy = Color(0xFF39D6A0)
    val Mod = Color(0xFFFFC53D)
    val Hard = Color(0xFFFF5A3C)
    val Long = Color(0xFF58A6FF)
    val Rest = Color(0xFF6B7280)
}

fun kindColor(k: Kind): Color = when (k) {
    Kind.EASY -> Pal.Easy
    Kind.HARD -> Pal.Hard
    Kind.LONG -> Pal.Long
    Kind.REST -> Pal.Rest
}

fun scoreColor(score: Int?): Color = when {
    score == null -> Pal.Rest
    score >= 60 -> Pal.Lime
    score >= 40 -> Pal.Mod
    else -> Pal.Hard
}

val PolarScheme = darkColorScheme(
    primary = Pal.Lime,
    onPrimary = Color.Black,
    secondaryContainer = Color(0xFF2B3416),
    onSecondaryContainer = Pal.Lime,
    background = Pal.Bg,
    onBackground = Pal.Text,
    surface = Pal.Card,
    onSurface = Pal.Text,
    surfaceVariant = Pal.CardHi,
    onSurfaceVariant = Pal.Sub,
    outline = Pal.Line,
    outlineVariant = Pal.Line
)
