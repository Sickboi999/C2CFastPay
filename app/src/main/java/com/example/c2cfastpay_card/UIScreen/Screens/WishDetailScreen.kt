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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.c2cfastpay_card.UIScreen.components.WishRepository
import com.example.c2cfastpay_card.UIScreen.components.WishItem
import com.example.c2cfastpay_card.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 主題色
val WishYellowAccent = Color(0xFFFFF176)
val WishOrangeDark = Color(0xFFFF9800)
val WishTextBlack = Color(0xFF191C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishDetailScreen(
    navController: NavController,
    wishId: String
) {
    val context = LocalContext.current
    val wishRepository = remember { WishRepository(context) }
    val scope = rememberCoroutineScope()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // 狀態變數
    var wishItem by remember { mutableStateOf<WishItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 許願者頭像
    var wisherAvatarUrl by remember { mutableStateOf("") }

    // 對話框控制
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val db = remember { FirebaseFirestore.getInstance() }

    // 載入資料
    LaunchedEffect(wishId) {
        // 取得願望詳情
        wishItem = wishRepository.getWishById(wishId)

        wishItem?.let { item ->
            // 抓頭像
            if (item.ownerId.isNotBlank()) {
                try {
                    val userDoc = db.collection("users").document(item.ownerId).get().await()
                    if (userDoc.exists()) {
                        wisherAvatarUrl = userDoc.getString("avatarUrl")
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
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("願望詳情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 如果是本人，顯示編輯/刪除選單
                    if (wishItem != null && wishItem!!.ownerId == currentUserId) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color.White
                        ) {
                            DropdownMenuItem(
                                text = { Text("編輯願望") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.EditWish.createRoute(wishId))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("刪除願望", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    } else {
                        // 非本人：顯示購物車按鈕(去商品聊聊列表) 
                        IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "聊聊", tint = WishOrangeDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // 底部操作列 (快速上架按鈕)
            if (wishItem != null) {
                val isOwner = wishItem!!.ownerId == currentUserId

                BottomAppBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (!isOwner) {
                                // 外人點擊 -> 快速上架 (帶入願望 ID)
                                // 這會導向 AddProductScreen，並自動填入此願望的資訊
                                navController.navigate("add_product?wishUuid=${wishItem!!.uuid}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOwner) Color.LightGray else WishOrangeDark
                        ),
                        enabled = !isOwner, // 本人不能點擊
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(50.dp)
                    ) {
                        if (isOwner) {
                            Text("這是您的願望", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text("我有這個商品！快速上架", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WishOrangeDark)
            }
        } else {
            val item = wishItem
            if (item != null) {
                // 資料準備
                val displayDesc = item.description.ifBlank { "沒有詳細描述。" }
                val displayLogistics = item.payment.ifBlank { "皆可" }
                val displayCondition = item.condition.ifBlank { "不拘" }
                val displayQty = item.qty.ifBlank { "1" }
                val displayNote = item.memo.ifBlank { "無備註" }

                // 決定頭像：優先用 wisherAvatarUrl，否則用 ui-avatars
                val finalAvatar = if (wisherAvatarUrl.isNotBlank()) wisherAvatarUrl
                else "https://ui-avatars.com/api/?name=${item.ownerName}&background=random"

                // 處理圖片
                val allImages = remember(item) {
                    (listOf(item.imageUri) + item.images).filter { it.isNotEmpty() }.distinct()
                }
                var selectedImage by remember { mutableStateOf(allImages.firstOrNull() ?: "") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // 圖片
                    if (allImages.isNotEmpty()) {
                        // 大圖
                        Card(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.3f),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = selectedImage.ifEmpty { allImages.first() }),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // 縮圖列表
                        if (allImages.size > 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allImages) { img ->
                                    val isSelected = img == selectedImage
                                    Image(
                                        painter = rememberAsyncImagePainter(model = img),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(if(isSelected) 2.dp else 0.dp, if(isSelected) WishOrangeDark else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedImage = img },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    } else {
                        // 無圖
                        Box(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).background(Color(0xFFFFF8E1), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("無參考圖片", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 標題
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(200.dp).height(16.dp).offset(y = 10.dp).background(WishYellowAccent, RoundedCornerShape(50)))
                        Text(text = item.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WishTextBlack, textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 詳細資訊
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            WishSectionTitle("預算")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = item.price, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WishTextBlack)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            WishSectionTitle("物流方式")
                            Text(text = displayLogistics, fontSize = 14.sp, color = Color.DarkGray)
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            WishSectionTitle("需求規格")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                WishSpecText(label = "欲購數量", value = displayQty)
                                WishSpecText(label = "接受狀態", value = displayCondition)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(32.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        WishSectionTitle("許願者")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // 點擊跳轉到該使用者的個人頁面
                                    if (item.ownerId.isNotBlank()) {
                                        navController.navigate("user_product/${item.ownerId}")
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            // 顯示頭像
                            Image(
                                painter = rememberAsyncImagePainter(finalAvatar),
                                contentDescription = "許願者頭像",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEEEEE)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            // 顯示名字/Email
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.ownerName.ifEmpty { "匿名許願者" }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(item.ownerEmail, fontSize = 12.sp, color = Color.Gray)
                            }

                            // 私訊按鈕 (非本人才顯示)
                            if (currentUserId.isNotBlank() && item.ownerId != currentUserId) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            // 建立/跳轉聊天室 (傳入 WishItem 轉為 ProductItem 格式的資訊)
                                            val matchId = createOrGetChatFromWish(db, currentUserId, item)
                                            if (matchId != null) {
                                                navController.navigate(Screen.Chat.createRoute(matchId))
                                            } else {
                                                Toast.makeText(context, "無法建立聊天室", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = "私訊",
                                        tint = WishOrangeDark
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Go",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 描述
                    Column(modifier = Modifier.fillMaxWidth()) {
                        WishSectionTitle("願望描述")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = displayDesc, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 備註
                    if (displayNote.isNotBlank() && displayNote != "無備註") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            WishSectionTitle("備註")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = displayNote, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("找不到該願望") }
            }
        }

        // 刪除確認對話框
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("確認刪除") },
                text = { Text("真的要刪除此願望嗎？刪除後無法復原。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                try {
                                    wishRepository.deleteWish(wishId)
                                    Toast.makeText(context, "願望已刪除", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "刪除失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("確認刪除", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消", color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

// 從願望建立聊天室
suspend fun createOrGetChatFromWish(
    db: FirebaseFirestore,
    currentUserId: String,
    wish: WishItem
): String? {
    return try {
        val matchesRef = db.collection("matches")
        val query = matchesRef
            .whereEqualTo("type", "NORMAL")
            .whereArrayContains("users", currentUserId)
            .get()
            .await()

        val existingMatch = query.documents.find { doc ->
            val users = doc.get("users") as? List<String> ?: emptyList()
            users.contains(wish.ownerId)
        }

        if (existingMatch != null) {
            return existingMatch.id
        }

        val newMatchRef = matchesRef.document()
        val newMatchData = hashMapOf(
            "users" to listOf(currentUserId, wish.ownerId),
            "type" to "NORMAL",
            "productId" to wish.uuid, // 使用願望 ID 作為 productId
            "productImage" to (if(wish.imageUri.isNotEmpty()) wish.imageUri else ""),
            "updatedAt" to System.currentTimeMillis(),
            "lastMessage" to "我對你的願望有興趣！",
            "otherUserName" to wish.ownerName
        )
        newMatchRef.set(newMatchData).await()
        return newMatchRef.id

    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun WishSectionTitle(text: String) {
    Box(modifier = Modifier.padding(bottom = 8.dp), contentAlignment = Alignment.CenterStart) {
        Box(modifier = Modifier.width(60.dp).height(10.dp).offset(y = 6.dp).background(WishYellowAccent, RoundedCornerShape(4.dp)))
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = WishTextBlack)
    }
}

@Composable
fun WishSpecText(label: String, value: String) {
    Row {
        Text(text = "$label : ", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, color = Color.DarkGray)
    }
}