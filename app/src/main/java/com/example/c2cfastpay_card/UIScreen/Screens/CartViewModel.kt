package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.data.CartItem
import com.example.c2cfastpay_card.UIScreen.components.CartRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// 購物車頁面的 ViewModel
class CartViewModel(private val cartRepository: CartRepository) : ViewModel() {

    // 購物車商品列表狀態
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    // Loading 狀態
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Toast 訊息
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            // 監聽 Repository 傳來的即時資料流
            cartRepository.getCartItemsFlow().collect { remoteItems ->
                // 先取得目前 UI 上已經存在的商品列表 (包含使用者目前的勾選狀態)
                val currentItems = _cartItems.value
                // 將新抓到的資料 (remote) 與目前的資料 (current) 比對合併
                val mergedItems = remoteItems.map { remote ->
                    // 檢查這個商品原本有沒有被勾選，有的話保留狀態
                    val oldItem = currentItems.find { it.id == remote.id }
                    // 如果有找到舊狀態，就沿用之前的 isChecked；否則預設為 false (未勾選)
                    remote.copy(isChecked = oldItem?.isChecked ?: false)
                }
                // 更新 UI 狀態
                _cartItems.value = mergedItems
            }
        }
    }

    // 回傳格式化後的字串 (例如 "1,200") 
    val totalPrice: StateFlow<String> = _cartItems.map { items ->
        // 篩選出被勾選的商品並計算總合
        val sum = items.filter { it.isChecked }.sumOf { item ->
            // 處理價格字串
            val price = item.productPrice.replace(",", "").toLongOrNull() ?: 0L
            price * item.quantity
        }
        // 格式化數字 (例如: 12000 -> "12,000")
        NumberFormat.getNumberInstance(Locale.US).format(sum)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0")

    // 使用者操作 

    // 切換商品的勾選狀態
    fun toggleItemChecked(item: CartItem) {
        _cartItems.value = _cartItems.value.map {
            if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
        }
    }

    // 增加商品數量
    fun increaseQuantity(item: CartItem) {
        // 檢查是否超過庫存
        if (item.quantity < item.stock) {
            // 更新資料庫
            viewModelScope.launch {
                cartRepository.updateCartItem(item.copy(quantity = item.quantity + 1))
            }
        } else {
            // 庫存不足提示
            viewModelScope.launch { _toastMessage.emit("庫存不足") }
        }
    }

    //減少商品數量 (最少為 1)
    fun decreaseQuantity(item: CartItem) {
        if (item.quantity > 1) {
            // 更新資料庫
            viewModelScope.launch {
                cartRepository.updateCartItem(item.copy(quantity = item.quantity - 1))
            }
        }
    }

    // 單一刪除
    fun removeItem(itemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            cartRepository.removeFromCart(itemId)
            _isLoading.value = false
            _toastMessage.emit("商品已刪除")
        }
    }

    // 批量刪除
    fun removeCartItems(ids: List<String>) {
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            cartRepository.removeCartItems(ids) // 呼叫 Repository 的批量刪除
            _isLoading.value = false
            _toastMessage.emit("已刪除 ${ids.size} 項商品")
        }
    }

    // 結帳
    fun checkout() {
        // 篩選出有勾選的商品
        val itemsToBuy = _cartItems.value.filter { it.isChecked }

        // 防呆：如果沒勾選商品，發送提示並返回
        if (itemsToBuy.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("請先勾選商品") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // 呼叫 Repository 執行結帳 (建立訂單 -> 扣庫存 -> 清除購物車)
            val result = cartRepository.checkout(itemsToBuy)
            _isLoading.value = false

            // 根據結果顯示成功或失敗訊息
            result.onSuccess { msg ->
                _toastMessage.emit(msg)
            }.onFailure { e ->
                _toastMessage.emit("結帳失敗: ${e.message}")
            }
        }
    }
}

// Factory
class CartViewModelFactory(private val repository: CartRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}