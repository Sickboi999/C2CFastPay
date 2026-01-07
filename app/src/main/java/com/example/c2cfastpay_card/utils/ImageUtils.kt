package com.example.c2cfastpay_card.utils 

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun saveImageToInternalStorage(context: Context, uri: Uri): String {
    return try {
        // 開啟選擇圖片的輸入流
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""

        // 在 App 的內部儲存空間 (filesDir) 建立一個獨一無二的檔案名稱
        val fileName = "product_image_${UUID.randomUUID()}.jpg"
        // 使用 filesDir 而不是 cacheDir，確保檔案不會被系統隨意清除
        val outputFile = File(context.filesDir, fileName)

        // 使用 FileOutputStream 將輸入流寫入到新檔案
        FileOutputStream(outputFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        // 關閉輸入流
        inputStream.close()

        // 回傳新檔案的 URI 字串
        outputFile.toUri().toString()

    } catch (e: Exception) {
        // 如果複製過程中出錯，記錄錯誤並回傳空字串
        Log.e("ImageSaveError", "Failed to save image to internal storage: ${e.message}")
        ""
    }
}