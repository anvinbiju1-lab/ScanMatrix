package com.example.securityapp.data.network

import com.squareup.moshi.Json

data class VirusTotalResponse(
    @Json(name = "data") val data: VtData?
)

data class VtData(
    @Json(name = "attributes") val attributes: VtAttributes?
)

data class VtAttributes(
    @Json(name = "last_analysis_stats") val lastAnalysisStats: VtStats?
)

data class VtStats(
    @Json(name = "harmless") val harmless: Int,
    @Json(name = "malicious") val malicious: Int,
    @Json(name = "suspicious") val suspicious: Int,
    @Json(name = "undetected") val undetected: Int
)
