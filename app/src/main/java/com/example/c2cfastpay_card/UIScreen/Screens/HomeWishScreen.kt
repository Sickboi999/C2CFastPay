package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.R
import com.example.c2cfastpay_card.UIScreen.components.BottomNavigationBar
import com.example.c2cfastpay_card.UIScreen.components.WishRepository
import com.example.c2cfastpay_card.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishPreviewPage(
    navController: NavController
) {
    val context = LocalContext.current
    val wishRepository = remember { WishRepository(context) }

    var searchQuery by remember { mutableStateOf("") }

    // 使用 Flow 監聽資料
    val wishList by wishRepository.getWishListFlow(searchQuery = searchQuery)
        .collectAsState(initial = emptyList())

    val primaryColor = Color(0xFFFBC02D) // 許願牆主題色 (黃色)
    val accentColor = Color(0xFFFF9800)  // 橘色

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        // 使用 Box 堆疊佈局 (背景圖在下，內容在上)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(Color.White)
        ) {
            // --- 底層：背景圖 ---
            Image(
                painter = painterResource(R.drawable.background_of_wishing_page),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .align(Alignment.TopCenter)
            )

            // --- 中層：頁面內容 ---

            // SALE 按鈕
            IconButton(
                onClick = { navController.navigate(Screen.Sale.route) },
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopCenter)
                    .offset(x = (-62).dp, y = 100.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.sale_button02),
                    contentDescription = "SALE",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // WISH 按鈕
            IconButton(
                onClick = { /* 當前頁面 */ },
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 88.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.wish_button02),
                    contentDescription = "WISH",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // 搜尋列
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 150.dp)
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newText: String -> searchQuery = newText },
                    label = {
                        Text("搜尋願望...", style = TextStyle(fontSize = 14.sp))
                    },
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜尋", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // 許願列表 
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 220.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(wishList) { wish ->
                    //  仿照 SaleProductPage 的卡片設計
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // 點擊跳轉到 願望詳情頁 
                                navController.navigate("wish_detail/${wish.uuid}")
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column {
                            // 圖片區域
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(Color(0xFFFFF8E1)) // 淡黃色
                            ) {
                                if (wish.imageUri.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = wish.imageUri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 無圖片顯示圖示
                                    Icon(
                                        painter = painterResource(R.drawable.wish_button02),
                                        contentDescription = "No Image",
                                        tint = Color.LightGray,
                                        modifier = Modifier.align(Alignment.Center).size(48.dp)
                                    )
                                }
                            }

                            // 文字資訊區域
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // 標題
                                Text(
                                    text = wish.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // 許願者
                                Text(
                                    text = "許願者: ${wish.ownerName.ifBlank { "匿名" }}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                // 預算顯示
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "預算 ${wish.price}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 底部：快速上架按鈕
                                Button(
                                    onClick = {
                                        val wishUuid = wish.uuid
                                        navController.navigate("add_product?wishUuid=$wishUuid")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp), // 小一點的按鈕，配合卡片尺寸
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "快速上架",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 頂層：透明標題列與購物車 ---
            TopAppBar(
                title = { },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "購物車",
                            tint = Color(0xFFF79329)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
    }
}