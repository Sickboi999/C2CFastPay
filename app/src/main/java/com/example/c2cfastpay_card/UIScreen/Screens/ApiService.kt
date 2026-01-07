package com.example.c2cfastpay_card.UIScreen.Screens

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 資料模型 (Data Models)
data class QueryRequest(val query: String)
data class Product(
    val 商品ID: String,
    val 語意文案: String,
    val 相似度: Float
)
data class RagQueryResponse(
    val rewritten_query: String,
    val products: List<Product>,
    val attributes: List<String>
)
data class GenDescRequest(val attributes: Map<String, String>)
data class GenDescResponse(val description: String)
data class GenFollowupRequest(
    val story_type: String,
    val product_name: String,
    val user_answer: String
)
data class GenFollowupResponse(val followup_question: String)
data class StoryQaItem(
    val 主題: String,
    val 主問題: String,
    val 主回答: String,
    val 延伸問: String,
    val 延伸答: String
)
data class GenStoryRequest(
    val product_name: String,
    val story_qa: List<StoryQaItem>
)
data class GenStoryResponse(val story: String)


// API 服務介面 (Retrofit Interface)
interface C2CFastPayApi {
    @POST("rag-query")
    suspend fun ragQuery(@Body req: QueryRequest): RagQueryResponse
    @POST("gen-desc")
    suspend fun generateDescription(@Body req: GenDescRequest): GenDescResponse
    @POST("gen-followup")
    suspend fun generateFollowupQuestion(@Body req: GenFollowupRequest): GenFollowupResponse
    @POST("gen-story")
    suspend fun generateStory(@Body req: GenStoryRequest): GenStoryResponse
}

// Retrofit 客戶端實例 (Singleton)
object RetrofitClient {
    private const val BASE_URL = "https://c2cfastpay-api-632365191955.asia-east1.run.app/"

    val apiService: C2CFastPayApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(C2CFastPayApi::class.java)
    }
}