package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import android.util.Log
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.navigation.Screen
import com.google.gson.Gson
import androidx.compose.ui.graphics.vector.ImageVector

// 定義 Sale 的色票 
val SalePrimary = Color(0xFF487F81)     // 主色 (藍綠)
val SaleLight = Color(0xFFE0F2F1)       // 淺色背景
val SaleText = Color(0xFF191C1C)        // 文字色
val SaleBackground = Color(0xFFF4F7F7)  // 頁面底色

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavController, draftJson: String? = null) {
    val context = LocalContext.current
    val productRepository = remember { ProductRepository(context) }

    // 初始化 ViewModel
    val viewModel: AddProductViewModel = viewModel(factory = AddProductViewModelFactory(productRepository))
    val scrollState = rememberScrollState()

    // 表單狀態變數 
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var cameraUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("1") }

    // 狀態選單
    var selectedStatus by remember { mutableStateOf("全新") }
    var statusExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("全新", "二手")

    // 物流選項 
    val logisticOptions = listOf("7-11", "全家", "面交")
    var selectedLogistics by remember { mutableStateOf(setOf<String>()) }

    var currentWishUuid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.uploadStatus) {
        viewModel.uploadStatus?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(draftJson) {
        if (!draftJson.isNullOrEmpty() && draftJson != "null") {
            try {
                // 將 JSON 字串轉回 WishDataDTO 物件
                val wishData = Gson().fromJson(draftJson, WishDataDTO::class.java)
                
                // 填入文字欄位
                title = wishData.title
                content = wishData.description
                story = wishData.story
                price = wishData.price
                stock = wishData.qty

                if (wishData.uuid.isNotEmpty()) {
                    currentWishUuid = wishData.uuid
                }

                // 填入圖片
                val newUris = mutableListOf<Uri>()
                if (wishData.imageUri.isNotEmpty()) newUris.add(wishData.imageUri.toUri())
                if (wishData.images.isNotEmpty()) newUris.addAll(wishData.images.map { it.toUri() })
                if (newUris.isNotEmpty()) photoUris = (photoUris + newUris).distinct()

                // 標記相機圖片
                if (wishData.cameraImages.isNotEmpty()) {
                    val camUris = wishData.cameraImages.map { it.toUri() }
                    cameraUris = (cameraUris + camUris).toSet()
                }

                // 設定新舊狀態
                selectedStatus = if (wishData.condition == "全新") "全新" else "二手"

                // 自動勾選物流 
                if (wishData.payment.isNotEmpty()) {
                    val newLogistics = mutableSetOf<String>()
                    if (wishData.payment.contains("7-11")) newLogistics.add("7-11")
                    if (wishData.payment.contains("全家")) newLogistics.add("全家")
                    if (wishData.payment.contains("面交")) newLogistics.add("面交")
                    if (newLogistics.isNotEmpty()) selectedLogistics = newLogistics
                }
            } catch (e: Exception) {
                Log.e("AddProductScreen", "解析失敗", e)
            }
        }
    }

    // 相簿選取器
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> photoUris = photoUris + uris }

    Scaffold(
        containerColor = SaleBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("上架商品", fontWeight = FontWeight.Bold, color = SaleText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SaleText) }
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
                // 圖片區塊
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("商品圖片", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(photoUris) { uri ->
                                Image(painter = rememberAsyncImagePainter(uri), contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            }
                            item {
                                Box(modifier = Modifier.size(100.dp).background(SaleLight).clickable { galleryLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = SalePrimary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // 表單區塊
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        BeautifulTextField(value = title, onValueChange = { title = it }, label = "商品標題", icon = Icons.Default.Title)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BeautifulTextField(value = price, onValueChange = { if(it.all{c->c.isDigit()}) price=it }, label = "價格", modifier = Modifier.weight(1f))
                            BeautifulTextField(value = stock, onValueChange = { if(it.all{c->c.isDigit()}) stock=it }, label = "庫存", modifier = Modifier.weight(1f))
                        }

                        // 新舊狀態
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedStatus,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("新舊狀態") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = SalePrimary,
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                statusOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedStatus = option
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 物流選項 
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = SalePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("物流方式", fontSize = 14.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
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
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SalePrimary,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if(isSelected) Color.Transparent else Color.LightGray
                                        )
                                    )
                                }
                            }
                        }

                        BeautifulTextField(value = content, onValueChange = { content = it }, label = "商品文案", singleLine = false, minLines = 3)
                        BeautifulTextField(value = story, onValueChange = { story = it }, label = "商品故事", singleLine = false, minLines = 2)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 送出按鈕
                Button(
                    onClick = {
                        if (title.isBlank() || price.isBlank() || selectedLogistics.isEmpty()) {
                            Toast.makeText(context, "請填寫完整資訊", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.submitProduct(
                            title = title,
                            description = content,
                            story = story,
                            price = price,
                            stock = stock,
                            condition = selectedStatus,
                            logistics = selectedLogistics,
                            photoUris = photoUris,
                            wishUuid = currentWishUuid,
                            onSuccess = {
                                navController.navigate(Screen.Sale.route) {
                                    popUpTo(Screen.Sale.route) { inclusive = true }
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = SalePrimary),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) CircularProgressIndicator(color = Color.White) else Text("確認上架", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SalePrimary) }
            }
        }
    }
}

@Composable
fun BeautifulTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector? = null, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true, minLines: Int = 1, keyboardType: KeyboardType = KeyboardType.Text, placeholder: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, placeholder = { Text(placeholder) },
        leadingIcon = if(icon!=null){{Icon(icon,null,tint=SalePrimary)}}else null,
        modifier = modifier, singleLine = singleLine, minLines = minLines, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = SalePrimary, unfocusedBorderColor = Color.LightGray)
    )
}

data class WishDataDTO(
    val uuid: String = "",
    val title: String = "", val price: String = "", val description: String = "", val story: String = "", val qty: String = "", val payment: String = "", val imageUri: String = "", val images: List<String> = emptyList(), val cameraImages: List<String> = emptyList(), val condition: String = ""
)