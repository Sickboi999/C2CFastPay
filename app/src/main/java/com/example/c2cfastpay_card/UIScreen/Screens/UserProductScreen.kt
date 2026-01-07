package com.example.c2cfastpay_card.UIScreen.Screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.WishItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// User 資料模型 
data class UserProfile(
    val id: String = "",
    val nickname: String = "匿名使用者",
    val avatarUrl: String = "",
    val bio: String = "這個人很懶，什麼都沒寫",
    val rating: Float = 5.0f,
    val reviewCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProductScreen(
    navController: NavController,
    userId: String  // 要查看的賣家 ID
) {
    val context = LocalContext.current

    // 狀態管理 
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    // 分開存儲：上架商品 與 願望清單
    var sellingProducts by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var wishProducts by remember { mutableStateOf<List<ProductItem>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    // 撈取資料 
    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect

        isLoading = true
        val db = FirebaseFirestore.getInstance()

        try {
            // 抓取使用者資料
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                userProfile = UserProfile(
                    id = userId,
                    nickname = userDoc.getString("name") ?: userDoc.getString("displayName") ?: "匿名賣家",
                    avatarUrl = userDoc.getString("avatarUrl") ?: userDoc.getString("photoUrl") ?: "",
                    bio = userDoc.getString("bio") ?: "歡迎來到我的賣場！"
                )
            } else {
                userProfile = UserProfile(id = userId, nickname = "未知的使用者")
            }

            // 抓取「上架商品」 (products 集合)
            val productsQuery = db.collection("products")
                .whereEqualTo("ownerId", userId)
                .get()
                .await()

            sellingProducts = productsQuery.documents.mapNotNull { doc ->
                try {
                    // 只顯示庫存 > 0 的
                    val stockStr = doc.getString("stock") ?: "0"
                    if ((stockStr.toIntOrNull() ?: 0) <= 0) return@mapNotNull null

                    // 轉換為 ProductItem
                    ProductItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        price = doc.getString("price") ?: "0",
                        description = doc.getString("description") ?: "",
                        imageUri = doc.getString("imageUri") ?: "",
                        images = (doc.get("images") as? List<String>) ?: emptyList(),
                        ownerId = doc.getString("ownerId") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        ownerEmail = doc.getString("ownerEmail") ?: "",
                        stock = stockStr,
                        payment = doc.getString("payment") ?: "",
                        condition = doc.getString("condition") ?: ""
                    )
                } catch (e: Exception) { null }
            }

            // 抓取「願望清單」 (wishes 集合)
            // 注意：WishItem 的 ownerId 在資料庫欄位是 "userId"
            val wishesQuery = db.collection("wishes")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            wishProducts = wishesQuery.documents.mapNotNull { doc ->
                try {
                    val wish = doc.toObject(WishItem::class.java) ?: return@mapNotNull null

                    // 將 WishItem 轉換成 ProductItem 以便放入 Grid 顯示
                    ProductItem(
                        id = wish.uuid, // 使用 WishItem 的 uuid 作為 id
                        title = wish.title,
                        price = wish.price,
                        description = wish.description,
                        imageUri = wish.imageUri,
                        images = wish.images,
                        ownerId = wish.ownerId,
                        ownerName = wish.ownerName,
                        ownerEmail = wish.ownerEmail,
                        stock = wish.qty,
                        condition = wish.condition,
                        payment = wish.payment
                    )
                } catch (e: Exception) { null }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "載入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 分頁邏輯 
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("上架商品", "願望清單")

    // 根據 Tab 決定要顯示哪個列表
    val displayList = if (selectedTabIndex == 0) sellingProducts else wishProducts

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("賣家主頁") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 檢舉或分享 */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF759E9F))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF9F9F9))
            ) {
                // 使用者資訊
                userProfile?.let { UserProfileHeader(user = it) }

                Spacer(modifier = Modifier.height(8.dp))

                // 分頁標籤
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF759E9F),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF759E9F)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
                            }
                        )
                    }
                }

                // 商品網格
                if (displayList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTabIndex == 0) "目前沒有上架商品" else "目前沒有願望清單",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayList) { item ->
                            ProductGridItem(
                                product = item,
                                isWish = (selectedTabIndex == 1),
                                onClick = {
                                    if (selectedTabIndex == 0) {
                                        // 上架商品 -> 商品詳情
                                        navController.navigate("product_detail/${item.id}")
                                    } else {
                                        // 願望清單 -> 願望詳情 (確認您的 Route 名稱是 wish_detail/{wishId})
                                        navController.navigate("wish_detail/${item.id}")
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

// Composable: 使用者資訊頭部 
@Composable
fun UserProfileHeader(user: UserProfile) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=${user.nickname}&background=random" },
            contentDescription = "Avatar",
            modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, Color(0xFFEEEEEE), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
            Text(text = "${user.rating} (${user.reviewCount} 則評價)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
        }
        if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = user.bio, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Composable: 商品卡片 
@Composable
fun ProductGridItem(
    product: ProductItem,
    isWish: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 圖片區塊
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color.LightGray)) {
                if (product.imageUri.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star, // 預設圖
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 顯示紅色標籤
                if (isWish) {
                    Surface(
                        color = Color(0xCCFF6F61),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "徵求中",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ${product.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}