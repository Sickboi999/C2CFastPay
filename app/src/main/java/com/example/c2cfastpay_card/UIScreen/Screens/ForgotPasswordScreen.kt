package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.c2cfastpay_card.ui.theme.C2CFastPay_CardTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    onConfirmSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit = {}
) {
    // UI 狀態 
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableStateOf(0) }

    // 計時器邏輯 
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds--
        }
    }

    val primaryColor = Color(0xFF487F81)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("重設密碼") },
                navigationIcon = {
                    IconButton(onClick = { onSwitchToLogin() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "忘記密碼了嗎？",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "請輸入您的註冊信箱，我們將寄送重設密碼連結給您。",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 信箱輸入框
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    errorMessage = ""
                    successMessage = ""
                },
                label = { Text("電子信箱") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                isError = errorMessage.isNotEmpty()
            )

            // 錯誤訊息顯示
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                )
            }

            // 成功訊息顯示
            if (successMessage.isNotEmpty()) {
                Text(
                    text = successMessage,
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 發送按鈕
            Button(
                onClick = {
                    // 基本格式檢查
                    if (email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        isLoading = true
                        errorMessage = ""
                        successMessage = ""

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                // 是否有這個 Email 的使用者
                                val querySnapshot = db.collection("users")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .await()

                                if (querySnapshot.isEmpty) {
                                    // 找不到使用者
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        errorMessage = "此信箱尚未註冊 C2C FastPay 帳號"
                                    }
                                } else {
                                    // 找到使用者，發送重設信
                                    FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()

                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        successMessage = "重設信已發送！請檢查您的信箱"
                                        // 發送成功，開始倒數 60 秒 
                                        cooldownSeconds = 60
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMessage = when {
                                        e.message?.contains("badly formatted") == true -> "信箱格式錯誤"
                                        else -> "處理失敗：${e.message}"
                                    }
                                }
                            }
                        }
                    } else {
                        errorMessage = "請輸入有效的信箱"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                // 倒數時禁用按鈕 
                enabled = !isLoading && cooldownSeconds == 0
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    // 顯示倒數秒數
                    Text(
                        text = if (cooldownSeconds > 0) "重新發送 (${cooldownSeconds}s)" else "發送重設信",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onSwitchToLogin() }) {
                Text("返回登入", color = Color.Gray)
            }
        }
    }
}