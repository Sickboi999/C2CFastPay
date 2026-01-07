package com.example.c2cfastpay_card.data

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(), // 訊息唯一 ID
    val senderId: String = "",                     // 傳送者的 User ID
    val text: String = "",                         // 訊息內容
    val timestamp: Long = System.currentTimeMillis(), // 傳送時間
    val type: String = "TEXT", // "TEXT" or "PROPOSAL"
    val proposal: Map<String, Any>? = null
)