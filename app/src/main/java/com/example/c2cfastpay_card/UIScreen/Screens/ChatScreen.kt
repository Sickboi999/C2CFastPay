package com.example.c2cfastpay_card.UIScreen.Screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Handshake
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.c2cfastpay_card.UIScreen.components.ChatRepository
import com.example.c2cfastpay_card.UIScreen.components.MatchDetails
import com.example.c2cfastpay_card.UIScreen.components.SwapProposal
import com.example.c2cfastpay_card.data.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    matchId: String,    // 聊天室 ID
    chatRepository: ChatRepository = ChatRepository(LocalContext.current)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // UI 狀態變數 
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var matchDetails by remember { mutableStateOf<MatchDetails?>(null) }
    var showProposalDialog by remember { mutableStateOf(false) }

    // 聊天室類型與對方名稱
    var matchType by remember { mutableStateOf("SWAP") }
    var chatTitle by remember { mutableStateOf("聊天室") }

    LaunchedEffect(matchId) {
        // 取得基本訊息與詳情
        matchDetails = chatRepository.getMatchDetails(matchId)
        chatRepository.getMessagesFlow(matchId).collect { messages = it }
    }

    // 獲取類型與對方真實名稱
    LaunchedEffect(matchId) {
        val db = FirebaseFirestore.getInstance()
        try {
            val doc = db.collection("matches").document(matchId).get().await()
            matchType = doc.getString("type") ?: "SWAP" // 判斷是否為交換模式

            val users = doc.get("users") as? List<String> ?: emptyList()
            val otherUserId = users.find { it != currentUserId }

            if (otherUserId != null) {
                val userDoc = db.collection("users").document(otherUserId).get().await()
                // 優先使用 name，若無則用 displayName
                chatTitle = userDoc.getString("name") ?: userDoc.getString("displayName") ?: "對方"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    // 使用查詢到的真實名稱
                    title = { Text(chatTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // SWAP 類型才顯示交易區塊
                if (matchDetails != null && matchType == "SWAP") {
                    TradeHeader(matchDetails!!)
                    HorizontalDivider()
                }
            }
        },
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // SWAP 類型才顯示協商按鈕 
                if (matchType == "SWAP") {
                    IconButton(onClick = { showProposalDialog = true }) {
                        Icon(Icons.Default.Handshake, contentDescription = "Proposal", tint = Color(0xFF487F81), modifier = Modifier.size(28.dp))
                    }
                }

                // 輸入框
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    placeholder = { Text("輸入訊息...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray, focusedBorderColor = Color(0xFF487F81))
                )

                // 發送按鈕
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                chatRepository.sendMessage(matchId, messageText)
                                messageText = ""
                            }
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (messageText.isNotBlank()) Color(0xFF487F81) else Color.Gray)
                }
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        // 自動捲動到最新訊息
        LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFFF5F5F5))) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { message ->
                    val isMe = message.senderId == currentUserId
                    // 判斷是「交換提案」還是「普通文字」 
                    if (message.type == "PROPOSAL" && message.proposal != null) {
                        ProposalBubble(
                            proposalMap = message.proposal,
                            isMe = isMe,
                            productsInfo = matchDetails,
                            onAccept = {
                                // 同意提案：更新狀態並建立訂單
                                scope.launch {
                                    val offered = message.proposal["offeredItems"] as? Map<String, Any> ?: emptyMap()
                                    val requested = message.proposal["requestedItems"] as? Map<String, Any> ?: emptyMap()

                                    // 轉型處理 (Firebase Map 轉 Int)
                                    val offeredInt = offered.entries.associate { it.key to (it.value as Number).toInt() }
                                    val requestedInt = requested.entries.associate { it.key to (it.value as Number).toInt() }

                                    val proposal = SwapProposal(
                                        id = message.proposal["id"] as? String ?: "",
                                        senderId = message.senderId,
                                        offeredItems = offeredInt,
                                        requestedItems = requestedInt
                                    )
                                    // 呼叫 Repository 更新狀態為 ACCEPTED
                                    val success = chatRepository.updateProposalStatus(matchId, message.id, proposal, "ACCEPTED")
                                    if(success) Toast.makeText(context, "訂單已成立", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "操作失敗(可能庫存不足)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReject = {
                                // 拒絕提案
                                scope.launch {
                                    chatRepository.updateProposalStatus(matchId, message.id, SwapProposal(), "REJECTED")
                                }
                            }
                        )
                    } else {
                        // 一般文字訊息
                        MessageBubble(message, isMe)
                    }
                }
            }
        }
    }

    // 只有 SWAP 模式會觸發這個 Dialog
    if (showProposalDialog && matchDetails != null) {
        ProposalDialog(
            details = matchDetails!!,
            onDismiss = { showProposalDialog = false },
            onSendProposal = { myItems, theirItems ->
                scope.launch {
                    val proposal = SwapProposal(senderId = currentUserId, offeredItems = myItems, requestedItems = theirItems, status = "PENDING")
                    chatRepository.sendProposal(matchId, proposal)
                    showProposalDialog = false
                }
            }
        )
    }
}

@Composable
fun TradeHeader(details: MatchDetails) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFAFAFA)).padding(vertical = 8.dp)) {
        if (details.myAvailableProducts.isNotEmpty()) {
            Text("對方喜歡你的：", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(details.myAvailableProducts) { prod -> MiniProductChip(prod) }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (details.theirAvailableProducts.isNotEmpty()) {
            Text("你喜歡對方的：", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(details.theirAvailableProducts) { prod -> MiniProductChip(prod) }
            }
        }
    }
}

// 顯示雙方可用商品的頂部區塊
@Composable
fun MiniProductChip(prod: Map<String, Any>) {
    val title = prod["title"] as? String ?: ""
    val imgUrl = prod["imageUrl"] as? String ?: ""
    Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color.LightGray), modifier = Modifier.padding(end = 8.dp).width(100.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            AsyncImage(model = imgUrl, contentDescription = null, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProposalDialog(
    details: MatchDetails,
    onDismiss: () -> Unit,
    onSendProposal: (Map<String, Int>, Map<String, Int>) -> Unit
) {
    val selectedMyItems = remember { mutableStateMapOf<String, Int>() }
    val selectedTheirItems = remember { mutableStateMapOf<String, Int>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("發起交換提案", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF487F81))
                Spacer(modifier = Modifier.height(16.dp))

                Text("你要提供什麼？", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(details.myAvailableProducts) { prod ->
                        ProductQuantityRow(prod, selectedMyItems)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("你想要什麼？", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(details.theirAvailableProducts) { prod ->
                        ProductQuantityRow(prod, selectedTheirItems)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(
                        onClick = { onSendProposal(selectedMyItems.toMap(), selectedTheirItems.toMap()) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF487F81)),
                        enabled = selectedMyItems.isNotEmpty() || selectedTheirItems.isNotEmpty()
                    ) {
                        Text("送出提案")
                    }
                }
            }
        }
    }
}

// 選擇交換商品的彈窗
@Composable
fun ProductQuantityRow(
    prod: Map<String, Any>,
    selectedMap: MutableMap<String, Int>
) {
    val id = prod["id"] as String
    val title = prod["title"] as String

    val stockRaw = prod["stock"]
    val maxStock = when (stockRaw) {
        is Number -> stockRaw.toInt()
        is String -> stockRaw.toIntOrNull() ?: 1
        else -> 1
    }

    val currentQty = selectedMap[id] ?: 0
    val isChecked = currentQty > 0

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { checked ->
                if (checked) selectedMap[id] = 1 else selectedMap.remove(id)
            }
        )
        Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)

        if (isChecked) {
            IconButton(onClick = { if (currentQty > 1) selectedMap[id] = currentQty - 1 }) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text("$currentQty", fontSize = 14.sp)
            IconButton(
                onClick = { if (currentQty < maxStock) selectedMap[id] = currentQty + 1 },
                enabled = currentQty < maxStock
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 顯示在聊天室內的提案氣泡 (含接受/拒絕按鈕)
@Composable
fun ProposalBubble(
    proposalMap: Map<String, Any>,
    isMe: Boolean,
    productsInfo: MatchDetails?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val status = proposalMap["status"] as? String ?: "PENDING"
    val offeredItemsRaw = proposalMap["offeredItems"] as? Map<String, Any> ?: emptyMap()
    val requestedItemsRaw = proposalMap["requestedItems"] as? Map<String, Any> ?: emptyMap()

    val align = if (isMe) Alignment.End else Alignment.Start
    val bgColor = Color.White

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.width(280.dp).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFF487F81))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("交換提案", fontWeight = FontWeight.Bold, color = Color(0xFF487F81))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("提供:", fontSize = 12.sp, color = Color.Gray)
                offeredItemsRaw.forEach { (id, qty) ->
                    val title = findTitleById(id, productsInfo)
                    Text("• $title x $qty", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("索取:", fontSize = 12.sp, color = Color.Gray)
                requestedItemsRaw.forEach { (id, qty) ->
                    val title = findTitleById(id, productsInfo)
                    Text("• $title x $qty", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (status == "PENDING") {
                    if (!isMe) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                Text("婉拒", color = Color.Red)
                            }
                            Button(onClick = onAccept, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF487F81)), shape = RoundedCornerShape(8.dp)) {
                                Text("接受")
                            }
                        }
                    } else {
                        Text("等待對方回應...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                } else {
                    val resultText = if (status == "ACCEPTED") "訂單已成立 🎉" else "提案已取消"
                    val resultColor = if (status == "ACCEPTED") Color(0xFF4CAF50) else Color.Gray
                    Text(text = resultText, color = resultColor, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

fun findTitleById(id: String, details: MatchDetails?): String {
    if (details == null) return "商品"
    val p1 = details.myAvailableProducts.find { it["id"] == id }
    if (p1 != null) return p1["title"] as? String ?: "商品"
    val p2 = details.theirAvailableProducts.find { it["id"] == id }
    if (p2 != null) return p2["title"] as? String ?: "商品"
    return "商品"
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val backgroundColor = if (isMe) Color(0xFF487F81) else Color.White
    val textColor = if (isMe) Color.White else Color.Black
    val shape = if (isMe) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(shape = shape, color = backgroundColor, shadowElevation = 1.dp, modifier = Modifier.widthIn(max = 280.dp)) {
            Text(text = message.text, modifier = Modifier.padding(12.dp), color = textColor, fontSize = 16.sp)
        }
        Text(text = formatTime(message.timestamp), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp))
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(timestamp)
}