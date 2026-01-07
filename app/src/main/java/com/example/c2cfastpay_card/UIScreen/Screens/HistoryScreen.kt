package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.c2cfastpay_card.UIScreen.components.BottomNavigationBar
import com.example.c2cfastpay_card.UIScreen.components.MatchItem
import com.example.c2cfastpay_card.UIScreen.components.MatchRepository
import com.example.c2cfastpay_card.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {

    val context = LocalContext.current
    val matchRepository = remember { MatchRepository(context) }
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(matchRepository))
    val matches by viewModel.matches.collectAsState()

    // Tab 狀態
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("以物易物協商室", "商品聊聊")
    val primaryColor = Color(0xFF487F81)

    // 分類顯示邏輯
    val displayMatches = if (selectedTab == 0) {
        // Tab 0: 協商室 (只顯示 SWAP)
        matches.filter { it.type == "SWAP" }
    } else {
        // Tab 1: 聊聊 (只顯示 NORMAL)
        matches.filter { it.type == "NORMAL" }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("聊天列表", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        // 底部導航列，讓使用者可以切換回首頁或其他頁面
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // 切換分頁 (Tabs)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = primaryColor)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        selectedContentColor = primaryColor,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            // 列表內容
            if (displayMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if(selectedTab == 0) "尚無協商紀錄" else "尚無聊聊紀錄", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayMatches, key = { it.id }) { matchItem ->
                        MatchHistoryItem(
                            item = matchItem,
                            onClick = { navController.navigate(Screen.Chat.createRoute(matchItem.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchHistoryItem(item: MatchItem, onClick: () -> Unit) {
    val isNormalChat = item.type == "NORMAL"

    // 標題
    val titleText = if (isNormalChat) {
        // 一般聊聊：顯示對方名字
        item.otherUserName
    } else {
        // 交換協商：顯示「對方名字 的 商品名稱」
        if (item.myLikedItems.isNotEmpty()) {
            "${item.otherUserName} 的 ${item.myLikedItems.joinToString("、")}"
        } else {
            "${item.otherUserName} 的商品"
        }
    }

    // 頭像邏輯 
    val imageUrl = if (isNormalChat) {
        // 如果是聊聊，優先使用抓到的 otherUserAvatar，沒有才用 ui-avatars
        if (item.otherUserAvatar.isNotBlank()) item.otherUserAvatar
        else "https://ui-avatars.com/api/?name=${item.otherUserName}&background=random"
    } else {
        // 如果是協商，顯示商品圖
        item.productImageUrl
    }

    val imageShape = if (isNormalChat) CircleShape else RoundedCornerShape(8.dp)
    val subText = if (isNormalChat) "點擊查看訊息..." else "點擊進入協商..."

    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = Uri.parse(imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(imageShape)
                    .background(Color(0xFFEEEEEE))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = Color.Black,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subText, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}