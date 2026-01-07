package com.example.c2cfastpay_card.UIScreen.components

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // 上架商品
    suspend fun addProduct(product: ProductItem, imageUris: List<Uri> = emptyList()): Boolean {
        return try {
            val userId = getCurrentUserId() ?: throw Exception("尚未登入")
            val currentUser = auth.currentUser

            // 上架時抓取正確名字 (優先查 DB -> 其次 Auth -> 最後預設)
            val userName = try {
                val userSnapshot = db.collection("users").document(userId).get().await()
                val dbName = userSnapshot.getString("name")
                    ?: userSnapshot.getString("username")
                    ?: userSnapshot.getString("displayName")

                dbName ?: currentUser?.displayName ?: "賣家${userId.takeLast(4)}"
            } catch (e: Exception) {
                currentUser?.displayName ?: "賣家${userId.takeLast(4)}"
            }

            // 處理圖片 (網址不變，本地檔案上傳)
            val finalImageUrls = mutableListOf<String>()
            for (uri in imageUris.distinct()) {
                val uriString = uri.toString()
                if (uriString.startsWith("http")) {
                    finalImageUrls.add(uriString)
                } else {
                    val url = uploadImageToStorage(uri)
                    if (url.isNotEmpty()) finalImageUrls.add(url)
                }
            }
            val finalMainImage = finalImageUrls.firstOrNull() ?: ""

            // 寫入商品 (包含 ownerName)
            val newProduct = product.copy(
                imageUri = finalMainImage,
                images = finalImageUrls,
                ownerId = userId,
                ownerName = userName, // ★ 這裡寫入正確名字
                ownerEmail = currentUser?.email ?: "",
                timestamp = System.currentTimeMillis()
            )

            db.collection("products").document(newProduct.id).set(newProduct).await()
            true
        } catch (e: Exception) {
            Log.e("ProductRepo", "上架失敗", e)
            false
        }
    }

    private suspend fun uploadImageToStorage(uri: Uri): String {
        return try {
            val userId = getCurrentUserId() ?: "guest"
            val filename = "images/$userId/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(filename)
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) { "" }
    }

    // 取得所有商品
    fun getAllProducts(searchQuery: String = ""): Flow<List<ProductItem>> = callbackFlow {
        val query = db.collection("products").orderBy("timestamp", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) { trySend(emptyList()); return@addSnapshotListener }

            if (snapshot != null) {
                val products = snapshot.documents.mapNotNull { safeParseProduct(it) }
                val filtered = if (searchQuery.isNotBlank()) {
                    products.filter { it.title.contains(searchQuery, true) }
                } else {
                    products
                }
                trySend(filtered)
            }
        }
        awaitClose { registration.remove() }
    }

    // 配對卡片專用
    suspend fun getProductsForMatching(swipedIds: List<String> = emptyList()): List<ProductItem> {
        val userId = getCurrentUserId()
        return try {
            val snapshot = db.collection("products")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()

            val allProducts = snapshot.documents.mapNotNull { safeParseProduct(it) }
            // 過濾掉自己的商品 & 已滑過的
            allProducts.filter { (userId == null || it.ownerId != userId) && !swipedIds.contains(it.id) }
        } catch (e: Exception) { emptyList() }
    }

    // 我的商品
    suspend fun getMyProducts(): List<ProductItem> {
        val userId = getCurrentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("ownerId", userId)
                .get().await()
            snapshot.documents.mapNotNull { safeParseProduct(it) }
        } catch (e: Exception) { emptyList() }
    }

    // 單一商品 / 更新 / 刪除
    suspend fun getProductById(productId: String): ProductItem? {
        return try {
            val doc = db.collection("products").document(productId).get().await()
            safeParseProduct(doc)
        } catch (e: Exception) { null }
    }

    suspend fun updateProduct(product: ProductItem): Boolean {
        return try {
            db.collection("products").document(product.id).set(product).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteProduct(productId: String) {
        try { db.collection("products").document(productId).delete().await() }
        catch (e: Exception) { Log.e("Repo", "刪除失敗", e) }
    }

    private fun safeParseProduct(doc: com.google.firebase.firestore.DocumentSnapshot): ProductItem? {
        return try {
            val title = doc.getString("title") ?: "未知商品"
            val priceRaw = doc.get("price")
            val stockRaw = doc.get("stock")
            val ownerId = doc.getString("ownerId") ?: ""

            val ownerName = doc.getString("ownerName") ?: "賣家"

            val imageUri = doc.getString("imageUri") ?: doc.getString("imageUrl") ?: ""
            val description = doc.getString("description") ?: ""
            val images = doc.get("images") as? List<String> ?: emptyList()

            val priceStr = priceRaw?.toString() ?: "0"
            val stockStr = stockRaw?.toString() ?: "1"

            ProductItem(
                id = doc.id,
                title = title,
                description = description,
                price = priceStr,
                stock = stockStr,
                ownerId = ownerId,
                ownerName = ownerName,
                imageUri = imageUri,
                images = images
            )
        } catch (e: Exception) {
            Log.e("ProductRepo", "解析失敗 ID: ${doc.id}", e)
            null
        }
    }
}