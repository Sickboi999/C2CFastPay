package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.c2cfastpay_card.navigation.Screen
import com.example.c2cfastpay_card.ui.theme.C2CFastPay_CardTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    onSwitchToLogin: () -> Unit
) {
    val viewModel: RegisterViewModel = viewModel()
    val primaryColor = Color(0xFF487F81)
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("註冊帳號") },
                navigationIcon = {
                    IconButton(onClick = { onSwitchToLogin() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Spacer(modifier = Modifier.height(20.dp))

            // 帳號輸入框 (這裡也要補上強制黑色文字，避免深色模式看不到)
            OutlinedTextField(
                value = viewModel.username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text("帳號名稱 (顯示名稱)") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = viewModel.errorMessage.contains("帳號"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 信箱
            OutlinedTextField(
                value = viewModel.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("電子信箱") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                isError = viewModel.errorMessage.contains("信箱"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 密碼
            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("設定密碼") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),

                // 顯示切換
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = Color.Gray
                        )
                    }
                },

                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                isError = viewModel.errorMessage.contains("密碼"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 確認密碼
            OutlinedTextField(
                value = viewModel.confirmPassword,
                onValueChange = { viewModel.updateConfirmPassword(it) },
                label = { Text("確認密碼") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),

                // 顯示切換
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = Color.Gray
                        )
                    }
                },

                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                isError = viewModel.errorMessage.contains("不一致"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 隱私條款
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = viewModel.privacyPolicyChecked,
                    onCheckedChange = { viewModel.updatePrivacyPolicyChecked(it) },
                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                )
                Text("我已閱讀並同意 ", fontSize = 14.sp, color = Color.Black)
                Text(
                    text = "隱私條款",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { showPrivacyDialog = true }
                )
            }

            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    containerColor = Color.White,
                    title = { Text(text = "隱私權條款", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = """
                                    (隱私權條款內容...)
                                """.trimIndent(),
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text("我了解了", color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.register(
                        onSuccess = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                enabled = viewModel.isRegisterEnabled && !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("立即註冊", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (viewModel.errorMessage.contains("驗證信已發送")) { // 這裡文字要跟 ViewModel 一致
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.register(
                            onSuccess = {
                                navController.navigate(Screen.Login.route)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    // 如果倒數中，禁用按鈕 
                    enabled = viewModel.resendCountDown == 0 && !viewModel.isLoading
                ) {
                    // 顯示倒數秒數 
                    if (viewModel.resendCountDown > 0) {
                        Text("重新發送 (${viewModel.resendCountDown}s)")
                    } else {
                        Text("重新發送驗證郵件")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    C2CFastPay_CardTheme {
        RegisterScreen(rememberNavController(), {})
    }
}