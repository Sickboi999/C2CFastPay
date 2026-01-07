package com.example.c2cfastpay_card.model

data class DraftProduct(
    val imageUri: String = "",
    // 傳遞所有照片
    val images: List<String> = emptyList(),
    // 傳遞「相機實拍」的照片清單
    val cameraImages: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val story: String = "",
    val condition: String = "",
    val fromAI: Boolean = false
)