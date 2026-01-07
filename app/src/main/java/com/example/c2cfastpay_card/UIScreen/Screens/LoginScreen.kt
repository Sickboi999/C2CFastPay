package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility 
import androidx.compose.material.icons.filled.VisibilityOff 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation 
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.c2cfastpay_card.navigation.Screen
import com.example.c2cfastpay_card.ui.theme.C2CFastPay_CardTheme

@Composable
fun LoginScreen(
    navController: NavController,
    onSwitchToRegister: () -> Unit,     // 切換到註冊頁的回應
    onForgetPasswordClick: () -> Unit   // 點擊忘記密碼的回應
) {
    val viewModel: LoginViewModel = viewModel()
    val primaryColor = Color(0xFF487F81)

    // 密碼顯示狀態 
    var isPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 強制白底
    ) {
        // 頂部裝飾
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "C2C FastPay",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                text = "歡迎回來",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 登入表單
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 帳號輸入框
            OutlinedTextField(
                value = viewModel.loginInput,
                onValueChange = { viewModel.updateInput(it) },
                label = { Text("信箱 / 帳號") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 密碼輸入框
            OutlinedTextField(
                value = viewModel.loginPassword,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("密碼") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),

                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "隱藏密碼" else "顯示密碼",
                            tint = Color.Gray
                        )
                    }
                },

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                isError = viewModel.errorMessage.isNotEmpty()
            )

            // 忘記密碼
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onForgetPasswordClick) {
                    Text("忘記密碼？", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 錯誤訊息顯示
            if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 登入按鈕
            Button(
                onClick = {
                    viewModel.login {
                        navController.navigate(Screen.Sale.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                // 載入中時禁用按鈕，防止重複點擊
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("登入", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 切換至註冊頁
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("還沒有帳號嗎？", color = Color.Gray)
                TextButton(onClick = onSwitchToRegister) {
                    Text("立即註冊", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    C2CFastPay_CardTheme {
        LoginScreen(rememberNavController(), {}, {})
    }
}