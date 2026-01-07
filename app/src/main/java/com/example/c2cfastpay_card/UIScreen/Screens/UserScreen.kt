package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.BottomNavigationBar
import com.example.c2cfastpay_card.navigation.Screen

@Composable
fun UserScreen(
    navController: NavController
) {
    val viewModel: UserViewModel = viewModel()

    // 觀察使用者資料與載入狀態
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 暫時用 0 代替未讀數量，直到 ViewModel 更新
    val unreadCount = 0

    val context = LocalContext.current

    // 監聽 ViewModel 的 Toast 訊息
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            if (message.isNotBlank()) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 圖片選擇器 (更換頭像)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(it) } // 選完圖直接上傳
    }

    // UI 狀態 (對話框控制)
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    var showTopUpDialog by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFF487F81)

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .background(Color(0xFFF5F5F5))
                    .verticalScroll(rememberScrollState())
            ) {
                // 頂部個人資料
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(primaryColor, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)), contentAlignment = Alignment.CenterStart) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        // 頭像區域
                        Box(modifier = Modifier.size(84.dp)) {
                            if (!user?.avatarUrl.isNullOrEmpty()) {
                                Image(painter = rememberAsyncImagePainter(user!!.avatarUrl), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White).clickable { imagePicker.launch("image/*") }, contentScale = ContentScale.Crop)
                            } else {
                                Image(painter = rememberVectorPainter(Icons.Default.Person), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White).padding(12.dp).clickable { imagePicker.launch("image/*") }, contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(Color.LightGray))
                            }
                            // 編輯圖示 (疊在頭像右下角)
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).background(Color.Black.copy(0.5f), CircleShape).padding(4.dp))
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // 暱稱與 Email
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { newNameInput = user?.name ?: ""; showEditNameDialog = true }) {
                                Text(user?.name ?: "載入中...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                            }
                            Text(user?.email ?: "", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(12.dp))

                            // 購物金顯示
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = Color.White.copy(0.2f), shape = RoundedCornerShape(50)) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$ ${user?.points ?: 0}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // 儲值按鈕
                                SmallFloatingActionButton(onClick = { showTopUpDialog = true }, containerColor = Color(0xFFFFD700), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 功能選單列表
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                    // 1. 商品管理
                    MenuCard(
                        icon = Icons.Default.ShoppingBag,
                        title = "我的商品管理",
                        subtitle = "查看或下架您上架的商品",
                        onClick = { navController.navigate(Screen.MyProducts.route) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. 訂單紀錄
                    MenuCard(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = "訂單紀錄 (買/賣)",
                        subtitle = "查看歷史交易與銷售狀況",
                        onClick = { navController.navigate(Screen.OrderHistory.route) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. 通知中心 (加入紅點)
                    MenuCard(
                        icon = Icons.Default.Notifications,
                        title = "通知中心",
                        subtitle = "查看配對成功與系統訊息",
                        badgeCount = unreadCount,
                        onClick = { navController.navigate(Screen.Notification.route) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. 修改密碼 (含倒數)
                    MenuCard(
                        icon = Icons.Default.LockReset,
                        title = "修改密碼",
                        subtitle = if (viewModel.resetPasswordCountDown > 0) // <-- 修正
                            "請稍後 ${viewModel.resetPasswordCountDown} 秒再試"
                        else "發送重設密碼信件至信箱",
                        onClick = { viewModel.sendResetPasswordEmail() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. 登出
                    MenuCard(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "登出帳號",
                        textColor = Color.Red,
                        onClick = { viewModel.logout { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } } }
                    )
                }
            }
        }

        // 彈出視窗 (Dialogs)
        // 修改暱稱 Dialog  
        if (showEditNameDialog) {
            AlertDialog(onDismissRequest = { showEditNameDialog = false }, title = { Text("修改暱稱") }, text = { OutlinedTextField(value = newNameInput, onValueChange = { newNameInput = it }, label = { Text("新暱稱") }) }, confirmButton = { Button(onClick = { viewModel.updateName(newNameInput); showEditNameDialog = false }) { Text("儲存") } }, dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("取消") } })
        }

        // 儲值 Dialog
        if (showTopUpDialog) {
            AlertDialog(
                onDismissRequest = { showTopUpDialog = false },
                title = { Text("儲值購物金", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                text = {
                    Column {
                        Text("請選擇儲值金額", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                        val amounts = listOf(100, 500, 1000, 5000)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            amounts.take(2).forEach { Button(onClick = { viewModel.addPoints(it); showTopUpDialog = false }, modifier = Modifier.weight(1f).padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("$$it") } }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            amounts.takeLast(2).forEach { Button(onClick = { viewModel.addPoints(it); showTopUpDialog = false }, modifier = Modifier.weight(1f).padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("$$it") } }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showTopUpDialog = false }) { Text("取消", color = Color.Gray) } }
            )
        }
    }
}

// MenuCard 保持不變
@Composable
fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    textColor: Color = Color.Black,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (textColor == Color.Red) Color.Red else Color(0xFF487F81),
                    modifier = Modifier.size(28.dp)
                )
                if (badgeCount > 0) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                if (subtitle != null) Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }

            if (badgeCount > 0) {
                Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.height(20.dp).widthIn(min = 20.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}