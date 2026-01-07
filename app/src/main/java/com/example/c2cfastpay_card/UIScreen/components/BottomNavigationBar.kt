package com.example.c2cfastpay_card.UIScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.c2cfastpay_card.R
import com.example.c2cfastpay_card.UIScreen.Screens.UserViewModel
import com.example.c2cfastpay_card.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val userViewModel: UserViewModel = viewModel()
    val unreadCount by userViewModel.unreadCount.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home (Sale)
        IconButton(onClick = {
            navController.navigate(Screen.Sale.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true } // 保存頁面狀態
                launchSingleTop = true  // 避免重複開啟同一頁
                restoreState = true     // 恢復之前保存的狀態
            }
        }) {
            Icon(painter = painterResource(R.drawable.img_2), contentDescription = "Home")
        }

        // WishOrProduct
        IconButton(onClick = {
            navController.navigate(Screen.WishOrProduct.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }) {
            Icon(painter = painterResource(R.drawable.img_3), contentDescription = "Add")
        }

        // CardStack
        IconButton(onClick = {
            navController.navigate(Screen.CardStack.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }) {
            Icon(painter = painterResource(R.drawable.img_4), contentDescription = "Connect")
        }

        // Chat (History)
        IconButton(onClick = {
            navController.navigate(Screen.History.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }) {
            Icon(painter = painterResource(R.drawable.img_5), contentDescription = "Chat")
        }

        // User
        IconButton(onClick = {
            navController.navigate(Screen.User.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }) {
            BadgedBox(
                badge = {
                    // 未讀數量大於 0 時顯示紅點
                    if (unreadCount > 0) {
                        Badge {
                            // 如果數量超過 99，顯示 "99+"，否則顯示數字
                            val displayCount = if (unreadCount > 99) "99+" else unreadCount.toString()
                            Text(text = displayCount)
                        }
                    }
                }
            ) {
                // 紅點
                Icon(painter = painterResource(R.drawable.img_6), contentDescription = "User")
            }
        }
    }
}