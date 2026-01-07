package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.c2cfastpay_card.UIScreen.components.BottomNavigationBar
import com.example.c2cfastpay_card.navigation.Screen

// --- 定義頁面專屬色系 ---
val SelectionBackground = Color(0xFFF4F7F7) // 淺灰背景，凸顯卡片
val SaleColor = Color(0xFF487F81)           // 上架主題色 (藍綠)
val WishColor = Color(0xFFFF9800)           // 許願主題色 (橘)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishOrProductScreen(navController: NavController) {
    Scaffold(
        containerColor = SelectionBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { }, // 留空，讓視覺集中在中間的內容
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SelectionBackground
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }


    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 頁面大標題
            Text(
                text = "您想要做什麼？",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // 選項 1：我要上架 (藍綠色卡片)
            SelectionCard(
                title = "我要上架",
                subtitle = "拍張照，輕鬆販售您的商品",
                icon = Icons.Default.Storefront,
                color = SaleColor,
                onClick = {
                    // 跳轉到上架流程第一步 (拍照/選圖)
                    navController.navigate(Screen.AddStep1.route)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 選項 2：我要許願 (橘色卡片)
            SelectionCard(
                title = "我要許願",
                subtitle = "找不到心儀商品？直接許願徵求",
                icon = Icons.Default.AutoAwesome,
                color = WishColor,
                onClick = {
                    // 跳轉到許願表單頁面
                    navController.navigate(Screen.AddWish.route)
                }
            )
        }
    }
}

// --- 抽取出來的美化卡片元件 ---
@Composable
fun SelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))    // 增加陰影讓卡片浮起來
            .clickable(onClick = onClick),              // 讓整張卡片可點擊
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左側：文字區
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }

            // 右側：圓形圖示背景
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)), // 淡色背景
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
