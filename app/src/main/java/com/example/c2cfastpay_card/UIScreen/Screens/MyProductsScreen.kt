package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.navigation.Screen
import com.example.c2cfastpay_card.ui.theme.SaleColorScheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { ProductRepository(context) }
    val scope = rememberCoroutineScope()

    // 商品列表資料狀態
    var myProducts by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 篩選器狀態：0=全部, 1=上架中(庫存>0), 2=已下架(庫存=0)
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("全部商品", "上架中", "已下架")
    val primaryColor = Color(0xFF487F81)

    // 載入資料函式
    fun loadData() {
        scope.launch {
            isLoading = true
            try {
                // 這裡讀取所有商品，篩選在本地做
                val allProducts = repository.getMyProducts()
                // 預設依照時間排序 (最新的在上面)
                myProducts = allProducts.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                android.util.Log.e("MyProductsScreen", "讀取失敗", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // 根據 Tab 計算要顯示的列表
    val displayProducts = remember(myProducts, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> myProducts.filter { (it.stock.toIntOrNull() ?: 0) > 0 }

            // 2. 同上
            2 -> myProducts.filter { (it.stock.toIntOrNull() ?: 0) <= 0 }
            else -> myProducts
        }
    }

    MaterialTheme(colorScheme = SaleColorScheme) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("我的商品管理", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                // --- 篩選 Tabs ---
                TabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = Color.White,
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedFilterIndex]),
                            color = primaryColor
                        )
                    }
                ) {
                    filterTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if(selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = primaryColor,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                // 商品列表內容
                if (isLoading) {
                    // 載入中
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (displayProducts.isEmpty()) {
                    // 空狀態
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("此分類暫無商品", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayProducts, key = { it.id }) { product ->
                            MyProductItemRow(
                                product = product,
                                onClick = {
                                    // 點擊跳轉到 DetailScreen
                                    navController.navigate(Screen.ProductDetail.createRoute(product.id))
                                },
                                onDelete = {
                                    // 刪除商品
                                    scope.launch {
                                        repository.deleteProduct(product.id)
                                        loadData()  // 刪除後重新載入列表
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

//商品列表
@Composable
fun MyProductItemRow(
    product: ProductItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // 讓整張卡片可點擊
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圖片
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                if (product.imageUri.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = product.imageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 內容
            Column(modifier = Modifier.weight(1f)) {
                // 標題與狀態標籤
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 如果庫存 <= 0，顯示「已下架」標籤
                    if ((product.stock.toIntOrNull() ?: 0) <= 0) {                        Surface(
                            color = Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text("已下架", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = product.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "NT$ ${product.price}",
                    fontSize = 15.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "庫存: ${product.stock} | 上架: ${SimpleDateFormat("MM/dd", Locale.getDefault()).format(product.timestamp)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 刪除按鈕
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}