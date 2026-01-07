package com.example.c2cfastpay_card.UIScreen.components

import java.util.UUID

// 用於歷史列表顯示 
data class MatchItem(
    val id: String = "",                // Match Document ID 
    val otherUserId: String = "",       // 對方的 User ID
    val otherUserName: String = "",     // 對方的名字
    val otherUserAvatar: String = "",

    // 我喜歡對方的商品名稱列表 (例如 ["面膜", "檯燈"])
    val myLikedItems: List<String> = emptyList(),

    // 代表性商品資訊
    val productId: String = "",
    val productTitle: String = "",      // 舊欄位保留 (相容性)
    val productImageUrl: String = "",   // 列表左側的小圖

    val timestamp: Long = System.currentTimeMillis(),

    // 聊天室類型 ("SWAP" = 交換協商, "NORMAL" = 普通聊聊)
    val type: String = "SWAP"
)

// 輔助轉換 
fun ProductItem.toMatchItem(): MatchItem {
    return MatchItem(
        id = UUID.randomUUID().toString(),
        otherUserId = this.ownerId,
        otherUserName = this.ownerName, // 填入擁有者名字
        productId = this.id,
        productTitle = this.title,
        productImageUrl = this.imageUri,
        // 轉換單一商品時，將標題放入列表
        myLikedItems = listOf(this.title),
        type = "NORMAL" // 預設視為普通聊聊
    )
}