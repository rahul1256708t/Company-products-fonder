package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CompanyProductsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Cleans up the raw text returned by the LLM (removes markdown triple backticks if present)
     */
    fun cleanJsonResponse(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```")) {
            // Remove starting ```json or ```
            val firstLineEnd = clean.indexOf("\n")
            if (firstLineEnd != -1) {
                clean = clean.substring(firstLineEnd).trim()
            } else {
                clean = clean.removePrefix("```json").removePrefix("```")
            }
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3).trim()
        }
        return clean
    }

    /**
     * Fetch products for a company using Gemini API
     */
    suspend fun findCompanyProducts(companyName: String): CompanyProductsResponse? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder value.")
            throw IllegalStateException("Gemini API Key is not set. Please set it in the Secrets panel in AI Studio.")
        }

        val prompt = """
            You are a Company Product database expert. Your task is to return a complete, accurate, and realistic list of official products current as of 2026 for the requested company: "$companyName".
            Generate a JSON object matching this exact schema:
            {
              "companyName": "Exact Company Name",
              "companyDescription": "Clear, informative 2-sentence description of the company philosophy, expertise, and main industries.",
              "companyFounded": "Founded Year, e.g. 1976",
              "companyLogoUrl": "Provide a highly reliable, recognizable company logo image or favicon API URL. If the company is famous and has an official website/domain, prefer using Clearbit's logo API (e.g., 'https://logo.clearbit.com/apple.com', 'https://logo.clearbit.com/tesla.com', 'https://logo.clearbit.com/starbucks.com', 'https://logo.clearbit.com/nike.com') or a Google Favicon API URL (e.g., 'https://www.google.com/s2/favicons?sz=256&domain=apple.com'). Avoid landscape or general photos; must be a clean brand icon or transparent/white-background logo graphic.",
              "products": [
                {
                  "name": "Exact Name of Product 1",
                  "category": "Main Category (e.g. Laptops, Coffee, Furniture, Athletic Wear, Cloud Software)",
                  "description": "Short, clear description of features, target audience, and standout capabilities.",
                  "price": "Realistic average selling price or starting price, e.g. '${'$'}999' or '${'$'}1.50/lb' or 'Free' or 'Subscription-based'. Include dollar sign if USD.",
                  "priceNumeric": 999.00,
                  "productUrl": "A realistic typical official product URL (e.g. https://www.website.com/product-name). Must be well-formed.",
                  "imageUrl": "A high-quality, relevant Unsplash photo URL representing this product. Provide a real, working Unsplash photo ID, e.g., https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500 for smartphones, https://images.unsplash.com/photo-1496181130204-755241524eab?w=500 for laptops, https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500 for shoes, etc. Make sure it loads beautifully.",
                  "imageSearchQuery": "A short, highly relevant search term (e.g., 'espresso machine', 'running sneakers', 'smart speaker') to use as a fallback.",
                  "popularity": 4.8
                }
              ]
            }

            Rules:
            1. Provide at least 5 products, and up to 10 products if the company is big.
            2. The categories should map to the actual official lines (no generic category names, keep them clean).
            3. Ensure the JSON returned is well-formed, valid, and contains no trailing commas.
            4. Make sure all values are realistic and accurate for $companyName.
            5. Do NOT return any markdown decoration extra text outside the JSON block. Return ONLY the raw JSON block.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        val response = apiService.generateContent(apiKey, request)
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("No response received from Gemini API.")

        val cleanText = cleanJsonResponse(rawText)
        Log.d(TAG, "Parsed and cleaned JSON text:\n$cleanText")

        return try {
            val adapter = moshi.adapter(CompanyProductsResponse::class.java)
            adapter.fromJson(cleanText)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing Gemini JSON: ${e.message}", e)
            throw Exception("Failed to parse Gemini output into a product search list. Please retry.")
        }
    }
}
