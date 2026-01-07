package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ProductFlowViewModel : ViewModel() {
    // 儲存所有照片列表
    val photoUris = mutableStateOf<List<Uri>>(emptyList())

    // 改用 List 來追蹤哪些 URI 是來自相機
    val cameraUris = mutableStateListOf<Uri>()

    // 計算屬性：只要 cameraUris 裡面有東西，就代表有實拍照片
    val hasCameraPhoto: Boolean
        get() = cameraUris.isNotEmpty()

    // AI 相關 
    var userInput = mutableStateOf("")
    var productName = mutableStateOf("")
    var aiDescription = mutableStateOf("")
    var finalStory = mutableStateOf("")
    var currentImageUri = mutableStateOf<Uri?>(null)
}