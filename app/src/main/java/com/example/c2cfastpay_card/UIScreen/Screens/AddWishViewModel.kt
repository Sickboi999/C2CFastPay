package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.UIScreen.components.WishRepository
import com.example.c2cfastpay_card.UIScreen.components.WishItem
import kotlinx.coroutines.launch

class AddWishViewModel(private val repository: WishRepository) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)

    fun submitWish(
        title: String,
        description: String,
        price: String,
        qty: String,
        note: String,
        condition: String,
        logistics: Set<String>,
        photoUris: List<Uri>,
        onSuccess: () -> Unit
    ) {
        if (isLoading) return
        isLoading = true
        statusMessage = null

        viewModelScope.launch {
            try {
                val logisticsStr = logistics.joinToString("、")
                // 取第一張圖
                val firstImageUri = photoUris.firstOrNull()

                val newWish = WishItem(
                    title = title,
                    description = description,
                    price = price,
                    qty = qty,
                    memo = note, // 對應 memo 欄位
                    condition = condition,
                    payment = logisticsStr
                    // imageUri 會在 Repository 上傳後填入
                    // ownerId 等資訊也會在 Repository 填入
                )

                repository.addWish(newWish, firstImageUri)

                statusMessage = "許願成功！"
                isLoading = false
                onSuccess()

            } catch (e: Exception) {
                isLoading = false
                statusMessage = "許願失敗: ${e.message}"
            }
        }
    }

    fun clearStatus() {
        statusMessage = null
    }
}

// Factory
class AddWishViewModelFactory(private val repository: WishRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddWishViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddWishViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}