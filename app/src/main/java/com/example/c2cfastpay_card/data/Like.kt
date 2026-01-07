package com.example.c2cfastpay_card.data

data class Like(
    val id: String = "",          // 文件 ID 
    val likerId: String = "",     // 誰按了喜歡 (我)
    val likerName: String = "",   // 我的名字
    val productId: String = "",   // 喜歡哪個商品
    val productOwnerId: String = "", // 那個商品是誰的 (賣家)
    val timestamp: Long = System.currentTimeMillis()
)