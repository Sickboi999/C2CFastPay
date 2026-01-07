package com.example.c2cfastpay_card.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument


// 引用 ViewModel
import com.example.c2cfastpay_card.UIScreen.Screens.ProductFlowViewModel

import com.example.c2cfastpay_card.UIScreen.Screens.AIChatScreen
import com.example.c2cfastpay_card.UIScreen.Screens.AddProductScreen
import com.example.c2cfastpay_card.UIScreen.Screens.AddProductStepOne
import com.example.c2cfastpay_card.UIScreen.Screens.AddWishScreen
import com.example.c2cfastpay_card.UIScreen.Screens.CardStackScreen
import com.example.c2cfastpay_card.UIScreen.Screens.CartScreen
import com.example.c2cfastpay_card.UIScreen.Screens.ChatScreen
import com.example.c2cfastpay_card.UIScreen.Screens.ForgotPasswordScreen
import com.example.c2cfastpay_card.UIScreen.Screens.HistoryScreen
import com.example.c2cfastpay_card.UIScreen.Screens.LoginScreen
import com.example.c2cfastpay_card.UIScreen.Screens.MyProductsScreen
import com.example.c2cfastpay_card.UIScreen.Screens.ProductDetailScreen
import com.example.c2cfastpay_card.UIScreen.Screens.RegisterScreen
import com.example.c2cfastpay_card.UIScreen.Screens.SaleProductPage
import com.example.c2cfastpay_card.UIScreen.Screens.UserScreen
import com.example.c2cfastpay_card.UIScreen.Screens.WishOrProductScreen
import com.example.c2cfastpay_card.UIScreen.Screens.WishPreviewPage
import com.example.c2cfastpay_card.UIScreen.Screens.WishDetailScreen
import com.example.c2cfastpay_card.UIScreen.components.WishRepository
import com.example.c2cfastpay_card.UIScreen.Screens.OrderHistoryScreen
import com.google.gson.Gson
import com.example.c2cfastpay_card.UIScreen.Screens.EditProductScreen
import com.example.c2cfastpay_card.UIScreen.Screens.EditWishScreen
import com.example.c2cfastpay_card.UIScreen.Screens.NotificationScreen
import com.example.c2cfastpay_card.UIScreen.Screens.UserProductScreen


@Composable
fun AppNavigationGraph(
    navController: NavHostController, // 負責執行跳轉動作
    startDestination: String          // App 一打開顯示的第一個畫面
) {
    // 建立共用的 ViewModel
    val productFlowViewModel: ProductFlowViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // 登入
        composable(route = Screen.Login.route) {
            LoginScreen(
                navController = navController,
                onSwitchToRegister = { navController.navigate(Screen.Register.route) },
                onForgetPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
            )
        }

        // 註冊
        composable(route = Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                onSwitchToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // 忘記密碼
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                navController = navController,
                onConfirmSuccess = { navController.popBackStack() },
                onSwitchToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // 主功能頁面
        composable(route = Screen.CardStack.route) {
            CardStackScreen(navController = navController)
        }

        composable(route = Screen.History.route) {
            HistoryScreen(navController = navController)
        }

        composable(route = Screen.Sale.route) {
            SaleProductPage(navController = navController)
        }

        composable(route = Screen.WishList.route) {
            WishPreviewPage(navController = navController)
        }

        // 新增選擇頁 (許願或商品)
        composable(route = Screen.WishOrProduct.route) {
            WishOrProductScreen(navController = navController)
        }

        // 【流程】上架第一步：拍照/選圖 (AddStep1)
        composable(route = Screen.AddStep1.route) {
            AddProductStepOne(
                navController = navController,
                flowViewModel = productFlowViewModel
            )
        }

        // 【流程】AI 上架助手 (AIChat) 
        composable(
            route = "ai_chat?imageUri={imageUri}",
            arguments = listOf(navArgument("imageUri") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            // 取得傳遞過來的 imageUri 參數
            val imageUri = backStackEntry.arguments?.getString("imageUri")

            AIChatScreen(
                navController = navController,
                flowViewModel = productFlowViewModel,
                imageUri = imageUri // 將參數傳入 Screen
            )
        }

        composable(Screen.OrderHistory.route) {
            OrderHistoryScreen(navController)
        }

        // 上架填寫頁 (AddProduct)
        composable(
            route = "add_product?draftJson={draftJson}&wishUuid={wishUuid}",
            arguments = listOf(
                navArgument("draftJson") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("wishUuid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val context = LocalContext.current
            // 使用 remember，確保 Repository 不會一直重複建立
            val wishRepository = remember { WishRepository(context) }

            // 取出參數
            val draftJson = backStackEntry.arguments?.getString("draftJson")
            val wishUuid = backStackEntry.arguments?.getString("wishUuid")

            // 狀態變數：用來存最後要顯示的 JSON
            var finalJsonForScreen by remember { mutableStateOf<String?>(draftJson) }
            var isLoading by remember { mutableStateOf(false) }

            // 【邏輯】如果有傳 wishUuid 進來，代表使用者想把 "許願" 轉成 "商品"
            // 所以我們要去資料庫抓那個許願的資料，然後填入 finalJsonForScreen
            LaunchedEffect(wishUuid) {
                if (wishUuid != null && wishUuid != "null" && wishUuid.isNotEmpty()) {
                    isLoading = true
                    val wishItem = wishRepository.getWishByUuid(wishUuid)
                    if (wishItem != null) {
                        finalJsonForScreen = Gson().toJson(wishItem)
                    }
                    isLoading = false
                }
            }

            // 根據讀取狀態顯示不同畫面
            if (!isLoading) {
                AddProductScreen(
                    navController = navController,
                    draftJson = finalJsonForScreen
                )
            } else {
                // 讀取中轉圈圈
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // 其他功能 

        composable(route = Screen.AddWish.route) {
            AddWishScreen(navController = navController)
        }
        composable(
            route = "user_product/{userId}", // 定義路徑，{userId} 是參數
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            // 從路徑取出 userId
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            // 呼叫剛剛寫好的 UserProductScreen
            UserProductScreen(navController = navController, userId = userId)
        }

        composable(Screen.EditWish.route) { backStackEntry ->
            val wishId = backStackEntry.arguments?.getString("wishId") ?: ""
            EditWishScreen(navController, wishId)
        }

        composable(Screen.EditProduct.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            EditProductScreen(navController, productId)
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                navController = navController,
                productId = productId
            )
        }

        composable(route = Screen.Cart.route) {
            CartScreen(navController = navController)
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("matchId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            ChatScreen(
                navController = navController,
                matchId = matchId
            )
        }

        // 會員相關頁面
        composable(route = Screen.User.route) {
            UserScreen(navController = navController)
        }

        composable(route = Screen.MyProducts.route) {
            MyProductsScreen(navController = navController)
        }

        composable(
            route = Screen.WishDetail.route,
            arguments = listOf(navArgument("wishId") { type = NavType.StringType })
        ) { backStackEntry ->
            // 取得參數
            val id = backStackEntry.arguments?.getString("wishId") ?: ""

            // 呼叫畫面 (參數名要對應 WishDetailScreen 的定義)
            WishDetailScreen(
                navController = navController,
                wishId = id // ★★★ 這裡要用 wishId ★★★
            )
        }

        composable(Screen.Notification.route) { NotificationScreen(navController) }
    }
}