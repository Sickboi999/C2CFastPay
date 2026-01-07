package com.example.c2cfastpay_card.data

import com.google.firebase.Timestamp

// 交換訂單 (與一般購物訂單 Order 分開)
data class SwapOrder(
    val id: String = "",
    val matchId: String = "",
    // 參與者 ID 列表 (通常是 [UserA, UserB])，方便查詢 "我的交換"
    val users: List<String> = emptyList(),

    // 交換的商品列表 Map<ProductId, Quantity>
    val itemQuantities: Map<String, Int> = emptyMap(),

    // 為了 UI 顯示，我們額外存一個簡易快照 (包含誰的商品、圖片、標題)
    val itemsSnapshot: List<SwapOrderItem> = emptyList(),

    // 物流狀態: 記錄每個人是否已出貨、已收貨
    // Key: UserId, Value: Boolean
    val shippingStatus: Map<String, Boolean> = emptyMap(),
    val receivingStatus: Map<String, Boolean> = emptyMap(),

    val status: String = "PROCESSING", // PROCESSING (進行中), COMPLETED (完成), CANCELLED (取消)
    val timestamp: Timestamp = Timestamp.now()
)

// 交換商品的快照 (用於訂單紀錄顯示，不用再去撈 Product)
data class SwapOrderItem(
    val productId: String = "",
    val ownerId: String = "", 
    val title: String = "",
    val imageUrl: String = "",
    val quantity: Int = 1
)