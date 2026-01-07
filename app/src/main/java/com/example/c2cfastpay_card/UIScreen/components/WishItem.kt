package com.example.c2cfastpay_card.UIScreen.components // ★ 確認是 components

import com.google.firebase.firestore.PropertyName
import java.util.UUID

data class WishItem(
    val uuid: String = UUID.randomUUID().toString(),

    val title: String = "",
    val price: String = "",
    val payment: String = "",
    val imageUri: String = "",
    val images: List<String> = emptyList(),

    val description: String = "",

    val qty: String = "1",
    val condition: String = "皆可",

    // 程式碼用 memo，但讀寫資料庫時找 "tags" 欄位
    @get:PropertyName("tags") @set:PropertyName("tags")
    var memo: String = "",

    // 程式碼用 ownerId，但讀寫資料庫時找 "userId" 欄位
    @get:PropertyName("userId") @set:PropertyName("userId")
    var ownerId: String = "",

    val ownerName: String = "",
    val ownerEmail: String = "",

    val timestamp: Long = System.currentTimeMillis()
)