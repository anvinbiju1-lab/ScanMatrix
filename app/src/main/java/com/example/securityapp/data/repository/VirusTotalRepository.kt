package com.example.securityapp.data.repository

import com.example.securityapp.data.network.VirusTotalApi
import com.example.securityapp.data.network.VirusTotalResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VirusTotalRepository @Inject constructor() {

    private val apiKey = "38eda5e5923921db9593b0adfcc16a112f9c1e4c8d4f6898c513dbff73ed87a2"
    
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.virustotal.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(VirusTotalApi::class.java)

    // A memory cache to prevent re-querying the same hash and eating up API quota
    private val resultCache = mutableMapOf<String, VirusTotalResult>()

    suspend fun checkFileHash(hash: String): VirusTotalResult {
        if (resultCache.containsKey(hash)) {
            return resultCache[hash]!!
        }

        return try {
            val response = api.getFileReport(hash, apiKey)
            if (response.isSuccessful) {
                val stats = response.body()?.data?.attributes?.lastAnalysisStats
                if (stats != null) {
                    val isMalicious = stats.malicious > 0 || stats.suspicious > 0
                    val result = VirusTotalResult.Scanned(
                        isMalicious = isMalicious,
                        maliciousVotes = stats.malicious,
                        suspiciousVotes = stats.suspicious
                    )
                    resultCache[hash] = result
                    result
                } else {
                    VirusTotalResult.Error("Invalid response format")
                }
            } else if (response.code() == 404) {
                // Hash not found in VT database, consider it unknown/safe for now
                val result = VirusTotalResult.NotFound
                resultCache[hash] = result
                result
            } else if (response.code() == 429 || response.code() == 401) {
                VirusTotalResult.RateLimited(response.code())
            } else {
                VirusTotalResult.Error("HTTP Error: ${response.code()}")
            }
        } catch (e: Exception) {
            VirusTotalResult.Error(e.message ?: "Unknown network error")
        }
    }
}

sealed class VirusTotalResult {
    data class Scanned(val isMalicious: Boolean, val maliciousVotes: Int, val suspiciousVotes: Int) : VirusTotalResult()
    object NotFound : VirusTotalResult()
    data class RateLimited(val code: Int) : VirusTotalResult()
    data class Error(val message: String) : VirusTotalResult()
}
