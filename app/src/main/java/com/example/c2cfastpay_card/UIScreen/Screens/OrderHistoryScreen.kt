package com.example.c2cfastpay_card.UIScreen.Screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.c2cfastpay_card.data.Order
import com.example.c2cfastpay_card.data.SwapOrder 
import com.example.c2cfastpay_card.UIScreen.components.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// ViewModel (負責資料載入與狀態更新)
class OrderViewModel : ViewModel() {
    private val repository = OrderRepository()

    // 三種列表狀態
    var myPurchases by mutableStateOf<List<Order>>(emptyList())      // 我買的
    var mySales by mutableStateOf<List<Order>>(emptyList())          // 我賣的
    var mySwapOrders by mutableStateOf<List<SwapOrder>>(emptyList()) // 交換訂單
    var isLoading by mutableStateOf(false)

    init {
        loadData()
    }

    // 載入所有訂單資料
    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                myPurchases = repository.getMyPurchases()
                mySales = repository.getMySales()
                mySwapOrders = repository.getMySwapOrders() // 載入交換訂單
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // 賣家出貨 (一般訂單)
    fun shipOrder(orderId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try { repository.shipOrder(orderId); loadData(); onSuccess() }
            catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }

    // 買家收貨/完成 (一般訂單)
    fun completeOrder(orderId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try { repository.completeOrder(orderId); loadData(); onSuccess() }
            catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }

    // 交換訂單的操作
    fun updateSwapStatus(orderId: String, action: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try { repository.updateSwapStatus(orderId, action); loadData(); onSuccess() }
            catch (e: Exception) { e.printStackTrace() }
            finally { isLoading = false }
        }
    }
}

// Screen UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(navController: NavController) {
    val viewModel: OrderViewModel = viewModel()
    // 0=購買, 1=銷售, 2=交換
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("購買紀錄", "銷售紀錄", "交換紀錄")
    val primaryColor = Color(0xFF487F81)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("訂單管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = primaryColor
                    )
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
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 根據 Tab 顯示不同內容
                    when (selectedTab) {
                        0 -> items(viewModel.myPurchases) { order ->    
                            // 購買紀錄卡片
                            OrderCard(order, isSellerView = false, onActionClick = { id ->
                                viewModel.completeOrder(id) { Toast.makeText(context, "訂單完成", Toast.LENGTH_SHORT).show() }
                            })
                        }
                        1 -> items(viewModel.mySales) { order ->
                            // 銷售紀錄卡片
                            OrderCard(order, isSellerView = true, onActionClick = { id ->
                                viewModel.shipOrder(id) { Toast.makeText(context, "已出貨", Toast.LENGTH_SHORT).show() }
                            })
                        }
                        2 -> items(viewModel.mySwapOrders) { swapOrder ->
                            // 交換紀錄卡片 (特殊邏輯)
                            SwapOrderCard(swapOrder, onActionClick = { id, action ->
                                viewModel.updateSwapStatus(id, action) { Toast.makeText(context, "狀態更新成功", Toast.LENGTH_SHORT).show() }
                            })
                        }
                    }
                }
            }
        }
    }
}

// UI Components (卡片樣式)
// 一般買賣卡片 (購買/銷售)
@Composable
fun OrderCard(
    order: Order,
    isSellerView: Boolean,
    onActionClick: (String) -> Unit
) {
    val dateStr = remember(order.timestamp) { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(order.timestamp.toDate()) }
    val (statusText, statusColor) = when(order.status) {
        "PENDING" -> "待出貨" to Color(0xFFE65100)
        "SHIPPED" -> "已出貨 / 待收貨" to Color(0xFF1976D2)
        "COMPLETED" -> "已完成" to Color(0xFF2E7D32)
        else -> order.status to Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            order.items.forEach { item ->
                Row {
                    Text(item.productTitle, modifier = Modifier.weight(1f))
                    Text("x${item.quantity}", color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$${item.pricePerUnit}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // 按鈕邏輯
                if (isSellerView && order.status == "PENDING") {
                    Button(onClick = { onActionClick(order.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("確認出貨") }
                } else if (!isSellerView && order.status == "SHIPPED") {
                    Button(onClick = { onActionClick(order.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF487F81))) { Text("確認收貨") }
                } else {
                    Spacer(modifier = Modifier.width(1.dp)) // 佔位
                }
                Text("總計: $${order.totalAmount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB00020))
            }
        }
    }
}

// 交換訂單卡片
@Composable
fun SwapOrderCard(
    order: SwapOrder,
    onActionClick: (String, String) -> Unit
) {
    val myId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // 取得我的狀態
    val isShipped = order.shippingStatus[myId] == true
    val isReceived = order.receivingStatus[myId] == true

    // 1. 如果資料庫已經是 COMPLETED，顯示交易完成
    // 2. 如果我也出貨了、我也收貨了，顯示「本方已完成 (等待對方)」
    // 3. 否則顯示「進行中」
    val isMyPartDone = isShipped && isReceived
    val displayStatus = when {
        order.status == "COMPLETED" -> "交易完成"
        isMyPartDone -> "等待對方完成"
        else -> "進行中"
    }

    val statusColor = when {
        order.status == "COMPLETED" -> Color(0xFF2E7D32) // 深綠
        isMyPartDone -> Color(0xFF1976D2) // 藍色
        else -> Color(0xFFE65100) // 橘色
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 標題列
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("交換單號: ${order.id.take(6)}", fontSize = 12.sp, color = Color.Gray)
                Text(displayStatus, color = statusColor, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 顯示內容 (透過 itemsSnapshot)
            val myItems = order.itemsSnapshot.filter { it.ownerId == myId }
            val theirItems = order.itemsSnapshot.filter { it.ownerId != myId }

            if (myItems.isNotEmpty()) {
                Text("我提供:", fontSize = 12.sp, color = Color.Gray)
                myItems.forEach { Text("• ${it.title} x${it.quantity}") }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (theirItems.isNotEmpty()) {
                Text("我收到:", fontSize = 12.sp, color = Color.Gray)
                theirItems.forEach { Text("• ${it.title} x${it.quantity}") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 按鈕區：如果還沒完全結束 (COMPLETED)，就顯示按鈕讓使用者操作
            if (order.status != "COMPLETED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onActionClick(order.id, "SHIP") },
                        enabled = !isShipped, // 如果已出貨就 disable
                        colors = ButtonDefaults.buttonColors(containerColor = if(isShipped) Color.Gray else Color(0xFF1976D2)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if(isShipped) "已出貨" else "確認出貨")
                    }

                    Button(
                        onClick = { onActionClick(order.id, "RECEIVE") },
                        enabled = !isReceived, // 如果已收貨就 disable
                        colors = ButtonDefaults.buttonColors(containerColor = if(isReceived) Color.Gray else Color(0xFF487F81)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if(isReceived) "已收貨" else "確認收貨")
                    }
                }
            } else {
                // 如果已經 COMPLETED
                Text(
                    "雙方交換已完成 🎉",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2E7D32),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}