
package com.example.c2cfastpay_card.UIScreen.Screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.UIScreen.Screens.ProductFlowViewModel
import com.example.c2cfastpay_card.model.DraftProduct
import com.example.c2cfastpay_card.navigation.Screen
import com.google.gson.Gson
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// UI 配色 
val Step1Primary = Color(0xFF487F81)    // 主題藍綠色
val Step1Background = Color(0xFFF4F7F7) // 背景淺灰

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductStepOne(
    navController: NavController,
    // 傳入共享 ViewModel，用來暫存選好的照片
    flowViewModel: ProductFlowViewModel
) {
    val context = LocalContext.current
    // 用來暫存「相機拍照前」建立的那個空檔案 URI
    val tempPhotoUri = remember { mutableStateOf<Uri?>(null) }
    // 從共享 ViewModel 取得目前的照片列表
    val currentUris = flowViewModel.photoUris.value
    // 檢查是否有至少一張是「相機實拍」的
    val hasCameraPhoto = flowViewModel.hasCameraPhoto

    // 相機啟動器：拍完照後會回傳 success (true/false)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // 如果拍照成功，就把剛剛暫存的 tempPhotoUri 加入 ViewModel
            tempPhotoUri.value?.let { uri ->
                val oldUris = flowViewModel.photoUris.value
                flowViewModel.photoUris.value = oldUris + uri
                // 標記這張是相機拍的
                flowViewModel.cameraUris.add(uri)
            }
        }
    }

    // 相簿啟動器：選完照片後回傳 URI 列表
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val oldUris = flowViewModel.photoUris.value
            flowViewModel.photoUris.value = oldUris + uris
        }
    }

    fun createUriForCamera(): Uri? {
        return try {
            // 在手機的「圖片」資料夾建立一個暫存檔
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile("product_${System.currentTimeMillis()}", ".jpg", storageDir)
            //透過 FileProvider 取得安全的 URI (Android 7.0+ 規定)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // 如果使用者「允許」，打開相機
            val uri = createUriForCamera()
            if (uri != null) {
                tempPhotoUri.value = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "需要相機權限", Toast.LENGTH_SHORT).show()
        }
    }

    //「相機拍攝」按鈕
    fun onCameraClick() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            // 有權限 -> 開相機
            val uri = createUriForCamera()
            if (uri != null) {
                tempPhotoUri.value = uri
                cameraLauncher.launch(uri)
            }
        } else {
            // 沒有權限 -> 詢問
            permissionLauncher.launch(permission)
        }
    }

    // 刪除照片
    fun removePhoto(uri: Uri) {
        flowViewModel.photoUris.value = flowViewModel.photoUris.value - uri
        if (flowViewModel.cameraUris.contains(uri)) {
            flowViewModel.cameraUris.remove(uri)
        }
    }

    Scaffold(
        containerColor = Step1Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("拍攝商品照", fontWeight = FontWeight.Bold, color = Color(0xFF191C1C)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack(Screen.WishOrProduct.route, inclusive = false)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 切換到許願功能
                    Button(
                        onClick = { navController.navigate(Screen.AddWish.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.padding(end = 12.dp).height(36.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("我要許願", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 提示訊息：還沒用相機拍過 
            if (!hasCameraPhoto) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("為確保交易安全，請至少使用相機拍攝一張真實商品照片。", fontSize = 14.sp, color = Color(0xFFE65100))
                    }
                }
            }

            // 照片預覽區 
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (currentUris.isEmpty()) {
                        // 空狀態
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text("尚未選擇照片", color = Color.LightGray)
                        }
                    } else {
                        // 有照片時顯示水平捲動列表
                        LazyRow(
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentUris) { uri ->
                                Box(modifier = Modifier.width(140.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp))) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // 刪除按鈕
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).clickable { removePhoto(uri) }.padding(4.dp)
                                    )
                                    // 如果是相機拍的，顯示相機圖示
                                    if (flowViewModel.cameraUris.contains(uri)) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Real Photo",
                                            tint = Color.White,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(24.dp).background(Step1Primary.copy(alpha = 0.8f), CircleShape).padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 按鈕：相機/相簿 ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onCameraClick() },
                    modifier = Modifier.weight(1f).height(80.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Step1Primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
                        Text("相機拍攝", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Step1Primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Step1Primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(28.dp))
                        Text("相簿選取", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 下一步操作區 
            Column(modifier = Modifier.fillMaxWidth()) {
                // AI 輔助文案
                Button(
                    onClick = {
                        val uris = flowViewModel.photoUris.value
                        if (uris.isNotEmpty()) {
                            val lastImageUri = uris.last().toString()
                            val encodedUri = URLEncoder.encode(lastImageUri, StandardCharsets.UTF_8.toString())
                            navController.navigate("ai_chat?imageUri=$encodedUri")
                        }
                    },
                    enabled = hasCameraPhoto,  // 強制要求要有相機照片
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(if (hasCameraPhoto) 8.dp else 0.dp, RoundedCornerShape(50)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2), disabledContainerColor = Color.LightGray),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 幫我寫文案", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 手動填寫
                OutlinedButton(
                    onClick = {
                        val uris = flowViewModel.photoUris.value
                        if (uris.isNotEmpty()) {
                            val uriStrings = uris.map { it.toString() }
                            val cameraUriStrings = flowViewModel.cameraUris.map { it.toString() }

                            // 建立 DraftProduct 物件，打包所有圖片資訊
                            val draft = DraftProduct(
                                imageUri = uriStrings.first(),    // 封面圖
                                images = uriStrings,              // 所有圖
                                cameraImages = cameraUriStrings,  // 相機圖  
                                fromAI = false
                            )

                            // 轉成 JSON 字串並編碼 (因為 URL 不能有特殊符號)
                            val json = Gson().toJson(draft)
                            val encodedJson = URLEncoder.encode(json, StandardCharsets.UTF_8.toString())
                            // 跳轉到 AddProductScreen，並帶上 draftJson 參數
                            navController.navigate("add_product?draftJson=$encodedJson")
                        }
                    },
                    enabled = hasCameraPhoto,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasCameraPhoto) Step1Primary else Color.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Step1Primary, disabledContentColor = Color.Gray),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("手動填寫資訊", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}