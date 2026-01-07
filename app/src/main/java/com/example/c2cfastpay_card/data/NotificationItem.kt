package com.example.c2cfastpay_card.data

import com.google.firebase.Timestamp
import java.util.UUID

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",       // 接收通知的人 (例如賣家、許願者)
    val type: String = "",         // 類型: "ORDER", "MATCH", "WISH_FULFILLED"
    val title: String = "",        // 標題
    val message: String = "",      // 內容
    val targetId: String = "",     // 點擊後要去的目標 ID (訂單ID / 商品ID / 配對ID)
    val isRead: Boolean = false,   // 是否已讀 (可選功能)
    val timestamp: Timestamp = Timestamp.now()
)