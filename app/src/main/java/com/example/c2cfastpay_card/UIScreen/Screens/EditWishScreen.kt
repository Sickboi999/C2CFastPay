package com.example.c2cfastpay_card.UIScreen.Screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.WishItem
import com.example.c2cfastpay_card.UIScreen.components.WishRepository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWishScreen(navController: NavController, wishId: String) {
    val context = LocalContext.current
    val repository = remember { WishRepository(context) }
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 表單狀態
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("全新") }
    var selectedLogistics by remember { mutableStateOf(setOf<String>()) }

    var existingImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var existingWish by remember { mutableStateOf<WishItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("全新", "二手", "皆可")
    val logisticOptions = listOf("7-11", "全家", "面交")

    LaunchedEffect(wishId) {
        val wish = repository.getWishById(wishId)
        if (wish != null) {
            if (wish.ownerId != currentUserId) {
                Toast.makeText(context, "您無權編輯此願望", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                return@LaunchedEffect
            }

            // 填入資料
            existingWish = wish
            title = wish.title
            description = wish.description
            price = wish.price
            quantity = wish.qty
            note = wish.memo
            selectedStatus = wish.condition

            // 解析物流字串 (例如 "7-11,全家" -> Set)
            val logistics = mutableSetOf<String>()
            if (wish.payment.contains("7-11")) logistics.add("7-11")
            if (wish.payment.contains("全家")) logistics.add("全家")
            if (wish.payment.contains("面交")) logistics.add("面交")
            selectedLogistics = logistics

            // 處理圖片
            val imgs = mutableListOf<String>()
            if (wish.imageUri.isNotEmpty()) imgs.add(wish.imageUri)
            imgs.addAll(wish.images)
            existingImages = imgs
        } else {
            Toast.makeText(context, "找不到願望", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        isLoading = false
    }

    Scaffold(
        containerColor = WishBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("編輯願望", fontWeight = FontWeight.Bold, color = WishText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WishText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = WishPrimary) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圖片預覽區
                if (existingImages.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("目前圖片 (編輯模式暫不支援修改圖片)", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(existingImages) { url ->
                                    Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 表單區塊
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        EditWishTextField(value = title, onValueChange = { title = it }, label = "願望標題", icon = Icons.Default.Title)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            EditWishTextField(value = price, onValueChange = { if(it.all{c->c.isDigit()}) price=it }, label = "價格", icon = Icons.Default.AttachMoney, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            EditWishTextField(value = quantity, onValueChange = { if(it.all{c->c.isDigit()}) quantity=it }, label = "數量", icon = Icons.Default.ShoppingCart, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        }

                        // 接受狀態選單
                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                            OutlinedTextField(
                                value = selectedStatus, onValueChange = {}, readOnly = true, label = { Text("接受狀態") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                            )
                            ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, modifier = Modifier.background(Color.White)) {
                                statusOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { selectedStatus = option; statusExpanded = false }) }
                            }
                        }

                        // 物流選項
                        Column {
                            Text("希望物流方式", fontSize = 14.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                logisticOptions.forEach { option ->
                                    val isSelected = selectedLogistics.contains(option)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val current = selectedLogistics.toMutableSet()
                                            if (isSelected) current.remove(option) else current.add(option)
                                            selectedLogistics = current
                                        },
                                        label = { Text(option) },
                                        leadingIcon = if(isSelected) {{ Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) }} else null,
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WishPrimary, selectedLabelColor = Color.White)
                                    )
                                }
                            }
                        }

                        EditWishTextField(value = description, onValueChange = { description = it }, label = "描述", icon = Icons.Default.Description, singleLine = false, minLines = 3)
                        EditWishTextField(value = note, onValueChange = { note = it }, label = "備註", icon = Icons.AutoMirrored.Filled.Notes, singleLine = false, minLines = 2)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 儲存按鈕
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            // 組合更新資料
                            val updatedWish = existingWish!!.copy(
                                title = title,
                                description = description,
                                price = price,
                                qty = quantity,
                                condition = selectedStatus,
                                payment = selectedLogistics.joinToString(","),
                                memo = note
                            )
                            // 寫入資料庫
                            repository.updateWish(updatedWish)
                            isSaving = false
                            Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = WishPrimary),
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White) else Text("確認修改", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditWishTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { Text(placeholder, color = Color.LightGray) },
        leadingIcon = if (icon != null) { { Icon(icon, contentDescription = null, tint = WishPrimary) } } else null,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = WishPrimary,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = WishPrimary,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}