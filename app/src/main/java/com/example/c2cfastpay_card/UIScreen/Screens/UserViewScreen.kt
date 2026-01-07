package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.data.NotificationItem
import com.example.c2cfastpay_card.data.User
import com.example.c2cfastpay_card.UIScreen.components.NotificationRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration // 新增 Import
import com.google.firebase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// 使用者個人頁面的 ViewModel
// 負責：資料監聽、儲值、頭像上傳、暱稱修改、重設密碼
class UserViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    // 通知 Repository 用於監聽未讀數 
    private val notificationRepository = NotificationRepository()

    // 未讀數量狀態 (給 UI 顯示紅點用) 
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // 使用者資料狀態
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // 載入中狀態
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 用來發送單次提示訊息 (Toast)
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // 儲存監聽器，以便在 ViewModel 銷毀時移除
    private var userListener: ListenerRegistration? = null

    // 重設密碼倒數計時 (單位：秒)，UI 會讀取此變數來顯示
    var resetPasswordCountDown by mutableStateOf(0)

    init {
        startListeningUserData()
        startListeningNotifications()
    }

    // 監聽未讀通知數量
    private fun startListeningNotifications() {
        viewModelScope.launch {
            notificationRepository.getUnreadCountFlow().collect { count ->
                _unreadCount.value = count
            }
        }

    }

    // 改用即時監聽資料
    private fun startListeningUserData() {
        val uid = auth.currentUser?.uid ?: return

        // 移除舊的監聽器 (避免重複)
        userListener?.remove()

        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserViewModel", "監聽使用者資料失敗", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // 自動將資料轉換為 User 物件並更新 StateFlow
                    _user.value = snapshot.toObject(User::class.java)
                    // Log.d("UserViewModel", "使用者資料已自動更新")
                }
            }
    }

    // 儲值功能
    fun addPoints(amount: Int) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // 使用原子操作增加數值
                db.collection("users").document(userId)
                    .update("points", FieldValue.increment(amount.toLong()))
                    .await()

                // 不需要再手動呼叫 fetchUserData() 了，因為監聽器會自動收到更新

                _toastMessage.emit("儲值成功！增加 $amount 點")

            } catch (e: Exception) {
                Log.e("UserViewModel", "儲值失敗", e)
                _toastMessage.emit("儲值失敗: ${e.message}")
            }
        }
    }

    // 上傳頭像
    fun uploadAvatar(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 上傳至 Firebase Storage
                val ref = storage.reference.child("users/$uid/avatar.jpg")
                ref.putFile(uri).await()

                // 取得下載連結
                val downloadUrl = ref.downloadUrl.await().toString()

                // 更新 User 文件
                db.collection("users").document(uid)
                    .update("avatarUrl", downloadUrl)
                    .await()

                // 不需要手動 fetchUserData()
                _toastMessage.emit("大頭貼更新成功")
            } catch (e: Exception) {
                Log.e("UserViewModel", "上傳失敗", e)
                _toastMessage.emit("上傳失敗，請稍後再試")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        val trimmedName = newName.trim()

        if (trimmedName.isBlank()) {
            viewModelScope.launch { _toastMessage.emit("暱稱不能為空") }
            return
        }

        if (trimmedName == _user.value?.name) return // 沒改動

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 檢查是否有其他人使用此名稱
                val query = db.collection("users")
                    .whereEqualTo("name", trimmedName)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    _toastMessage.emit("此帳號名稱已被使用，請換一個")
                } else {

                    // 批次更新
                    val batch = db.batch()
                    // 更新使用者本人的資料
                    val userRef = db.collection("users").document(uid)
                    batch.update(userRef, "name", trimmedName)

                    // 搜尋並更新所有「上架商品 (products)」的 ownerName
                    val productsQuery = db.collection("products")
                        .whereEqualTo("ownerId", uid)
                        .get()
                        .await()

                    for (doc in productsQuery.documents) {
                        batch.update(doc.reference, "ownerName", trimmedName)
                    }

                    // 搜尋並更新所有「願望清單 (wishes)」的 ownerName
                    // 注意：WishItem 在資料庫的欄位是 userId
                    val wishesQuery = db.collection("wishes")
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()

                    for (doc in wishesQuery.documents) {
                        batch.update(doc.reference, "ownerName", trimmedName)
                    }

                    // 一次性提交所有變更
                    batch.commit().await()
                    _toastMessage.emit("暱稱與所有商品資料已更新")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "更新暱稱失敗", e)
                _toastMessage.emit("更新失敗: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 啟動倒數計時器
    private fun startResetPasswordTimer() {
        resetPasswordCountDown = 60 // 設定 60 秒
        viewModelScope.launch {
            while (resetPasswordCountDown > 0) {
                delay(1000)
                resetPasswordCountDown--
            }
        }
    }

    // 發送重設密碼信
    fun sendResetPasswordEmail() {
        // 防呆檢查：如果正在倒數，直接跳出
        if (resetPasswordCountDown > 0) {
            viewModelScope.launch { _toastMessage.emit("請稍後再試 (${resetPasswordCountDown}s)") }
            return
        }

        val email = auth.currentUser?.email
        if (email != null) {
            // 立即啟動倒數，不用等 Firebase 回傳 
            startResetPasswordTimer()
            viewModelScope.launch { _toastMessage.emit("正在發送重設信...") }

            // 執行發送
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    viewModelScope.launch { _toastMessage.emit("重設信已發送至信箱") }
                }
                .addOnFailureListener {
                    // 維持倒數，避免使用者一直按
                    viewModelScope.launch { _toastMessage.emit("發送失敗: ${it.message}") }
                }
        } else {
            viewModelScope.launch { _toastMessage.emit("無法取得電子信箱") }
        }
    }

    // 測試通知函式
    fun sendTestNotification() {
        val uid = auth.currentUser?.uid ?: return

        val notif = NotificationItem(
            userId = uid, // 發給自己
            type = "SYSTEM",
            title = "測試通知成功！",
            message = "Firestore 寫入功能正常運作中。",
            targetId = ""
        )

        db.collection("notifications").add(notif)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _toastMessage.emit("測試通知已發送！請去通知中心查看")
                }
            }
            .addOnFailureListener { e ->
                viewModelScope.launch {
                    _toastMessage.emit("發送失敗: ${e.message}")
                }
                Log.e("TestNotif", "Error", e)
            }
    }

    // 登出
    fun logout(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}