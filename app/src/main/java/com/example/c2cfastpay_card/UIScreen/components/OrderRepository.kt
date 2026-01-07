package com.example.c2cfastpay_card.UIScreen.components

import android.util.Log
import com.example.c2cfastpay_card.data.Order
import com.example.c2cfastpay_card.data.SwapOrder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class OrderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 一般購物訂單 
    // 取得「我購買的」
    suspend fun getMyPurchases(): List<Order> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("buyerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // 取得「我售出的」
    suspend fun getMySales(): List<Order> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("sellerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // 賣家出貨
    suspend fun shipOrder(orderId: String) {
        db.collection("orders").document(orderId).update("status", "SHIPPED").await()
    }

    // 買家收貨
    suspend fun completeOrder(orderId: String) {
        db.collection("orders").document(orderId).update("status", "COMPLETED").await()
    }

    // 交換訂單
    // 取得「我的交換」
    suspend fun getMySwapOrders(): List<SwapOrder> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("swap_orders")
                .whereArrayContains("users", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            snapshot.toObjects(SwapOrder::class.java)
        } catch (e: Exception) {
            Log.e("OrderRepo", "Get swap orders failed", e)
            emptyList()
        }
    }

    // 更新交換狀態 (出貨/收貨)
    suspend fun updateSwapStatus(orderId: String, action: String) {
        val myId = auth.currentUser?.uid ?: return
        val orderRef = db.collection("swap_orders").document(orderId)

        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(orderRef)

                // 讀取目前的狀態 Map
                val currentShipping = snapshot.get("shippingStatus") as? Map<String, Boolean> ?: emptyMap()
                val currentReceiving = snapshot.get("receivingStatus") as? Map<String, Boolean> ?: emptyMap()

                if (action == "SHIP") {
                    val newMap = currentShipping.toMutableMap()
                    newMap[myId] = true
                    transaction.update(orderRef, "shippingStatus", newMap)
                } else if (action == "RECEIVE") {
                    val newMap = currentReceiving.toMutableMap()
                    newMap[myId] = true
                    transaction.update(orderRef, "receivingStatus", newMap)

                    // 檢查是否所有人都收到貨了 (雙方都 True 則完成)
                    if (newMap.isNotEmpty() && newMap.values.all { it }) {
                        transaction.update(orderRef, "status", "COMPLETED")
                    }
                }
                null // 這裡回傳 null 是為了讓 Transaction 語法正確
            }.await()
        } catch (e: Exception) {
            Log.e("OrderRepo", "Update swap status failed", e)
            throw e
        }
    }
}