package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.c2cfastpay_card.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay 
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// 註冊頁面的 ViewModel
// 負責：表單驗證、註冊帳號、發送驗證信、重發冷卻計時
class RegisterViewModel : ViewModel() {
    // UI 狀態
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var username by mutableStateOf("")
    var privacyPolicyChecked by mutableStateOf(false)

    // 錯誤訊息
    var errorMessage by mutableStateOf("")

    // 註冊按鈕是否可按
    var isRegisterEnabled by mutableStateOf(false)

    // 是否正在處理中
    var isLoading by mutableStateOf(false)

    // 倒數計時變數 (秒)
    var resendCountDown by mutableStateOf(0)

    fun updateEmail(email: String) {
        this.email = email.trim()
        checkRegistrationValidity()
    }

    fun updatePassword(password: String) {
        this.password = password
        checkRegistrationValidity()
    }

    fun updateConfirmPassword(confirmPassword: String) {
        this.confirmPassword = confirmPassword
        checkRegistrationValidity()
    }

    fun updateUsername(username: String) {
        this.username = username.trim()
        checkRegistrationValidity()
    }

    fun updatePrivacyPolicyChecked(checked: Boolean) {
        privacyPolicyChecked = checked
        checkRegistrationValidity()
    }

    // 檢查所有欄位是否合法
    // 只要有任何一項不符合，就停用註冊按鈕，並顯示對應錯誤
    private fun checkRegistrationValidity() {
        val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        val isPasswordValid = password.length >= 6
        val isConfirmPasswordValid = password == confirmPassword
        val isUsernameValid = username.isNotBlank()

        isRegisterEnabled = isEmailValid && isPasswordValid && isConfirmPasswordValid && isUsernameValid && privacyPolicyChecked

        errorMessage = when {
            !isUsernameValid && username.isNotEmpty() -> "請輸入帳號名稱"
            !isEmailValid && email.isNotEmpty() -> "請輸入有效的電子信箱"
            !isPasswordValid && password.isNotEmpty() -> "密碼長度至少需 6 個字元"
            !isConfirmPasswordValid && confirmPassword.isNotEmpty() -> "兩次輸入的密碼不一致"
            !privacyPolicyChecked -> "請勾選同意隱私權條款"
            else -> ""
        }
        // 如果全部清空，清除錯誤訊息
        if(email.isEmpty() && password.isEmpty() && confirmPassword.isEmpty() && username.isEmpty()) errorMessage = ""
    }

    // 啟動倒數計時器 
    // 防止使用者連續發送驗證信
    fun startResendTimer() {
        resendCountDown = 30 // 
        viewModelScope.launch {
            while (resendCountDown > 0) {
                delay(1000)
                resendCountDown--
            }
        }
    }

    // 執行註冊 / 重發驗證信
    fun register(onSuccess: () -> Unit) {
        // 基本檢查：是否啟用、是否處理中、是否在冷卻中
        if (isRegisterEnabled && !isLoading) {
            // 如果正在倒數中，禁止執行
            if (resendCountDown > 0) return

            isLoading = true
            errorMessage = ""
            viewModelScope.launch {
                try {
                    val auth = FirebaseAuth.getInstance()
                    val db = FirebaseFirestore.getInstance()

                    // 檢查是否已經登入 
                    var firebaseUser = auth.currentUser

                    if (firebaseUser == null) {
                        // 建立 Auth 帳號
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        firebaseUser = result.user
                    }

                    if (firebaseUser != null) {
                        // 建立 User 資料 (如果還沒建立)
                        val docRef = db.collection("users").document(firebaseUser.uid)
                        val docSnap = docRef.get().await()

                        if (!docSnap.exists()) {
                            // 建立使用者資料文件
                            val newUser = User(
                                id = firebaseUser.uid,
                                email = email,
                                name = username,
                                avatarUrl = "", // 預設無頭像
                                points = 10000  // 註冊送積分
                            )
                            docRef.set(newUser).await()
                        }

                        // 發送驗證信
                        firebaseUser.sendEmailVerification().await()

                        // 發送成功後，啟動計時器 
                        startResendTimer()

                        withContext(Dispatchers.Main) {
                            isLoading = false
                            // 提示使用者去收信，而不是直接跳轉登入
                            errorMessage = "驗證信已發送！若未收到，請檢查垃圾郵件匣。"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        // 將 Firebase 的英文錯誤訊息轉為中文
                        errorMessage = when {
                            e.message?.contains("The email address is already in use") == true -> "此信箱已被註冊"
                            e.message?.contains("The email address is badly formatted") == true -> "信箱格式錯誤"
                            e.message?.contains("Password should be at least 6 characters") == true -> "密碼長度不足"
                            else -> "註冊失敗：${e.message}"
                        }
                    }
                }
            }
        }
    }
}