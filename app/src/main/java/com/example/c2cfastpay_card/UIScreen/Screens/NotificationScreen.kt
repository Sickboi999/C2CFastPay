package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // ★ 1. 確保有這行 Import
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.c2cfastpay_card.data.NotificationItem
import com.example.c2cfastpay_card.UIScreen.components.NotificationRepository
import com.example.c2cfastpay_card.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// 通知的 ViewModel
// 負責監聽通知資料、標記已讀
class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    // 從 Repository 獲取即時通知列表
    val notifications = repository.getNotificationsFlow()

    // 標記已讀
    fun markRead(id: String) {
        viewModelScope.launch {
            repository.markSingleAsRead(id)
        }
    }

    // 標記全部已讀
    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    val viewModel: NotificationViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // 進入頁面自動消除紅點 (全部已讀)
    LaunchedEffect(Unit) {
        viewModel.markAllRead()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("通知中心", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("目前沒有新通知", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { item ->
                    NotificationRow(item = item, onClick = {    
                        // 先標記這則通知為已讀
                        viewModel.markRead(item.id)

                        // 根據通知類型跳轉到對應頁面
                        when (item.type) {
                            "ORDER" -> navController.navigate(Screen.OrderHistory.route)    // 訂單相關 -> 訂單紀錄
                            "MATCH" -> navController.navigate(Screen.History.route)         // 配對成功 -> 聊天列表
                            "WISH_FULFILLED" -> {
                                if (item.targetId.isNotEmpty()) {
                                    navController.navigate(Screen.ProductDetail.createRoute(item.targetId))
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    // 格式化時間戳記
    val timeStr = remember(item.timestamp) {
        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(item.timestamp.toDate())
    }

    // 根據類型選擇圖示與顏色
    val (icon, iconColor) = when (item.type) {
        "ORDER" -> Icons.Default.ShoppingCart to Color(0xFF487F81)          // 訂單
        "MATCH" -> Icons.Default.Favorite to Color(0xFFE91E63)              // 配對
        "WISH_FULFILLED" -> Icons.Default.AutoAwesome to Color(0xFFFF9800)  // 許願
        else -> Icons.Default.Notifications to Color.Gray                   // 其他
    }

    // 未讀通知顯示白色背景+陰影，已讀則顯示淺灰背景
    val containerColor = if (item.isRead) Color(0xFFF9F9F9) else Color.White
    val elevation = if (item.isRead) 0.dp else 2.dp

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(elevation),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圖示
            Box(
                modifier = Modifier.size(48.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))

            // 文字內容
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (item.isRead) Color.Gray else Color.Black)
                Text(text = item.message, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = timeStr, fontSize = 12.sp, color = Color.LightGray)
            }

            // 未讀，顯示小紅點
            if (!item.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape))
            }
        }
    }
}