package com.example.c2cfastpay_card.UIScreen.components

import android.util.Log
import com.example.c2cfastpay_card.data.NotificationItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 即時監聽列表，已讀狀態變更時，UI 會自動更新
    fun getNotificationsFlow(): Flow<List<NotificationItem>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("NotifRepo", "監聽列表失敗", e)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(NotificationItem::class.java) ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    // 監聽未讀數量
    fun getUnreadCountFlow(): Flow<Int> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val registration = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val count = snapshot?.size() ?: 0
                trySend(count)
            }
        awaitClose { registration.remove() }
    }

    // 單則已讀
    suspend fun markSingleAsRead(notificationId: String) {
        if (notificationId.isEmpty()) return
        try {
            db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e("NotifRepo", "標記單則已讀失敗", e)
        }
    }

    // 全部已讀 
    suspend fun markAllAsRead() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val batch = db.batch()
            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            for (doc in snapshot.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 發送通知
    suspend fun sendNotification(notification: NotificationItem) {
        db.collection("notifications").document(notification.id).set(notification).await()
    }
}