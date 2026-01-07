package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.data.NotificationItem
import com.example.c2cfastpay_card.UIScreen.components.WishItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 上架商品的 ViewModel (大腦)
// 負責處理：使用者輸入的資料 -> 整理格式 -> 交給 Repository 上傳 -> 發送通知
class AddProductViewModel(private val repository: ProductRepository) : ViewModel() {

    // UI 狀態 
    var isLoading by mutableStateOf(false)
    var uploadStatus by mutableStateOf<String?>(null)

    // 提交商品
    fun submitProduct(
        title: String,
        description: String,
        story: String,
        price: String,
        stock: String,
        condition: String,
        logistics: Set<String>,
        photoUris: List<Uri>,
        wishUuid: String? = null, // 接收願望 ID
        onSuccess: () -> Unit
    ) {
        if (isLoading) return
        isLoading = true
        uploadStatus = null

        viewModelScope.launch {
            try {
                // 獲取當前使用者的 ID
                val currentUser = FirebaseAuth.getInstance().currentUser
                val myOwnerId = currentUser?.uid ?: ""
                val myOwnerName = currentUser?.displayName ?: "使用者"

                // 將物流 Set 轉成字串 
                val logisticsStr = logistics.joinToString("、")

                // 這裡傳入暫時的 URI，Repository 內部會上傳後替換成真實 URL
                val firstImageUri = photoUris.firstOrNull()?.toString() ?: ""

                // 建立商品資料物件
                val newProduct = ProductItem(
                    title = title,
                    description = description,
                    story = story,
                    price = price,
                    stock = stock,
                    condition = condition,
                    payment = logisticsStr,
                    ownerId = myOwnerId,     // 設定擁有者 ID
                    ownerName = myOwnerName, // 設定擁有者名稱
                    imageUri = firstImageUri
                )

                // 上架 (repository.addProduct 回傳的是 Boolean)
                val isSuccess = repository.addProduct(newProduct, photoUris)

                if (isSuccess) {
                    // 從物件本身拿到 ID 
                    val newProductId = newProduct.id

                    // 願望通知邏輯
                    if (!wishUuid.isNullOrEmpty()) {
                        sendWishNotification(wishUuid, title, newProductId)
                    }

                    uploadStatus = "上架成功！"
                    onSuccess()
                } else {
                    uploadStatus = "上架失敗 (Repository 回傳 false)"
                }

            } catch (e: Exception) {
                uploadStatus = "上架發生錯誤: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // 抽離通知邏輯，保持主程式碼乾淨
    private suspend fun sendWishNotification(wishUuid: String, productTitle: String, productId: String) {
        try {
            Log.d("Notification", "檢測到許願 ID: $wishUuid")
            val db = FirebaseFirestore.getInstance()

            // 查詢願望以取得許願者 ID (ownerId)
            val wishSnapshot = db.collection("wishes").document(wishUuid).get().await()
            val wishItem = wishSnapshot.toObject(WishItem::class.java)

            if (wishItem != null && wishItem.ownerId.isNotBlank()) {
                val notif = NotificationItem(
                    userId = wishItem.ownerId, // 通知許願者
                    type = "WISH_FULFILLED",
                    title = "✨ 您的願望成真了！",
                    message = "有人上架了您許願的商品【$productTitle】，快去看看！",
                    targetId = productId // 導向新商品
                )
                db.collection("notifications").document(notif.id).set(notif).await()
                Log.d("Notification", "願望通知已發送給 ${wishItem.ownerId}")
            }
        } catch (e: Exception) {
            Log.e("Notification", "發送通知失敗", e)
        }
    }

    fun clearStatus() {
        uploadStatus = null
    }
}

class AddProductViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}