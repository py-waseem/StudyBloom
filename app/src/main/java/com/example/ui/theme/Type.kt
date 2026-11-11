package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.R

// Google Font Playfair Display installed locally in res/font/playfair_display.ttf
val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

// Exact Welcome / Login Screen Typography Styles
val AppNameStyle = TextStyle(
    fontFamily = PlayfairFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp,
    color = LoginDeepPlum
)

val HeadlineStyle = TextStyle(
    fontFamily = PlayfairFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    color = LoginDeepPlum,
    textAlign = TextAlign.Center
)

val SubtitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    color = LoginDeepPlum,
    textAlign = TextAlign.Center
)

val ButtonTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    color = LoginPureWhite,
    textAlign = TextAlign.Center
)

val SignInPromptStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    color = LoginDeepPlum,
    textAlign = TextAlign.Center
)

val SignInActionStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    color = LoginDeepPlum,
    textAlign = TextAlign.Center
)

val PlaceholderTitleStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    color = LoginDeepPlum
)

// Default Material 3 Typography
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
