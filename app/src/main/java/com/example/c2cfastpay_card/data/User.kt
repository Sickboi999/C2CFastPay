package com.example.c2cfastpay_card.data

data class User(
    val id: String = "",        // Firebase Auth 的 UID
    val email: String = "",     // 電子信箱
    val name: String = "",      // 暱稱 (目前先預設為信箱前綴，未來可讓用戶改)
    val avatarUrl: String = "", // 大頭貼網址 (預留欄位)
    val points: Int = 99999,
    val createdAt: Long = System.currentTimeMillis() // 註冊時間
)