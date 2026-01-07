package com.example.c2cfastpay_card.data

import java.util.UUID

// 購物車項目資料模型
data class CartItem(
    val id: String = UUID.randomUUID().toString(), // 1. ID

    // 商品資訊
    val productId: String = "",      // 商品ID
    val productTitle: String = "",   // 標題
    val productPrice: String = "",   // 價格
    val productImage: String = "",   // 圖片

    // 賣家資訊
    val sellerId: String = "",       // 賣家ID

    // 購物資訊
    var quantity: Int = 1,           // 數量
    val addedAt: Long = System.currentTimeMillis(), // 8. 加入時間

    // 庫存與勾選
    val stock: Int = 99,             // 庫存
    var isChecked: Boolean = false   // 是否勾選
) {
    // 無參數建構子 (Firebase 讀取需要)
    constructor() : this("", "", "", "0", "", "", 1, 0L, 99, false)
}