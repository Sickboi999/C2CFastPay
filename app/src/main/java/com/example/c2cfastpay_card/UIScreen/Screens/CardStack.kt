package com.example.c2cfastpay_card.UIScreen.Screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.c2cfastpay_card.R
import com.example.c2cfastpay_card.UIScreen.components.ProductItem
import com.example.c2cfastpay_card.UIScreen.components.ProductRepository
import com.example.c2cfastpay_card.UIScreen.components.MatchRepository
import com.example.c2cfastpay_card.UIScreen.components.CardItem
import com.example.c2cfastpay_card.navigation.Screen
import com.spartapps.swipeablecards.state.rememberSwipeableCardsState
import com.spartapps.swipeablecards.ui.lazy.LazySwipeableCards
import com.spartapps.swipeablecards.ui.lazy.items
import com.spartapps.swipeablecards.ui.SwipeableCardDirection
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

@Composable
fun CardStackScreen(navController: NavController) {
    val context = LocalContext.current
    val productRepository = remember { ProductRepository(context) }
    val matchRepository = remember { MatchRepository(context) }

    val viewModel: CardStackViewModel = viewModel(
        factory = CardStackViewModelFactory(productRepository, matchRepository)
    )

    val cardsToShow by viewModel.cards.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 監聽配對成功狀態 
    val matchedProduct by viewModel.matchedProduct.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPotentialMatches()
    }

    // 主畫面
    CardStackLayout(
        navController = navController,
        items = cardsToShow,
        isLoading = isLoading,
        viewModel = viewModel
    )

    // 顯示配對成功彈窗 
    if (matchedProduct != null) {
        MatchSuccessDialog(
            product = matchedProduct!!,
            onDismiss = { viewModel.dismissMatchPopup() },
            onChatClick = {
                viewModel.dismissMatchPopup()
                // 這裡導向 History，因為配對成功是 SWAP 類型，會在 History 的預設分頁看到
                navController.navigate(Screen.History.route)
            }
        )
    }
}

@Composable
fun CardStackLayout(
    navController: NavController,
    items: List<ProductItem>,
    isLoading: Boolean,
    viewModel: CardStackViewModel,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(modifier = modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        val (topBar, cardDeck, controlButtons) = createRefs()

        // 頂部 Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .constrainAs(topBar) { top.linkTo(parent.top) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(painterResource(R.drawable.a_1_back_buttom), contentDescription = "Back", modifier = Modifier.size(24.dp))
            }
            Text("商品名片配對", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF487F81))
        }

        // 卡片區域 
        Box(
            modifier = Modifier
                .constrainAs(cardDeck) {
                    top.linkTo(topBar.bottom, margin = 20.dp)
                    bottom.linkTo(controlButtons.top, margin = 20.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF487F81))
            } else if (items.isEmpty()) {
                Text("沒有更多商品了", color = Color.Gray, fontSize = 18.sp)
            } else {
                // 使用 key 確保當 items 變動時重組
                key(items.size) {
                    val state = rememberSwipeableCardsState(itemCount = { items.size })
                    val scope = rememberCoroutineScope()

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.75f)
                        ) {
                            LazySwipeableCards(
                                state = state,
                                onSwipe = { swipedProduct, direction ->
                                    val product = swipedProduct as ProductItem
                                    if (direction == SwipeableCardDirection.Right) {
                                        viewModel.swipeRight(product)
                                    } else {
                                        viewModel.swipeLeft(product)
                                    }
                                }
                            ) {
                                items(items) { product, index, offset ->
                                    Box {
                                        // 原始卡片
                                        CardItem(
                                            product = product,
                                            offset = offset
                                        )

                                        // 滑動動畫回饋 (紅心/綠叉) 
                                        // 確保只在最上層卡片顯示動畫效果
                                        // 這裡假設 LazySwipeableCards 的 offset 是針對當前被拖曳的卡片
                                        SwipeFeedbackOverlay(offset.x)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 底部按鈕 
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
                        ) {
                            // 叉叉按鈕：改成綠色
                            FloatingActionButton(
                                onClick = { scope.launch { state.swipe(SwipeableCardDirection.Left) } },
                                containerColor = Color.White,
                                contentColor = Color(0xFF4CAF50), // 綠色
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Pass", modifier = Modifier.size(32.dp))
                            }

                            // 愛心按鈕：改成紅色
                            FloatingActionButton(
                                onClick = { scope.launch { state.swipe(SwipeableCardDirection.Right) } },
                                containerColor = Color.White,
                                contentColor = Color(0xFFFF5252), // 紅色
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Like", modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // 底部文字按鈕
        TextButton(
            onClick = { navController.navigate(Screen.History.route) },
            modifier = Modifier.constrainAs(controlButtons) {
                bottom.linkTo(parent.bottom, margin = 20.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("前往以物易物協商室", color = Color(0xFF487F81), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 滑動回饋遮罩
@Composable
fun SwipeFeedbackOverlay(offsetX: Float) {
    // 根據滑動距離計算透明度 (0 ~ 1)，滑動 300px 達到全透明
    val alpha = min(abs(offsetX) / 300f, 1f)

    // 只有當滑動有一定程度時才顯示，避免干擾
    if (alpha > 0.1f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)) // 跟卡片圓角一致
                .zIndex(2f), // 確保在最上層
            contentAlignment = Alignment.Center
        ) {
            if (offsetX > 0) {
                // 右滑：顯示紅色愛心
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = alpha * 0.5f)), // 半透明白底
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = Color(0xFFFF5252).copy(alpha = alpha),
                        modifier = Modifier
                            .size(100.dp)
                            .scale(1f + alpha * 0.2f) // 輕微放大效果
                            .rotate(-15f) // 稍微傾斜
                    )
                }
            } else {
                // 左滑：顯示綠色叉叉
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = alpha * 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Nope",
                        tint = Color(0xFF4CAF50).copy(alpha = alpha),
                        modifier = Modifier
                            .size(100.dp)
                            .scale(1f + alpha * 0.2f)
                            .rotate(15f)
                    )
                }
            }
        }
    }
}

// 配對成功彈窗 
@Composable
fun MatchSuccessDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onChatClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "配對成功！🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF487F81) // 主題綠色
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "您與 ${product.ownerName} 互相喜歡！",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 顯示配對商品圖 (圓形大圖)
                AsyncImage(
                    model = product.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFFFF5252), CircleShape), // 紅色愛心邊框
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 按鈕區
                Button(
                    onClick = onChatClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF487F81)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Handshake, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("前往協商", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("繼續滑", color = Color.Gray, fontSize = 16.sp)
                }
            }
        }
    }
}