package com.example.c2cfastpay_card.UIScreen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// 按鈕
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,                 // 被點擊時執行的動作
    icon: ImageVector,                   // 圖示
    text: String,                        // 說明文字
    enabled: Boolean = true,             // 啟用
    contentDescription: String? = null   // 無障礙說明文字
) {
    // Column 垂直排列：上(按鈕) -> 中(間距) -> 下(文字)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally  
    ) {
        // 按鈕主體
        FloatingActionButton(
            onClick = onClick,
            // 背景顏色
            containerColor = if (enabled) {
                FloatingActionButtonDefaults.containerColor
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            // 圖示顏色邏輯 
            contentColor = if (enabled) {
                contentColorFor(MaterialTheme.colorScheme.surface)
            } else {
                MaterialTheme.colorScheme.onError.copy(alpha = 0.3f)
            },
        ) {
            // 圖示
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }

        // 讓按鈕跟文字不要黏在一起
        Spacer(modifier = Modifier.height(4.dp))

        // 文字標籤
        Text(
            textAlign = TextAlign.Center,
            text = text,
            style = MaterialTheme.typography.labelLarge 
        )
    }
}