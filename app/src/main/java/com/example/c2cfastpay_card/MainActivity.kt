package com.example.c2cfastpay_card

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.c2cfastpay_card.navigation.AppNavigationGraph
import com.example.c2cfastpay_card.navigation.Screen 
import com.example.c2cfastpay_card.ui.theme.C2CFastPay_CardTheme
import com.google.firebase.auth.FirebaseAuth 

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. 檢查登入狀態
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 2. 決定起始頁面
        // 如果有使用者 且 Email 已驗證 -> 直接去販售首頁
        // 否則 -> 去登入頁
        val startDestination = if (currentUser != null && currentUser.isEmailVerified) {
            Screen.Sale.route
        } else {
            Screen.Login.route
        }

        setContent {
            C2CFastPay_CardTheme {
                val navController = rememberNavController()

                // 3. 將起始頁面傳進去
                AppNavigationGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}