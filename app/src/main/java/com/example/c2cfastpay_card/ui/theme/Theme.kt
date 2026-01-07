package com.example.c2cfastpay_card.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// Sale 的配色方案
val SaleColorScheme = lightColorScheme(
    primary = Color(0xFFE7F2F2),         // 主要色 (淺藍綠)
    onPrimary = Color(0xFF487F81),       // 主要色上的文字/圖示 (深藍綠)
    secondary = Color(0xFF759E9F),       // 次要色 (中藍綠)
    onSecondary = Color.White,        // 定義次要色上的顏色，通常是 Black 或 White
    background = Color.White,            // 背景色 (白色)
    onBackground = Color.Black,       // 背景上的文字/圖示
    surface = Color(0xFFE7F2F2),         // 表面色 (淺藍綠，用於 Card 等)
    onSurface = Color(0xFF487F81),    // 表面上的文字/圖示 (深藍綠)
)

// Wish 的配色方案
val WishColorScheme = lightColorScheme(
    primary = Color(0xFFFBE1BF),         // 主要色 (淺橘黃)
    onPrimary = Color(0xFFF79329),       // 主要色上的文字/圖示 (深橘)
    secondary = Color(0xFFFFC881),       // 次要色 (中橘黃)
    onSecondary = Color.White,
    background = Color.White,            // 背景色 (白色)
    onBackground = Color.Black,
    surface = Color(0xFFFBE1BF),         // 表面色 (淺橘黃)
    onSurface = Color(0xFFF79329),    // 表面上的文字/圖示 (深橘)
)

// 定義一個預設的 DarkColorScheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF759E9F), // 使用 Sale 的次要色
    secondary = Color(0xFFFFC881), // 使用 Wish 的次要色
    background = Color(0xFF1C1B1F), // 深色背景
    surface = Color(0xFF2C2B2F), // 深色表面
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// 定義一個預設的 LightColorScheme 
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF759E9F), // 使用 Sale 的次要色
    secondary = Color(0xFFFF9800), // 橘色
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)


@Composable
fun C2CFastPay_CardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // 1. 如果支援動態取色，且開啟功能 -> 使用系統桌布配色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 2. 如果是深色模式 -> 使用 DarkColorScheme
        darkTheme -> DarkColorScheme
        // 3. 否則 -> 使用預設亮色模式
        else -> LightColorScheme
    }

    // 將決定好的顏色和字體套用到 MaterialTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}