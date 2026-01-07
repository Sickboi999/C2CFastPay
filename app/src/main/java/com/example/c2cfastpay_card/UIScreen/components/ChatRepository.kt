package com.example.c2cfastpay_card.UIScreen.components

import android.content.Context
import android.util.Log
import com.example.c2cfastpay_card.data.Message
import com.example.c2cfastpay_card.data.SwapOrder
import com.example.c2cfastpay_card.data.SwapOrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class MatchDetails(
    val myAvailableProducts: List<Map<String, Any>> = emptyList(),
    val theirAvailableProducts: List<Map<String, Any>> = emptyList(),
    val otherUserName: String = "聊天室" // 顯示對方暱稱用
)

data class SwapProposal(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val offeredItems: Map<String, Int> = emptyMap(),
    val requestedItems: Map<String, Int> = emptyMap(),
    val status: String = "PENDING"
)

class ChatRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // 讀取配對資料
    suspend fun getMatchDetails(matchId: String): MatchDetails? {
        val myId = getCurrentUserId() ?: return null

        return try {
            val doc = db.collection("matches").document(matchId).get().await()
            if (!doc.exists()) return null

            val data = doc.data ?: return null
            val user1Id = data["user1Id"] as? String ?: ""
            val user2Id = data["user2Id"] as? String ?: ""

            // 抓取對方名字
            val otherUserId = if (myId == user1Id) user2Id else user1Id
            var otherName = "對方"
            if (otherUserId.isNotEmpty()) {
                val userDoc = db.collection("users").document(otherUserId).get().await()
                otherName = userDoc.getString("name")
                    ?: userDoc.getString("username")
                            ?: userDoc.getString("displayName")
                            ?: "對方"
            }

            // 取得雙方喜歡的 ID 列表 
            // user1LikedIds = User1 喜歡的 (User2 的東西)
            val user1LikedIds = data["user1LikedProductIds"] as? List<String> ?: emptyList()
            // user2LikedIds = User2 喜歡的 (User1 的東西)
            val user2LikedIds = data["user2LikedProductIds"] as? List<String> ?: emptyList()

            // 判斷哪些 ID 屬於誰
            val myTargetIds: List<String>
            val theirTargetIds: List<String>

            if (myId == user1Id) {
                // 我是 User1
                // 我擁有的籌碼 = User2 喜歡的那些
                myTargetIds = user2LikedIds
                // 對方擁有的籌碼 = User1 (我) 喜歡的那些
                theirTargetIds = user1LikedIds
            } else {
                // 我是 User2
                // 我擁有的籌碼 = User1 喜歡的那些
                myTargetIds = user1LikedIds
                // 對方擁有的籌碼 = User2 (我) 喜歡的那些
                theirTargetIds = user2LikedIds
            }

            val myAvailableProducts = fetchProductsByIds(myTargetIds)
            val theirAvailableProducts = fetchProductsByIds(theirTargetIds)

            MatchDetails(
                myAvailableProducts = myAvailableProducts,
                theirAvailableProducts = theirAvailableProducts,
                otherUserName = otherName
            )

        } catch (e: Exception) {
            Log.e("ChatRepository", "讀取 MatchDetails 失敗", e)
            null
        }
    }

    // 輔助函式：根據 ID 列表去抓最新商品資料
    private suspend fun fetchProductsByIds(ids: List<String>): List<Map<String, Any>> {
        if (ids.isEmpty()) return emptyList()
        val list = mutableListOf<Map<String, Any>>()

        for (id in ids) {
            try {
                val doc = db.collection("products").document(id).get().await()
                if (doc.exists()) {
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    // 補上 ID
                    data["id"] = doc.id
                    // 確保 stock 存在且正確 (轉型保護)
                    val stockRaw = data["stock"]
                    val stockStr = when(stockRaw) {
                        is String -> stockRaw
                        is Number -> stockRaw.toString()
                        else -> "1"
                    }
                    data["stock"] = stockStr

                    // 確保 title 和 imageUri 存在 (防崩潰)
                    if (!data.containsKey("title")) data["title"] = "未知商品"
                    if (!data.containsKey("imageUrl")) {
                        data["imageUrl"] = data["imageUri"] ?: ""
                    }

                    list.add(data)
                }
            } catch (e: Exception) {
                Log.w("ChatRepo", "抓取商品 $id 失敗", e)
            }
        }
        return list
    }

    // 發送訊息
    suspend fun sendMessage(matchId: String, messageText: String) {
        val userId = getCurrentUserId() ?: return
        val messageId = UUID.randomUUID().toString()
        val newMessageMap = hashMapOf(
            "id" to messageId, "senderId" to userId, "text" to messageText,
            "type" to "TEXT", "timestamp" to System.currentTimeMillis()
        )
        try {
            db.collection("matches").document(matchId).collection("messages").document(messageId).set(newMessageMap).await()
            updateLastMessage(matchId, messageText)
        } catch (e: Exception) { Log.e("ChatRepo", "Send Msg Error", e) }
    }

    // 發送提案
    suspend fun sendProposal(matchId: String, proposal: SwapProposal) {
        val userId = getCurrentUserId() ?: return
        val messageId = UUID.randomUUID().toString()
        val proposalMap = mapOf(
            "id" to proposal.id, "senderId" to userId,
            "offeredItems" to proposal.offeredItems, "requestedItems" to proposal.requestedItems,
            "status" to proposal.status
        )
        val newMessageMap = hashMapOf(
            "id" to messageId, "senderId" to userId, "text" to "提出了交換方案",
            "type" to "PROPOSAL", "proposal" to proposalMap, "timestamp" to System.currentTimeMillis()
        )
        try {
            db.collection("matches").document(matchId).collection("messages").document(messageId).set(newMessageMap).await()
            updateLastMessage(matchId, "提出了交換方案")
        } catch (e: Exception) { Log.e("ChatRepository", "提案發送失敗", e) }
    }

    // 更新提案 (接受 -> 扣庫存 -> 建訂單)
    suspend fun updateProposalStatus(matchId: String, messageId: String, proposal: SwapProposal, newStatus: String): Boolean {
        if (newStatus == "REJECTED") {
            db.collection("matches").document(matchId).collection("messages").document(messageId)
                .update("proposal.status", "REJECTED").await()
            updateLastMessage(matchId, "已婉拒交換提案")
            return true
        }

        if (newStatus == "ACCEPTED") {
            return try {
                db.runTransaction { transaction ->
                    val allItems = proposal.offeredItems.keys + proposal.requestedItems.keys
                    val allItemQtys = proposal.offeredItems + proposal.requestedItems

                    val productRefs = allItems.map { db.collection("products").document(it) }
                    val productSnapshots = productRefs.map { transaction.get(it) }

                    val itemsSnapshotList = mutableListOf<SwapOrderItem>()

                    for (snapshot in productSnapshots) {
                        val pid = snapshot.id
                        val requiredQty = allItemQtys[pid] ?: 0

                        // 讀取庫存 (強力轉型)
                        val currentStockRaw = snapshot.get("stock")
                        val currentStock: Int = when (currentStockRaw) {
                            is Number -> currentStockRaw.toInt()
                            is String -> currentStockRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        if (currentStock < requiredQty) {
                            throw FirebaseFirestoreException("庫存不足", FirebaseFirestoreException.Code.ABORTED)
                        }

                        // 扣庫存 (轉 String 寫回，避免型別錯誤)
                        val newStock = currentStock - requiredQty
                        transaction.update(snapshot.reference, "stock", newStock.toString())

                        // 建立快照 (訂單紀錄要顯示用的)
                        if (requiredQty > 0) {
                            itemsSnapshotList.add(
                                SwapOrderItem(
                                    productId = pid,
                                    ownerId = snapshot.getString("ownerId") ?: "",
                                    title = snapshot.getString("title") ?: "未知商品",
                                    imageUrl = snapshot.getString("imageUri") ?: snapshot.getString("imageUrl") ?: "",
                                    quantity = requiredQty
                                )
                            )
                        }
                    }

                    // 更新訊息狀態
                    val messageRef = db.collection("matches").document(matchId).collection("messages").document(messageId)
                    transaction.update(messageRef, "proposal.status", "ACCEPTED")

                    // 建立訂單
                    val orderId = UUID.randomUUID().toString()
                    val userIds = matchId.split("_")
                    val initialStatusMap = userIds.associateWith { false }

                    val newSwapOrder = SwapOrder(
                        id = orderId, matchId = matchId, users = userIds,
                        itemQuantities = allItemQtys,
                        itemsSnapshot = itemsSnapshotList, // ★ 傳入快照
                        shippingStatus = initialStatusMap, receivingStatus = initialStatusMap,
                        status = "PROCESSING", timestamp = com.google.firebase.Timestamp.now()
                    )

                    val orderRef = db.collection("swap_orders").document(orderId)
                    transaction.set(orderRef, newSwapOrder)

                    val matchRef = db.collection("matches").document(matchId)
                    transaction.update(matchRef, "lastMessage", "訂單已成立！請至【交換紀錄】查看")

                }.await()
                true
            } catch (e: Exception) {
                Log.e("ChatRepo", "接受提案失敗", e)
                false
            }
        }
        return false
    }

    private suspend fun updateLastMessage(matchId: String, text: String) {
        val updateData = mapOf("lastMessage" to text, "timestamp" to FieldValue.serverTimestamp())
        db.collection("matches").document(matchId).update(updateData).await()
    }

    fun getMessagesFlow(matchId: String): Flow<List<Message>> = callbackFlow {
        val registration = db.collection("matches").document(matchId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) trySend(snapshot.toObjects(Message::class.java))
            }
        awaitClose { registration.remove() }
    }
}