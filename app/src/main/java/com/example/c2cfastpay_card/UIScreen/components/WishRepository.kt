package com.example.c2cfastpay_card.UIScreen.components

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.c2cfastpay_card.UIScreen.components.WishItem
import com.example.c2cfastpay_card.data.User // 引用 User 資料模型
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage // 新增
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WishRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance() // 新增 Storage 實體
    private val wishCollection = db.collection("wishes")

    // 取得當前用戶 ID
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // 發布願望
    suspend fun addWish(wish: WishItem, imageUri: Uri?) {
        val userId = getCurrentUserId() ?: throw Exception("尚未登入")

        // 取得使用者資料
        val userName = try {
            val userSnapshot = db.collection("users").document(userId).get().await()
            val user = userSnapshot.toObject(User::class.java)
            user?.name ?: "匿名許願者"
        } catch (e: Exception) {
            "匿名許願者"
        }
        val userEmail = auth.currentUser?.email ?: ""

        // 處理圖片上傳
        var finalImageUrl = ""
        if (imageUri != null) {
            finalImageUrl = uploadImageToStorage(imageUri)
        }

        // 準備寫入資料
        val newWish = wish.copy(
            uuid = UUID.randomUUID().toString(), // 確保有唯一 ID
            imageUri = finalImageUrl,
            ownerId = userId,
            ownerName = userName,
            ownerEmail = userEmail,
            timestamp = System.currentTimeMillis()
        )

        // 寫入 Firestore
        wishCollection.document(newWish.uuid).set(newWish).await()

        Log.d("WishRepository", "許願成功: ${newWish.title}")
    }

    // 上傳圖片到 Firebase Storage
    private suspend fun uploadImageToStorage(uri: Uri): String {
        return try {
            val userId = getCurrentUserId() ?: "guest"
            // 存放在 wishes/userId/ 資料夾下
            val filename = "wishes/$userId/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(filename)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("WishRepository", "圖片上傳失敗", e)
            ""
        }
    }

    suspend fun deleteWish(wishId: String) {
        try {
            db.collection("wishes").document(wishId).delete().await()
        } catch (e: Exception) {
            Log.e("WishRepo", "刪除願望失敗", e)
            throw e // 拋出異常讓 UI 層處理
        }
    }

    // 取得願望列表 (即時監聽 + 搜尋過濾)
    fun getWishListFlow(searchQuery: String = ""): Flow<List<WishItem>> = callbackFlow {
        // 抓取所有願望，並依時間排序
        val query = wishCollection.orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot: QuerySnapshot?, e: FirebaseFirestoreException? ->
            if (e != null) {
                Log.w("WishRepository", "Listen failed.", e)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val allWishes = snapshot.toObjects(WishItem::class.java)

                // 過濾
                if (searchQuery.isBlank()) {
                    trySend(allWishes)
                } else {
                    val filteredList = allWishes.filter { item ->
                        // 比對標題 OR 描述
                        item.title.contains(searchQuery, ignoreCase = true) ||
                                item.description.contains(searchQuery, ignoreCase = true)
                    }
                    trySend(filteredList)
                }
            }
        }

        awaitClose { listener.remove() }
    }

    // 根據 UUID 取得單一願望
    suspend fun getWishByUuid(uuid: String): WishItem? {
        return try {
            val snapshot = wishCollection.document(uuid).get().await()
            // 如果用 document ID 存，直接轉物件即可
            if (snapshot.exists()) {
                snapshot.toObject(WishItem::class.java)
            } else {
                // 如果 document ID 不是 uuid，則用 query 查
                val querySnapshot = wishCollection.whereEqualTo("uuid", uuid).get().await()
                if (!querySnapshot.isEmpty) querySnapshot.documents.first().toObject(WishItem::class.java) else null
            }
        } catch (e: Exception) {
            Log.e("WishRepository", "Get wish failed", e)
            null
        }
    }

    // 取得單一願望
    suspend fun getWishById(wishId: String): WishItem? {
        return try {
            val snapshot = db.collection("wishes").document(wishId).get().await()
            snapshot.toObject(WishItem::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // 更新願望
    suspend fun updateWish(wish: WishItem) {
        if (wish.uuid.isEmpty()) return 
        db.collection("wishes").document(wish.uuid) 
            .set(wish)
            .await()
    }
}