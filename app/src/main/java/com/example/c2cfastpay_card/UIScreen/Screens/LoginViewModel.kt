package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// 登入頁面的 ViewModel
// 負責處理：輸入驗證、帳號反查 Email、Firebase 登入驗證
class LoginViewModel : ViewModel() {
    // 改名為 loginInput，因為它可能是 Email 也可能是帳號
    var loginInput by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    // errorMessage: 用來顯示在輸入框下方的紅字錯誤
    var errorMessage by mutableStateOf("")
    // isLoading: 控制按鈕是否轉圈圈 (防止重複點擊)
    var isLoading by mutableStateOf(false)

    fun updateInput(input: String) {
        // 移除空白，避免誤觸
        loginInput = input.trim()
    }

    // 更新密碼框
    fun updatePassword(password: String) {
        loginPassword = password
    }

    fun login(onSuccess: () -> Unit) {
        if (loginInput.isBlank() || loginPassword.isBlank()) return

        viewModelScope.launch {
            isLoading = true
            errorMessage = ""
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            try {
                var emailToLogin = loginInput

                // 判斷輸入的是否為 Email 格式
                val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()

                if (!isEmail) {
                    // 如果不是 Email，假設是帳號，去 Firestore 查詢對應的 Email
                    val querySnapshot = db.collection("users")
                        .whereEqualTo("name", loginInput)
                        .limit(1)
                        .get()
                        .await()

                    if (!querySnapshot.isEmpty) {
                        emailToLogin = querySnapshot.documents[0].getString("email") ?: ""
                    } else {
                        throw Exception("找不到此帳號")
                    }
                }

                // 使用 Email 進行登入
                auth.signInWithEmailAndPassword(emailToLogin, loginPassword).await()
                val user = auth.currentUser

                // 切換回 Main Thread 更新 UI
                withContext(Dispatchers.Main) {
                    isLoading = false
                    // 檢查 Email 是否已驗證
                    if (user?.isEmailVerified == true) {
                        onSuccess() // 登入成功，執行跳轉
                    } else {
                        // 雖然帳密對了，但沒驗證信箱
                        auth.signOut()
                        errorMessage = "請先驗證您的電子郵件"
                    }
                }
            } catch (e: Exception) {
                // 錯誤處理
                withContext(Dispatchers.Main) {
                    isLoading = false
                    // 優化錯誤訊息顯示
                    errorMessage = if (e.message?.contains("找不到此帳號") == true) {
                        "帳號不存在"
                    } else {
                        "登入失敗：帳號或密碼錯誤"
                    }
                }
            }
        }
    }
}