package com.example.securityapp.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface VirusTotalApi {
    @GET("api/v3/files/{id}")
    suspend fun getFileReport(
        @Path("id") fileHash: String,
        @Header("x-apikey") apiKey: String
    ): Response<VirusTotalResponse>
}
