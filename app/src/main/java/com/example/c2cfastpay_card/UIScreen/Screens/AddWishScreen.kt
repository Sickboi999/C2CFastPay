package com.example.c2cfastpay_card.UIScreen.Screens

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.WishRepository
import com.example.c2cfastpay_card.navigation.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Wish 風格的色票 
val WishPrimary = Color(0xFFFF9800)     // 主色 (橘色)
val WishLight = Color(0xFFFFF3E0)       // 背景
val WishText = Color(0xFF191C1C)        // 文字
val WishBackground = Color(0xFFFDFBF7)  // 頁面

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishScreen(navController: NavController) {
    val context = LocalContext.current
    val wishRepository = remember { WishRepository(context) }
    val viewModel: AddWishViewModel = viewModel(factory = AddWishViewModelFactory(wishRepository))
    val scrollState = rememberScrollState()

    // 表單狀態
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    // 狀態 (全新/二手)
    val statusOptions = listOf("全新", "二手", "皆可")
    var selectedStatus by remember { mutableStateOf(statusOptions[0]) }
    var statusExpanded by remember { mutableStateOf(false) }
    // 物流
    val logisticOptions = listOf("7-11", "全家", "面交")
    var selectedLogistics by remember { mutableStateOf(setOf<String>()) }

    // 相機相關狀態 
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // 顯示 ViewModel 傳來的訊息 (例如：許願成功)
    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    // 相簿選取器
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        photoUris = photoUris + uris
    }

    // 相機啟動器 
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            photoUris = photoUris + tempCameraUri!!
        }
    }

    Scaffold(
        containerColor = WishBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("新增願望", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = WishText)) },
                navigationIcon = {
                    IconButton(onClick = {
                        // 返回到選擇頁 (WishOrProduct)
                        navController.popBackStack(Screen.WishOrProduct.route, inclusive = false)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WishText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圖片上傳區塊
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(16.dp).background(WishPrimary, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("參考圖片", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WishText)
                            Text(" (選填，可上傳多張)", fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 顯示已選圖片
                            items(photoUris) { uri ->
                                Box(modifier = Modifier.size(100.dp)) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    // 刪除按鈕
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(2.dp).clickable { photoUris = photoUris - uri },
                                        tint = Color.White
                                    )
                                }
                            }

                            // 拍照按鈕
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEEEEE)) // 稍微深一點的灰色區別
                                        .border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            // 建立暫存檔案
                                            val file = context.createImageFile()
                                            // 取得 FileProvider URI
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider", 
                                                file
                                            )
                                            tempCameraUri = uri
                                            // 啟動相機
                                            cameraLauncher.launch(uri)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("拍照", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 選相簿按鈕
                            item {
                                Box(
                                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(WishLight.copy(alpha = 0.3f)).border(2.dp, WishPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clickable { galleryLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = WishPrimary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("相簿", fontSize = 12.sp, color = WishPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. 表單區塊
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        BeautifulWishTextField(value = title, onValueChange = { title = it }, label = "願望標題", icon = Icons.Default.Title, placeholder = "例如：PS5 遊戲主機")
                        // 價格與數量並排
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BeautifulWishTextField(value = price, onValueChange = { if (it.all { c -> c.isDigit() }) price = it }, label = "願付價格", icon = Icons.Default.AttachMoney, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            BeautifulWishTextField(value = quantity, onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it }, label = "欲購數量", icon = Icons.Default.ShoppingCart, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        }

                        // 下拉選單：接受狀態
                        ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedStatus, onValueChange = {}, readOnly = true, label = { Text("接受狀態") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = WishPrimary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = WishPrimary, unfocusedBorderColor = Color.LightGray, focusedLabelColor = WishPrimary, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                            )
                            ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, modifier = Modifier.background(Color.White)) {
                                statusOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { selectedStatus = option; statusExpanded = false }) }
                            }
                        }

                        // 物流選項 (FilterChip)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = WishPrimary, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("希望物流方式", color = Color.Gray, fontSize = 14.sp) }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                logisticOptions.forEach { option ->
                                    val isSelected = selectedLogistics.contains(option)
                                    FilterChip(
                                        selected = isSelected, onClick = { val current = selectedLogistics.toMutableSet(); if (isSelected) current.remove(option) else current.add(option); selectedLogistics = current },
                                        label = { Text(option, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = if (isSelected) { { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null,
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WishPrimary, selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White, containerColor = Color.White),
                                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = if (isSelected) Color.Transparent else Color.LightGray), shape = RoundedCornerShape(50)
                                    )
                                }
                            }
                        }
                        BeautifulWishTextField(value = description, onValueChange = { description = it }, label = "願望描述", icon = Icons.Default.Description, singleLine = false, minLines = 4, placeholder = "詳細描述您希望的商品細節...")
                        BeautifulWishTextField(value = note, onValueChange = { note = it }, label = "備註", icon = Icons.AutoMirrored.Filled.Notes, singleLine = false, minLines = 3, placeholder = "其他補充事項...")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 送出按鈕
                Button(
                    onClick = {
                        if (title.isBlank() || description.isBlank() || price.isBlank() || selectedLogistics.isEmpty()) {
                            Toast.makeText(context, "請填寫完整資訊", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.submitWish(title, description, price, quantity, note, selectedStatus, selectedLogistics, photoUris, onSuccess = {
                            navController.navigate(Screen.WishList.route) {
                                popUpTo(Screen.WishList.route) { inclusive = true }
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WishPrimary),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("送出中...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("確認許願", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = WishPrimary); Spacer(modifier = Modifier.height(16.dp)); Text("願望發送中...", fontWeight = FontWeight.Bold, color = WishText) }
                    }
                }
            }
        }
    }
}

@Composable
fun BeautifulWishTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector? = null, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true, minLines: Int = 1, keyboardType: KeyboardType = KeyboardType.Text, placeholder: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(text = label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = { Text(placeholder, color = Color.LightGray) },
        leadingIcon = if (icon != null) { { Icon(icon, contentDescription = null, tint = WishPrimary) } } else null,
        modifier = modifier, singleLine = singleLine, minLines = minLines, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = WishPrimary, unfocusedBorderColor = Color.LightGray, focusedLabelColor = WishPrimary, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
    )
}

// 建立圖片檔案
fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        imageFileName,
        ".jpg",
        storageDir
    )
}