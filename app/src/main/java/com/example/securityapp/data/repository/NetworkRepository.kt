package com.example.securityapp.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppNetworkUsage(
    val uid: Int,
    val packageName: String,
    val wifiBytesReceived: Long,
    val wifiBytesSent: Long,
    val mobileBytesReceived: Long,
    val mobileBytesSent: Long
) {
    val totalWifi: Long get() = wifiBytesReceived + wifiBytesSent
    val totalMobile: Long get() = mobileBytesReceived + mobileBytesSent
    val totalUsage: Long get() = totalWifi + totalMobile
}

@Singleton
class NetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getNetworkUsage(startTime: Long, endTime: Long): Map<Int, AppNetworkUsage> = withContext(Dispatchers.IO) {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        
        val usageMap = mutableMapOf<Int, AppNetworkUsage>()

        // Helper to query usage
        fun queryUsage(networkType: Int, isWifi: Boolean) {
            try {
                val bucket = NetworkStats.Bucket()
                val networkStats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
                
                while (networkStats.hasNextBucket()) {
                    networkStats.getNextBucket(bucket)
                    val uid = bucket.uid
                    if (uid > 0) { // Ignore system root/kernel usage
                        val existing = usageMap[uid] ?: AppNetworkUsage(uid, "", 0L, 0L, 0L, 0L)
                        usageMap[uid] = if (isWifi) {
                            existing.copy(
                                wifiBytesReceived = existing.wifiBytesReceived + bucket.rxBytes,
                                wifiBytesSent = existing.wifiBytesSent + bucket.txBytes
                            )
                        } else {
                            existing.copy(
                                mobileBytesReceived = existing.mobileBytesReceived + bucket.rxBytes,
                                mobileBytesSent = existing.mobileBytesSent + bucket.txBytes
                            )
                        }
                    }
                }
                networkStats.close()
            } catch (e: Exception) {
                // Ignore SecurityExceptions or unsupported devices
                e.printStackTrace()
            }
        }

        queryUsage(ConnectivityManager.TYPE_WIFI, true)
        queryUsage(ConnectivityManager.TYPE_MOBILE, false)

        usageMap
    }
}
