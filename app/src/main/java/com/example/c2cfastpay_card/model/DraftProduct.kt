package com.example.c2cfastpay_card.model

data class DraftProduct(
    val imageUri: String = "",
    // 傳遞所有照片
    val images: List<String> = emptyList(),
    // 【關鍵】傳遞「相機實拍」的照片清單 (用來在下一頁鎖定不給刪)
    val cameraImages: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val story: String = "",
    val condition: String = "",
    val fromAI: Boolean = false
)