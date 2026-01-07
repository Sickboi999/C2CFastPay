package com.example.c2cfastpay_card.UIScreen.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.c2cfastpay_card.R

@Composable
fun CardItem(
    modifier: Modifier = Modifier,
    product: ProductItem,
    offset: Offset,
) {
    // 翻轉狀態管理
    var isFlipped by remember { mutableStateOf(false) }

    // 動畫數值 (0f -> 180f)
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400), // 動畫時間
        label = "rotation"
    )

    Card(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Y 軸旋轉造成翻牌效果
                rotationY = rotation
                // 設定相機距離，讓 3D 效果更明顯
                cameraDistance = 8 * density
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        // 根據旋轉角度決定顯示「正面」還是「背面」
        if (rotation <= 90f) {
            // 正面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isFlipped = true } // 點擊翻到背面
            ) {
                // 商品圖片
                Image(
                    painter = rememberAsyncImagePainter(model = product.imageUri),
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 漸層陰影 (讓文字更清楚)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                // 底部文字資訊
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = product.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NT$ ${product.price}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107) // 金黃色強調價格
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.description.take(50) + if(product.description.length > 50) "..." else "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "點擊查看詳情",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        } else {
            // 背面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .clickable { isFlipped = false }
            ) {
                // 背景圖
                Image(
                    painter = painterResource(R.drawable.b_14_business_card_front_page),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 內縮的白色資訊區塊
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.95f))
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        // 標題與價格
                        Text(
                            text = product.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "NT$ ${product.price}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)

                        // 賣家資訊
                        DetailSection("賣家", product.ownerName.ifEmpty { "匿名賣家" })

                        // 其他詳細資訊
                        DetailSection("商品描述", product.description)
                        DetailSection("商品規格", product.specs)
                        DetailSection("商品故事", product.story)
                        DetailSection("注意事項", product.notes)

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "再次點擊返回圖片",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

// 輔助元件：顯示標題與內容
@Composable
fun DetailSection(title: String, content: String) {
    if (content.isNotBlank()) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF487F81) // 藍綠色
            )
            Text(
                text = content,
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}