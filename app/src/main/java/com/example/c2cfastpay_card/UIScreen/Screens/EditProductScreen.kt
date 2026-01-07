package com.example.c2cfastpay_card.UIScreen.Screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// 沿用主題色
val EditPrimary = Color(0xFF487F81)
val EditBackground = Color(0xFFF4F7F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(navController: NavController, productId: String) {
    val context = LocalContext.current
    val repository = remember { ProductRepository(context) }
    val scope = rememberCoroutineScope()

    // 取得當前用戶 ID
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // 資料狀態
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // 圖片狀態
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var oldImageUrl by remember { mutableStateOf("") }

    // 控制狀態
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var existingProduct by remember { mutableStateOf<ProductItem?>(null) }

    // 初始化：載入舊資料
    LaunchedEffect(productId) {
        val product = repository.getProductById(productId)
        if (product != null) {
            // 安全檢查：如果不是擁有者，強制離開
            if (product.ownerId != currentUserId) {
                Toast.makeText(context, "您無權編輯此商品", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
                return@LaunchedEffect
            }

            existingProduct = product
            title = product.title
            price = product.price
            stock = product.stock
            description = product.description
            oldImageUrl = product.imageUri
        } else {
            Toast.makeText(context, "找不到商品", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        isLoading = false
    }

    // 圖片選擇器
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri = it }
    }

    Scaffold(
        containerColor = EditBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("編輯商品", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EditPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圖片編輯區塊
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F0F0))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                // 顯示新選的圖片
                                Image(
                                    painter = rememberAsyncImagePainter(imageUri),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (oldImageUrl.isNotEmpty()) {
                                // 顯示舊圖片
                                Image(
                                    painter = rememberAsyncImagePainter(oldImageUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // 無圖示顯示預設圖標
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Gray)
                                    Text("更換圖片", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("(點擊圖片可更換)", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 輸入欄位區塊 
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BeautifulEditTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = "商品名稱",
                            icon = Icons.Default.Title
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BeautifulEditTextField(
                                value = price,
                                onValueChange = { if (it.all { c -> c.isDigit() }) price = it },
                                label = "價格",
                                icon = Icons.Default.AttachMoney,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            BeautifulEditTextField(
                                value = stock,
                                onValueChange = { if (it.all { c -> c.isDigit() }) stock = it },
                                label = "庫存數量",
                                icon = Icons.Default.Inventory2,
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        BeautifulEditTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "商品描述",
                            icon = Icons.Default.Description,
                            singleLine = false,
                            minLines = 4
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 儲存按鈕
                Button(
                    onClick = {
                        if (title.isBlank() || price.isBlank() || stock.isBlank()) {
                            Toast.makeText(context, "請填寫完整資訊", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isSaving = true

                            // 圖片邏輯：如果沒選新圖，就繼續用舊圖
                            val finalImageUrl = oldImageUrl

                            val updatedProduct = existingProduct!!.copy(
                                title = title,
                                price = price,
                                stock = stock,
                                description = description,
                                imageUri = finalImageUrl
                            )

                            // 寫入資料庫
                            repository.updateProduct(updatedProduct)

                            isSaving = false
                            Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EditPrimary),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("確認修改", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// 輔助 UI 元件：美化版輸入框
@Composable
fun BeautifulEditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = EditPrimary) }
        } else null,
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = EditPrimary,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = EditPrimary,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}