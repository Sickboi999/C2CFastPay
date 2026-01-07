package com.example.c2cfastpay_card.data

import com.google.firebase.Timestamp

// 訂單的主結構
data class Order(
    val id: String = "",
    val buyerId: String = "",       // 買家 ID (用來查「我買的」)
    val sellerId: String = "",      // 賣家 ID (用來查「我賣的」)
    val items: List<OrderItem> = emptyList(), // 訂單內的商品列表
    val totalAmount: Long = 0,      // 訂單總金額
    val status: String = "PENDING", // 狀態: PENDING(處理中), SHIPPED(已出貨), COMPLETED(完成)
    val timestamp: Timestamp = Timestamp.now(), // 下單時間
    val shippingInfo: String = "標準運送" // 物流方式 (可擴充)
)

// 訂單內的商品快照 (Snapshot)
// 不直接用 CartItem 是因為訂單成立後，資料就不該再變動
data class OrderItem(
    val productId: String = "",
    val productTitle: String = "",
    val productImage: String = "",
    val pricePerUnit: Long = 0, // 下單時的單價
    val quantity: Int = 0
)