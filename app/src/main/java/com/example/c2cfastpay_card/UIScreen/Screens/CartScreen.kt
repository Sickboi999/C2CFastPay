package com.example.c2cfastpay_card.UIScreen.Screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.data.CartItem
import com.example.c2cfastpay_card.UIScreen.components.CartRepository
import com.example.c2cfastpay_card.ui.theme.SaleColorScheme
import com.example.c2cfastpay_card.navigation.Screen

// 定義主題色 (深綠色) & 警告色
val PrimaryGreen = Color(0xFF487F81)
val AlertRed = Color(0xFFB00020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController) {
    val context = LocalContext.current
    // 初始化 Repository 與 ViewModel
    // 使用 remember 確保 Repository 不會因為重組而重複建立
    val cartRepository = remember { CartRepository(context) }
    val viewModel: CartViewModel = viewModel(
        factory = CartViewModelFactory(cartRepository)
    )

    // 觀察 ViewModel 的狀態
    val cartItems by viewModel.cartItems.collectAsState()
    val totalPrice by viewModel.totalPrice.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 狀態管理 
    // 1. 是否處於「編輯模式」(多選刪除)
    var isEditMode by remember { mutableStateOf(false) }

    // 2. 單一商品刪除警告 (儲存待刪除的商品)
    var itemToDelete by remember { mutableStateOf<CartItem?>(null) }

    // 3. 批量刪除警告
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // 計算已選數量
    val selectedCount = cartItems.count { it.isChecked }

    // 監聽結帳或操作結果 Toast
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme(colorScheme = SaleColorScheme) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("購物車 (${cartItems.size})", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 編輯購物車
                        if (cartItems.isNotEmpty()) {
                            TextButton(onClick = { isEditMode = !isEditMode }) {
                                Text(
                                    text = if (isEditMode) "完成" else "編輯",
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            bottomBar = {
                // 只有購物車有東西時才顯示底部欄
                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 16.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditMode) {
                                // 編輯模式：顯示刪除按鈕 
                                Spacer(modifier = Modifier.weight(1f)) // 推到右邊
                                Button(
                                    onClick = {
                                        // 刪除警告
                                        if (selectedCount > 0) showBatchDeleteDialog = true
                                    },
                                    enabled = selectedCount > 0 && !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AlertRed,
                                        disabledContainerColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("刪除已選 ($selectedCount)", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                // 一般模式：顯示總金額與結帳
                                Column {
                                    Text(
                                        text = "總金額",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "NT$ $totalPrice",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                // 結帳按鈕
                                Button(
                                    onClick = { viewModel.checkout() },
                                    enabled = selectedCount > 0 && !isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryGreen,
                                        disabledContainerColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "去結帳 ($selectedCount)",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                // 判斷購物車是否為空
                if (cartItems.isEmpty()) {
                    EmptyCartView(navController)
                } else {
                    // 購物車列表 
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFAFAFA)),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cartItems, key = { it.id }) { item ->
                            CartItemRow(
                                item = item,
                                isEditMode = isEditMode,
                                onToggleCheck = { viewModel.toggleItemChecked(item) },
                                onIncrease = { viewModel.increaseQuantity(item) },
                                onDecrease = { viewModel.decreaseQuantity(item) },
                                onDeleteClick = {
                                    // ★★★ 需求 2: 單刪除也要警告 ★★★
                                    itemToDelete = item
                                }
                            )
                        }
                    }
                }

                // Loading 遮罩 
                if (isLoading) {
                    LoadingOverlay()
                }

                // 單一刪除確認 
                if (itemToDelete != null) {
                    AlertDialog(
                        onDismissRequest = { itemToDelete = null },
                        title = { Text("確認刪除") },
                        text = { Text("確定要將「${itemToDelete?.productTitle}」移除嗎？") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    itemToDelete?.let { viewModel.removeItem(it.id) }
                                    itemToDelete = null
                                }
                            ) { Text("刪除", color = AlertRed) }
                        },
                        dismissButton = {
                            TextButton(onClick = { itemToDelete = null }) { Text("取消") }
                        },
                        containerColor = Color.White
                    )
                }

                // 批量刪除確認 
                if (showBatchDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatchDeleteDialog = false },
                        title = { Text("確認批量刪除") },
                        text = { Text("確定要刪除選取的 $selectedCount 項商品嗎？") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val idsToDelete = cartItems.filter { it.isChecked }.map { it.id }
                                    viewModel.removeCartItems(idsToDelete)
                                    showBatchDeleteDialog = false
                                    isEditMode = false // 刪除後退出編輯模式
                                }
                            ) { Text("全部刪除", color = AlertRed) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatchDeleteDialog = false }) { Text("取消") }
                        },
                        containerColor = Color.White
                    )
                }
            }
        }
    }
}

// 抽離的空購物車畫面 
@Composable
fun EmptyCartView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFE0E0E0)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("購物車是空的", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("趕快去挑選喜歡的商品吧！", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                navController.navigate(Screen.Sale.route) {
                    popUpTo(Screen.Sale.route) { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(50)
        ) {
            Text("去逛逛", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)        }
    }
}

// Loading 遮罩 
@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

// 購物車單一商品列 
@Composable
fun CartItemRow(
    item: CartItem,
    isEditMode: Boolean,
    onToggleCheck: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 勾選框
            Box(
                modifier = Modifier
                    .height(80.dp)
                    .wrapContentWidth(),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggleCheck() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (isEditMode) AlertRed else PrimaryGreen, // 編輯模式變紅色
                        uncheckedColor = Color.LightGray
                    )
                )
            }

            // 商品圖片
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                if (item.productImage.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = item.productImage),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Img", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 商品資訊
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 標題與刪除鈕 (一般模式才顯示單刪按鈕，編輯模式隱藏以免混淆)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.productTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF333333),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )

                    if (!isEditMode) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "刪除",
                            tint = Color.LightGray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onDeleteClick() }
                        )
                    }
                }

                // 價格與數量控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NT$ ${item.productPrice}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // 編輯模式下，禁止修改數量 
                    if (!isEditMode) {
                        QuantitySelector(
                            quantity = item.quantity,
                            onIncrease = onIncrease,
                            onDecrease = onDecrease,
                            isMaxReached = item.quantity >= item.stock
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    isMaxReached: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .height(28.dp)
    ) {
        // 減少按鈕
        IconButton(
            onClick = onDecrease,
            modifier = Modifier.size(28.dp),
            enabled = quantity > 1  // 數量 > 1 才能減少
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "減少",
                modifier = Modifier.size(14.dp),
                tint = if (quantity > 1) Color.Gray else Color.LightGray
            )
        }

        // 數量顯示
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
                .border(width = 1.dp, color = Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quantity.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 增加按鈕
        IconButton(
            onClick = onIncrease,
            modifier = Modifier.size(28.dp),
            enabled = !isMaxReached
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "增加",
                modifier = Modifier.size(14.dp),
                tint = if (!isMaxReached) PrimaryGreen else Color.LightGray
            )
        }
    }
}