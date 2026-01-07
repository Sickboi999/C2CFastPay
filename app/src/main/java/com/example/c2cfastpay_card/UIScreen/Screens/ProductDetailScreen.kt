package com.example.c2cfastpay_card.UIScreen.Screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.CartRepository
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.data.CartItem
import com.example.c2cfastpay_card.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 商品專用配色 
val MintGreenAccent = Color(0xFFE0F2F1)
val MintGreenDark = Color(0xFF487F81)
val TextBlack = Color(0xFF191C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String
) {
    val context = LocalContext.current
    val repository = remember { ProductRepository(context) }
    val cartRepository = remember { CartRepository(context) }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // 商品資料狀態
    var product by remember { mutableStateOf<ProductItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 變數宣告 
    var selectedImageUri by remember { mutableStateOf("") }

    // 賣家真實頭像狀態
    var sellerAvatarUrl by remember { mutableStateOf("") }

    // 菜單與對話框控制
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val db = remember { FirebaseFirestore.getInstance() }

    // 載入商品資料與賣家資訊
    LaunchedEffect(productId) {
        product = repository.getProductById(productId)

        product?.let { item ->
            // 設定預設選中圖片
            selectedImageUri = if (item.images.isNotEmpty()) item.images[0] else item.imageUri

            // 抓取賣家真實頭像
            if (item.ownerId.isNotBlank()) {
                try {
                    val userDoc = db.collection("users").document(item.ownerId).get().await()
                    if (userDoc.exists()) {
                        sellerAvatarUrl = userDoc.getString("avatarUrl")
                            ?: userDoc.getString("photoUrl")
                                    ?: ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("商品詳情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 判斷是否為賣家本人
                    if (product != null) {
                        if (product!!.ownerId == currentUserId) {
                            // 是本人 -> 顯示「編輯/刪除」選單
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = Color.White
                            ) {
                                DropdownMenuItem(
                                    text = { Text("編輯商品") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.EditProduct.createRoute(productId))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("刪除商品", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        } else {
                            // 不是本人 -> 顯示「購物車」按鈕
                            IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "購物車", tint = MintGreenDark)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // 底部操作列 (加入購物車)
            if (product != null) {
                val isOwner = product!!.ownerId == currentUserId
                BottomAppBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (isOwner) {
                        // 本人不能買自己的商品
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.White),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                            enabled = false
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("這是您的商品", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // 加入購物車按鈕
                        Button(
                            onClick = {
                                scope.launch {
                                    val cartItem = CartItem(
                                        productId = product!!.id,
                                        productTitle = product!!.title,
                                        productPrice = product!!.price,
                                        productImage = product!!.imageUri,
                                        quantity = 1,
                                        stock = try { product!!.stock.toInt() } catch (e:Exception) { 1 },
                                        sellerId = product!!.ownerId
                                    )
                                    val success = cartRepository.addToCart(cartItem)
                                    if (success) Toast.makeText(context, "已加入購物車", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "加入失敗", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreenDark),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("加入購物車", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MintGreenDark)
            }
        } else if (product != null) {
            val item = product!!

            // 整理圖片列表
            val displayImages = remember(item) {
                val list = mutableListOf<String>()
                if (item.images.isNotEmpty()) list.addAll(item.images)
                else if (item.imageUri.isNotEmpty()) list.add(item.imageUri)
                list.distinct()
            }
            val displayStock = if (item.stock.isBlank()) "1" else item.stock
            val displayCondition = if (item.condition.isBlank()) "全新" else item.condition
            val displayLogistics = item.payment.ifBlank { "7-11、全家、面交" }
            val displayDescription = if (item.description.isBlank()) "賣家沒有留下文案。" else item.description

            val finalAvatar = if (sellerAvatarUrl.isNotBlank()) sellerAvatarUrl
            else "https://ui-avatars.com/api/?name=${item.ownerName}&background=random"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圖片顯示區塊
                if (displayImages.isNotEmpty()) {
                    // 大圖顯示區域
                    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1.3f), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        // ★★★ 使用 selectedImageUri ★★★
                        Image(
                            painter = rememberAsyncImagePainter(
                                if(selectedImageUri.isNotEmpty()) selectedImageUri else displayImages[0]
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 如果有多張圖，顯示縮圖列表
                    if (displayImages.size > 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(displayImages) { imgUri ->
                                val isSelected = imgUri == selectedImageUri
                                Image(
                                    painter = rememberAsyncImagePainter(imgUri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MintGreenDark else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedImageUri = imgUri }, // 點擊切換大圖
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Text("無圖片", color = Color.Gray) }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 標題
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(200.dp).height(16.dp).offset(y = 10.dp).background(MintGreenAccent, RoundedCornerShape(50)))
                    Text(text = item.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextBlack, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 詳細資訊
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        ProductSectionTitle("物流方式")
                        Text(text = displayLogistics, fontSize = 14.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(24.dp))
                        ProductSectionTitle("商品價格")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, null, tint = TextBlack, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = item.price, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ProductSectionTitle("商品規格")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ProductSpecText("庫存", displayStock)
                            ProductSpecText("狀態", displayCondition)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(32.dp))

                // 賣家資訊
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProductSectionTitle("賣家資訊")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // 點擊查看賣家所有商品
                                if (item.ownerId.isNotBlank()) navController.navigate("user_product/${item.ownerId}")
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(finalAvatar),
                            contentDescription = "賣家頭像",
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEEEEEE)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.ownerName.ifEmpty { "匿名賣家" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(item.ownerEmail, fontSize = 12.sp, color = Color.Gray)
                        }

                        // 如果不是本人，顯示私訊按鈕
                        if (currentUserId.isNotBlank() && item.ownerId != currentUserId) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        // 建立或取得聊天室 ID
                                        val matchId = createOrGetChat(db, currentUserId, item)
                                        if (matchId != null) {
                                            navController.navigate(Screen.Chat.createRoute(matchId))
                                        } else {
                                            Toast.makeText(context, "無法建立聊天室", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "私訊", tint = MintGreenDark)
                            }
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Go", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 文案與故事
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProductSectionTitle("商品文案")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = displayDescription, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth())
                }
                // 如果有 AI 生成的故事，才顯示這區塊
                if (item.story.isNotBlank()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ProductSectionTitle("商品故事")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = item.story, fontSize = 13.sp, color = Color(0xFF666666), lineHeight = 18.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("找不到商品") }
        }

        // 確認刪除對話框
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("確認刪除") },
                text = { Text("真的要刪除此商品嗎？刪除後無法復原。") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            try {
                                repository.deleteProduct(productId)
                                Toast.makeText(context, "商品已刪除", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "刪除失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("確認刪除", color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("取消", color = Color.Gray) }
                },
                containerColor = Color.White
            )
        }
    }
}

// 建立聊天室函式
suspend fun createOrGetChat(
    db: FirebaseFirestore,
    currentUserId: String,
    product: ProductItem
): String? {
    return try {
        val matchesRef = db.collection("matches")
        // 查詢我參與的聊天室
        val query = matchesRef
            .whereEqualTo("type", "NORMAL")
            .whereArrayContains("users", currentUserId)
            .get()
            .await()

        // 進一步過濾：對方必須是 product.ownerId
        val existingMatch = query.documents.find { doc ->
            val users = doc.get("users") as? List<String> ?: emptyList()
            users.contains(product.ownerId)
        }

        if (existingMatch != null) {
            return existingMatch.id     // 已存在，直接回傳 ID
        }

        // 不存在，建立新的
        val newMatchRef = matchesRef.document()
        val newMatchData = hashMapOf(
            "users" to listOf(currentUserId, product.ownerId),
            "type" to "NORMAL",
            "productId" to product.id,
            "productImage" to (if(product.images.isNotEmpty()) product.images[0] else product.imageUri),
            "updatedAt" to System.currentTimeMillis(),
            "lastMessage" to "",
            "otherUserName" to product.ownerName    // 暫存對方名稱
        )
        newMatchRef.set(newMatchData).await()
        return newMatchRef.id

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ProductSectionTitle(text: String) {
    Box(modifier = Modifier.padding(bottom = 8.dp), contentAlignment = Alignment.CenterStart) {
        Box(modifier = Modifier.width(60.dp).height(10.dp).offset(y = 6.dp).background(MintGreenAccent, RoundedCornerShape(4.dp)))
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextBlack)
    }
}

@Composable
fun ProductSpecText(label: String, value: String) {
    Row {
        Text(text = "$label : ", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, color = Color.DarkGray)
    }
}