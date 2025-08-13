package com.bitchat.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors inspired by a modern South African palette.
private val MzansiDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5B865), // A warm, earthy gold
    onPrimary = Color.Black,
    secondary = Color(0xFFC0523C), // A deep, inviting red
    onSecondary = Color.White,
    background = Color(0xFF1E1E1E), // Dark charcoal, but not pure black
    onBackground = Color(0xFFD5D5D5), // Soft white text
    surface = Color(0xFF2C2C2E), // A slightly lighter charcoal for surfaces
    onSurface = Color(0xFFE5B865), // Earthy gold for text/icons
    error = Color(0xFFCF6679),
    onError = Color.Black,
    primaryContainer = Color(0xFF454545), // A muted gray for chat bubbles
    onPrimaryContainer = Color(0xFFD5D5D5),
    secondaryContainer = Color(0xFF6E4334), // A darker red for secondary elements
    onSecondaryContainer = Color.White
)

private val MzansiLightColorScheme = lightColorScheme(
    primary = Color(0xFFB07F2E), // A rich, darker gold
    onPrimary = Color.White,
    secondary = Color(0xFFC0523C), // A deep, inviting red
    onSecondary = Color.White,
    background = Color(0xFFF9F9F9), // A clean off-white
    onBackground = Color(0xFF1F1F1F), // Dark text for contrast
    surface = Color.White,
    onSurface = Color(0xFFB07F2E), // Rich gold for text/icons
    error = Color(0xFFB00020),
    onError = Color.White,
    primaryContainer = Color(0xFFE8E8E8), // A light gray for chat bubbles
    onPrimaryContainer = Color(0xFF1F1F1F),
    secondaryContainer = Color(0xFFD59987), // A lighter red for secondary elements
    onSecondaryContainer = Color.White
)

@Composable
fun BitchatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> MzansiDarkColorScheme
        else -> MzansiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
