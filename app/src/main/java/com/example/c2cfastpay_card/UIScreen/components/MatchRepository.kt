package com.example.c2cfastpay_card.UIScreen.components

import android.content.Context
import android.util.Log
import com.example.c2cfastpay_card.data.Like
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class SwipeDirection { LEFT, RIGHT }

data class SwipeRecord(
    val userId: String = "",
    val productId: String = "",
    val direction: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class MatchRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getMatchId(userA: String, userB: String): String {
        return if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
    }

    // 記錄滑動
    suspend fun recordSwipe(productId: String, direction: SwipeDirection) {
        val myId = getCurrentUserId() ?: return
        val swipeData = SwipeRecord(userId = myId, productId = productId, direction = direction.name)
        val docId = "${myId}_${productId}"

        try {
            db.collection("swipes").document(docId).set(swipeData).await()
        } catch (e: Exception) {
            Log.e("MatchRepository", "記錄滑動失敗", e)
        }
    }

    suspend fun getSwipedProductIds(): List<String> {
        val myId = getCurrentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("swipes").whereEqualTo("userId", myId).get().await()
            snapshot.documents.mapNotNull { it.getString("productId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 右滑喜歡
    suspend fun likeProduct(targetProduct: ProductItem): Boolean {
        val myId = getCurrentUserId() ?: return false
        val otherId = targetProduct.ownerId

        if (otherId.isBlank() || myId == otherId) return false

        Log.d("MatchRepo", "Like: 我($myId) -> 商品(${targetProduct.id})")

        try {
            // 儲存 Like
            val likeId = "${myId}_${targetProduct.id}"
            val likeData = Like(
                id = likeId,
                likerId = myId,
                productId = targetProduct.id,
                productOwnerId = otherId
            )
            db.collection("likes").document(likeId).set(likeData).await()
            recordSwipe(targetProduct.id, SwipeDirection.RIGHT)

            // 準備商品資料快照
            // 注意：price 轉 Double 防止崩潰
            val priceVal = targetProduct.price.toString().toDoubleOrNull() ?: 0.0

            val productSummary = mapOf(
                "id" to targetProduct.id,
                "title" to targetProduct.title,
                "imageUrl" to targetProduct.imageUri,
                "ownerId" to otherId,
                "price" to priceVal
            )

            // 檢查是否已有聊天室
            val matchId = getMatchId(myId, otherId)
            val matchRef = db.collection("matches").document(matchId)
            val matchDoc = matchRef.get().await()

            if (matchDoc.exists()) {
                // 聊天室已存在 -> 追加商品
                Log.d("MatchRepo", "聊天室已存在，追加商品")
                val user1Id = matchDoc.getString("user1Id")
                val fieldToUpdate = if (user1Id == myId) "user1LikedProductIds" else "user2LikedProductIds"

                db.runTransaction { transaction ->
                    transaction.update(matchRef, fieldToUpdate, FieldValue.arrayUnion(targetProduct.id))
                    // 更新 Map 裡的商品詳情
                    transaction.update(matchRef, "productsInfo.${targetProduct.id}", productSummary)
                    transaction.update(matchRef, "lastMessage", "對另一件商品感興趣：${targetProduct.title}")
                    transaction.update(matchRef, "timestamp", FieldValue.serverTimestamp())
                }.await()

                return true

            } else {
                // 聊天室不存在 -> 檢查對方是否喜歡過我 (Reverse Like)
                val reverseLikes = db.collection("likes")
                    .whereEqualTo("likerId", otherId)
                    .whereEqualTo("productOwnerId", myId)
                    .limit(1)
                    .get()
                    .await()

                if (!reverseLikes.isEmpty) {
                    Log.d("MatchRepo", "配對成功！建立新聊天室")

                    val theirLikeDoc = reverseLikes.documents.first()
                    val myProductIdTheyLiked = theirLikeDoc.getString("productId") ?: ""

                    // 嘗試抓取我方商品的詳細資料
                    var myProductSummary = mapOf<String, Any>("id" to myProductIdTheyLiked)
                    if (myProductIdTheyLiked.isNotEmpty()) {
                        val pDoc = db.collection("products").document(myProductIdTheyLiked).get().await()
                        if (pDoc.exists()) {
                            val pPrice = pDoc.get("price")?.toString()?.toDoubleOrNull() ?: 0.0
                            myProductSummary = mapOf(
                                "id" to pDoc.id,
                                "title" to (pDoc.getString("title") ?: "我的商品"),
                                "imageUrl" to (pDoc.getString("imageUri") ?: ""),
                                "ownerId" to myId,
                                "price" to pPrice
                            )
                        }
                    }

                    createInitialMatch(myId, otherId, targetProduct.id, productSummary, myProductIdTheyLiked, myProductSummary)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("MatchRepo", "Like 處理失敗", e)
        }
        return false
    }

    private suspend fun createInitialMatch(
        myId: String,
        otherId: String,
        targetProductId: String,
        targetProductData: Map<String, Any>,
        myProductId: String,
        myProductData: Map<String, Any>
    ) {
        val matchId = getMatchId(myId, otherId)
        val (u1, u2) = if (myId < otherId) myId to otherId else otherId to myId

        val u1Likes = if (u1 == myId) listOf(targetProductId) else listOf(myProductId)
        val u2Likes = if (u2 == myId) listOf(targetProductId) else listOf(myProductId)

        val matchData = hashMapOf(
            "id" to matchId,
            "users" to listOf(u1, u2),
            "user1Id" to u1,
            "user2Id" to u2,
            "user1LikedProductIds" to u1Likes,
            "user2LikedProductIds" to u2Likes,
            "productsInfo" to mapOf(
                targetProductId to targetProductData,
                myProductId to myProductData
            ),
            "lastMessage" to "配對成功！開始協商吧",
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("matches").document(matchId).set(matchData).await()
    }

    // 組裝列表顯示資料 (暱稱 + 商品清單) 
    suspend fun getMatches(): List<MatchItem> {
        val myId = getCurrentUserId() ?: return emptyList()
        try {
            val snapshot = db.collection("matches")
                .whereArrayContains("users", myId)
                .get()
                .await()

            Log.d("MatchRepo", "GetMatches: 找到 ${snapshot.size()} 筆")

            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val type = doc.getString("type") ?: "SWAP"

                // 抓取對方 ID
                val users = data["users"] as? List<String> ?: emptyList()
                val otherUserId = users.find { it != myId } ?: return@mapNotNull null

                // 抓取對方的名字與頭像
                var otherName = doc.getString("otherUserName") ?: "對方"
                var otherAvatar = "" // 預設空字串

                try {
                    val userDoc = db.collection("users").document(otherUserId).get().await()
                    val realName = userDoc.getString("name")
                        ?: userDoc.getString("displayName")
                        ?: userDoc.getString("username")
                    if (realName != null) otherName = realName

                    // 抓取頭像
                    otherAvatar = userDoc.getString("avatarUrl")
                        ?: userDoc.getString("photoUrl")
                                ?: ""
                } catch (e: Exception) {
                    // 失敗維持預設
                }

                // 組裝資料
                if (type == "NORMAL") {
                    val imageUrl = doc.getString("productImage") ?: ""
                    MatchItem(
                        id = doc.id,
                        otherUserId = otherUserId,
                        otherUserName = otherName,
                        otherUserAvatar = otherAvatar, // ★ 填入抓到的頭像
                        myLikedItems = emptyList(),
                        productImageUrl = imageUrl,
                        timestamp = doc.getLong("updatedAt") ?: 0L,
                        type = "NORMAL"
                    )
                } else {
                    // SWAP 邏輯
                    val user1Id = data["user1Id"] as? String ?: ""
                    val productsInfo = data["productsInfo"] as? Map<String, Map<String, Any>> ?: emptyMap()

                    val myLikedIds = if (user1Id == myId)
                        data["user1LikedProductIds"] as? List<String> ?: emptyList()
                    else
                        data["user2LikedProductIds"] as? List<String> ?: emptyList()

                    val myLikedTitles = myLikedIds.reversed().take(3).mapNotNull { pid ->
                        val info = productsInfo[pid]
                        info?.get("title") as? String
                    }

                    val lastLikedId = myLikedIds.lastOrNull()
                    val lastProductInfo = productsInfo[lastLikedId]
                    val imageUrl = lastProductInfo?.get("imageUrl") as? String
                        ?: lastProductInfo?.get("imageUri") as? String ?: ""

                    MatchItem(
                        id = doc.id,
                        otherUserId = otherUserId,
                        otherUserName = otherName,
                        otherUserAvatar = otherAvatar, // ★ 填入抓到的頭像
                        myLikedItems = myLikedTitles,
                        productImageUrl = imageUrl,
                        timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L,
                        type = "SWAP"
                    )
                }
            }
            return items.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("MatchRepo", "Get matches error", e)
            return emptyList()
        }
    }
}