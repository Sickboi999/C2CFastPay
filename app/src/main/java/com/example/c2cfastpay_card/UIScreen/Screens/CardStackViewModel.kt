package com.example.c2cfastpay_card.UIScreen.Screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.data.NotificationItem
import com.example.c2cfastpay_card.UIScreen.components.MatchRepository
import com.example.c2cfastpay_card.UIScreen.components.NotificationRepository
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.UIScreen.components.SwipeDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 滑卡配對頁面的 ViewModel
// 負責：載入卡片、處理滑動動作(左滑/右滑)、判斷配對成功
class CardStackViewModel(
    private val productRepository: ProductRepository,          // 商品資料
    private val matchRepository: MatchRepository,              // 按讚/配對邏輯
    private val notificationRepository: NotificationRepository // 通知Repo
) : ViewModel() {

    // 卡片堆疊的資料流 
    private val _cards = MutableStateFlow<List<ProductItem>>(emptyList())
    val cards: StateFlow<List<ProductItem>> = _cards.asStateFlow()

    // 載入讀取狀態
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 觸發「配對成功」彈窗的狀態 
    private val _matchedProduct = MutableStateFlow<ProductItem?>(null)
    val matchedProduct: StateFlow<ProductItem?> = _matchedProduct.asStateFlow()

    init {
        loadPotentialMatches()
    }

    // 載入潛在的配對對象
    // 邏輯：從資料庫撈商品，並過濾掉已經滑過的 (喜歡/不喜歡)
    fun loadPotentialMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 過濾已滑過的卡片邏輯
                val swipedIds = matchRepository.getSwipedProductIds()
                val newCards = productRepository.getProductsForMatching(swipedIds)
                _cards.value = newCards
                Log.d("CardStackViewModel", "成功載入 ${newCards.size} 張未滑過的卡片")
            } catch (e: Exception) {
                Log.e("CardStackViewModel", "載入卡片失敗", e)
                _cards.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 處理左滑 (不喜歡 / Skip)
    fun swipeLeft(product: ProductItem) {
        _cards.update { list -> list.filterNot { it.id == product.id } }
        viewModelScope.launch {
            matchRepository.recordSwipe(product.id, SwipeDirection.LEFT)
        }
    }
    // 處理右滑 (喜歡 / Like)
    fun swipeRight(product: ProductItem) {
        // 先從 UI 移除卡片 (Optimistic Update)
        _cards.update { list -> list.filterNot { it.id == product.id } }

        viewModelScope.launch {
            try {
                val isMatched = matchRepository.likeProduct(product)

                if (isMatched) {
                    Log.d("CardStackViewModel", "配對成功！")

                    // 觸發 UI 彈窗
                    _matchedProduct.value = product

                    // 發送配對成功通知
                    val targetUserId = product.ownerId // 通知商品的擁有者

                    if (targetUserId.isNotBlank()) {
                        val notif = NotificationItem(
                            userId = targetUserId,
                            type = "MATCH",
                            title = "❤️ 配對成功！",
                            message = "恭喜！有人也喜歡了您的商品【${product.title}】，快去查看配對紀錄。",
                            targetId = "MATCH_HISTORY"
                        )
                        notificationRepository.sendNotification(notif)
                    }
                }
            } catch (e: Exception) {
                Log.e("CardStackViewModel", "儲存喜歡失敗", e)
            }
        }
    }

    // 關閉彈窗的方法
    fun dismissMatchPopup() {
        _matchedProduct.value = null
    }
}

class CardStackViewModelFactory(
    private val productRepository: ProductRepository,
    private val matchRepository: MatchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardStackViewModel::class.java)) {
            val notificationRepo = NotificationRepository()
            @Suppress("UNCHECKED_CAST")
            return CardStackViewModel(productRepository, matchRepository, notificationRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}