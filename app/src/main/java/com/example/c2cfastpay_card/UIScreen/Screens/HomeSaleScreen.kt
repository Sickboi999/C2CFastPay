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
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleProductPage(
    navController: NavController
) {
    val context = LocalContext.current

    // 初始化 Repository
    val productRepository = remember { ProductRepository(context) }

    // 搜尋關鍵字狀態
    var searchQuery by remember { mutableStateOf("") }

    // 取得原始資料
    val rawProductList by productRepository.getAllProducts(searchQuery = searchQuery)
        .collectAsState(initial = emptyList())

    // 過濾邏輯：只顯示庫存 > 0 的商品
    val availableProductList = remember(rawProductList) {
        rawProductList.filter { product ->
            val stockInt = product.stock.ifBlank { "1" }.toIntOrNull() ?: 0
            stockInt > 0
        }
    }

    // 主題色 (深綠色)
    val primaryColor = Color(0xFF487F81)

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        // 使用 Box 來實現圖層堆疊效果 (背景圖在下，內容在上)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(Color.White)
        ) {
            // --- 底層：背景圖 ---
            Image(
                painter = painterResource(R.drawable.backgroud_of_selling_page),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .align(Alignment.TopCenter)
            )

            // --- 中層：按鈕與搜尋列 ---

            // SALE 按鈕
            IconButton(
                onClick = { /* 當前頁面 */ },
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 88.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.sale_button),
                    contentDescription = "SALE",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            // WISH 按鈕
            IconButton(
                onClick = { navController.navigate(Screen.WishList.route) },
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopCenter)
                    .offset(x = 62.dp, y = 100.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.wish_button),
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
                    onValueChange = { newText -> searchQuery = newText },
                    label = {
                        Text(
                            "搜尋商品...",
                            style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                        )
                    },
                    // 明確設定文字顏色為黑色，避免深色模式看不到
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Black
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜尋",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        // 確保文字輸入時也是黑色
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // 商品列表
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 220.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(availableProductList) { product ->
                    // 單一商品卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {    // 點擊跳轉到商品詳情頁
                                navController.navigate("product_detail/${product.id}")
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
                                    .background(Color(0xFFEEEEEE))
                            ) {
                                if (product.imageUri.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = product.imageUri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 無圖片時顯示預設 Icon
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "No Image",
                                        tint = Color.Gray,
                                        modifier = Modifier.align(Alignment.Center)
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
                                    text = product.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // 賣家資訊
                                Text(
                                    text = "賣家: ${product.ownerName.ifBlank { "匿名" }}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                // 狀態標籤
                                Text(
                                    text = "#${product.condition.ifBlank { "二手" }}",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 底部欄：價格 + 庫存標籤
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 價格
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = product.price,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }

                                    // 右下角庫存標籤
                                    Surface(
                                        color = primaryColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        val stockCount = product.stock.ifBlank { "1" }
                                        Text(
                                            text = "剩 $stockCount 件",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 頂層：透明標題列與購物車 ---
            TopAppBar(
                title = { },    // 不顯示標題，因為背景已有圖片
                navigationIcon = {},
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "購物車",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent  // 透明背景
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
    }
}