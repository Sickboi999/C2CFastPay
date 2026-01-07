package com.example.c2cfastpay_card.navigation

// 集中管理所有頁面的網址
sealed class Screen(val route: String) {

    object CardStack : Screen("card_stack_screen") // 滑動配對
    object History : Screen("history_screen")      // 歷史紀錄
    object Sale : Screen("sale_screen")            // 販售頁
    object WishList : Screen("wish_list_screen")   // 許願池

    object AddProduct : Screen("add_product?wishUuid={wishUuid}")
    object AddWish : Screen("add_wish_screen")

    // 會員登入相關
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object ForgotPassword : Screen("forgot_password_screen")

    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    object Cart : Screen("cart_screen")

    // 聊天室 (傳入配對 ID)
    object Chat : Screen("chat_screen/{matchId}") {
        fun createRoute(matchId: String) = "chat_screen/$matchId"
    }
    // object Profile : Screen("profile_screen")

    // 上架流程
    object AddStep1 : Screen("add_step1")
    object WishOrProduct : Screen("wish_or_product")

    // 會員中心
    object User : Screen("user_screen")
    object MyProducts : Screen("my_products_screen")
    object OrderHistory : Screen("order_history_screen")

    // 編輯功能
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }

    object WishDetail : Screen("wish_detail/{wishId}") {
        fun createRoute(wishId: String) = "wish_detail/$wishId"
    }

    object EditWish : Screen("edit_wish/{wishId}") {
        fun createRoute(wishId: String) = "edit_wish/$wishId"
    }

    object Notification : Screen("notification_screen")
}