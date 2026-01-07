package com.example.c2cfastpay_card.UIScreen.components

import android.content.Context
import android.util.Log
import com.example.c2cfastpay_card.data.CartItem
import com.example.c2cfastpay_card.data.NotificationItem 
import com.example.c2cfastpay_card.data.Order
import com.example.c2cfastpay_card.data.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CartRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // 加入購物車
    suspend fun addToCart(cartItem: CartItem): Boolean {
        val userId = getCurrentUserId() ?: return false
        val userCartRef = db.collection("users").document(userId).collection("cart")
        try {
            val querySnapshot = userCartRef.whereEqualTo("productId", cartItem.productId).get().await()
            if (querySnapshot.isEmpty) {
                val newDoc = userCartRef.document()
                val newItem = cartItem.copy(id = newDoc.id, quantity = 1)
                newDoc.set(newItem).await()
                return true
            } else {
                val existingDoc = querySnapshot.documents.first()
                val existingItem = existingDoc.toObject(CartItem::class.java)
                if (existingItem != null) {
                    val stockInt = if (cartItem.stock is Int) cartItem.stock as Int else cartItem.stock.toString().toIntOrNull() ?: 0
                    if (existingItem.quantity + 1 <= stockInt) {
                        existingDoc.reference.update("quantity", existingItem.quantity + 1).await()
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CartRepository", "加入購物車失敗", e)
        }
        return false
    }

    // 取得購物車列表
    fun getCartItemsFlow(): Flow<List<CartItem>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) { trySend(emptyList()); close(); return@callbackFlow }
        val registration = db.collection("users").document(userId).collection("cart")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    trySend(snapshot.toObjects(CartItem::class.java))
                }
            }
        awaitClose { registration.remove() }
    }

    // 刪除單一商品
    suspend fun removeFromCart(cartItemId: String) {
        val userId = getCurrentUserId() ?: return
        db.collection("users").document(userId).collection("cart").document(cartItemId).delete().await()
    }

    // 批次刪除
    suspend fun removeCartItems(cartItemIds: List<String>) {
        val userId = getCurrentUserId() ?: return
        if (cartItemIds.isEmpty()) return
        val batch = db.batch()
        val col = db.collection("users").document(userId).collection("cart")
        cartItemIds.forEach { batch.delete(col.document(it)) }
        batch.commit().await()
    }

    // 更新購物車
    suspend fun updateCartItem(item: CartItem) {
        val userId = getCurrentUserId() ?: return
        if (item.id.isNotEmpty()) {
            db.collection("users").document(userId).collection("cart").document(item.id).set(item).await()
        }
    }

    // 結帳
    suspend fun checkout(itemsToBuy: List<CartItem>): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("未登入"))
        if (itemsToBuy.isEmpty()) return Result.failure(Exception("購物車是空的"))

        // 準備資料
        val ordersBySeller = itemsToBuy.groupBy { it.sellerId }
        val totalCostForBuyer = itemsToBuy.sumOf {
            (it.productPrice.replace(",", "").toLongOrNull() ?: 0L) * it.quantity
        }

        return try {
            // 執行 Firestore Transaction
            // 規則：Transaction 內部必須「先讀取所有資料」，然後「再執行所有寫入」
            db.runTransaction { transaction ->

                // 全部讀取
                // 讀買家資料
                val userRef = db.collection("users").document(userId)
                val userSnapshot = transaction.get(userRef)

                // 預先讀取所有商品 (避免在迴圈中讀取)
                val productMap = mutableMapOf<String, DocumentSnapshot>()
                itemsToBuy.forEach { item ->
                    if (!productMap.containsKey(item.productId)) {
                        val prodRef = db.collection("products").document(item.productId)
                        productMap[item.productId] = transaction.get(prodRef)
                    }
                }

                // 預先讀取所有賣家 (為了加錢)
                val sellerMap = mutableMapOf<String, DocumentSnapshot>()
                ordersBySeller.keys.filter { it.isNotBlank() }.forEach { sellerId ->
                    if (!sellerMap.containsKey(sellerId)) {
                        val sellerRef = db.collection("users").document(sellerId)
                        sellerMap[sellerId] = transaction.get(sellerRef)
                    }
                }

                // 邏輯檢查
                if (!userSnapshot.exists()) throw FirebaseFirestoreException("買家帳號異常", FirebaseFirestoreException.Code.ABORTED)
                val currentPoints = userSnapshot.getLong("points") ?: 0L

                if (currentPoints < totalCostForBuyer) {
                    throw FirebaseFirestoreException("餘額不足！(現有: $currentPoints, 需: $totalCostForBuyer)", FirebaseFirestoreException.Code.ABORTED)
                }

                // 檢查庫存
                itemsToBuy.forEach { item ->
                    val snapshot = productMap[item.productId]
                    if (snapshot == null || !snapshot.exists()) {
                        throw FirebaseFirestoreException("商品【${item.productTitle}】已下架", FirebaseFirestoreException.Code.ABORTED)
                    }
                    val stockStr = snapshot.getString("stock") ?: "0"
                    val currentStock = stockStr.toIntOrNull() ?: 0
                    if (currentStock < item.quantity) {
                        throw FirebaseFirestoreException("商品【${item.productTitle}】庫存不足 (剩 $currentStock)", FirebaseFirestoreException.Code.ABORTED)
                    }
                }

                // 全部寫入
                // 扣買家錢
                transaction.update(userRef, "points", currentPoints - totalCostForBuyer)

                // 處理每個賣家的訂單
                ordersBySeller.forEach { (sellerId, cartItemsForThisSeller) ->
                    var subTotal = 0L
                    val orderItems = mutableListOf<OrderItem>()

                    // 處理庫存與項目
                    cartItemsForThisSeller.forEach { item ->
                        val price = (item.productPrice.replace(",", "").toLongOrNull() ?: 0L)
                        val cost = price * item.quantity
                        subTotal += cost

                        // 扣庫存
                        val snapshot = productMap[item.productId]!!
                        val currentStock = (snapshot.getString("stock") ?: "0").toIntOrNull() ?: 0
                        val newStock = currentStock - item.quantity
                        transaction.update(snapshot.reference, "stock", newStock.toString())

                        orderItems.add(
                            OrderItem(
                                productId = item.productId,
                                productTitle = item.productTitle,
                                productImage = item.productImage,
                                pricePerUnit = price,
                                quantity = item.quantity
                            )
                        )
                    }

                    // 給賣家加錢
                    if (sellerId.isNotBlank()) {
                        val sellerSnap = sellerMap[sellerId]
                        if (sellerSnap != null && sellerSnap.exists()) {
                            transaction.update(sellerSnap.reference, "points", FieldValue.increment(subTotal))
                        }
                    }

                    // 建立訂單紀錄
                    val newOrderRef = db.collection("orders").document()
                    val orderData = Order(
                        id = newOrderRef.id,
                        buyerId = userId,
                        sellerId = sellerId,
                        items = orderItems,
                        totalAmount = subTotal,
                        status = "PENDING",
                        timestamp = com.google.firebase.Timestamp.now()
                    )
                    transaction.set(newOrderRef, orderData)
                }

                // 清空購物車
                itemsToBuy.forEach { item ->
                    val cartItemRef = db.collection("users").document(userId).collection("cart").document(item.id)
                    transaction.delete(cartItemRef)
                }

            }.await() // 等待 Transaction 完成

            // 發送通知
            Log.d("DEBUG_NOTIF", "結帳成功，開始處理通知...")

            ordersBySeller.forEach { (sellerId, items) ->
                Log.d("DEBUG_NOTIF", "檢查賣家: $sellerId")

                if (sellerId.isNotBlank()) {
                    // if (sellerId != userId) {
                    val notif = NotificationItem(
                        userId = sellerId, // 通知對象 (賣家)
                        type = "ORDER",
                        title = "🎉 您有新訂單！",
                        message = "恭喜！有買家下單了您的 ${items.size} 件商品，請前往訂單紀錄確認。",
                        targetId = "ORDER_HISTORY"
                    )

                    db.collection("notifications").document(notif.id).set(notif)
                        .addOnSuccessListener {
                            Log.d("DEBUG_NOTIF", "✅ 通知發送成功！ID: ${notif.id} 給 $sellerId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("DEBUG_NOTIF", "❌ 通知發送失敗", e)
                        }
                    // }
                } else {
                    Log.e("DEBUG_NOTIF", "⚠️ 賣家 ID 為空，無法發送通知 (請檢查商品資料是否包含 ownerId/sellerId)")
                }
            }

            Result.success("結帳成功！")
        } catch (e: Exception) {
            Log.e("CartRepository", "結帳失敗", e)
            Result.failure(e)
        }
    }
}