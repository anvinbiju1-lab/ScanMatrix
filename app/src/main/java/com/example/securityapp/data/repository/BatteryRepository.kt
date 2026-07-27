package com.example.securityapp.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.securityapp.data.db.BatteryUsageDao
import com.example.securityapp.data.db.BatteryUsageEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryStateSnapshot(
    val level: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val healthStatus: String
)

data class AppBatteryUsage(
    val packageName: String,
    val foregroundTimeMs: Long,
    val totalTimeInForeground: Long, // Often same as above, but good to differentiate
    var batteryPercentageConsumed: Float = 0f // estimated purely by time relative to total time
)

@Singleton
class BatteryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryUsageDao: BatteryUsageDao
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val gson = Gson()

    fun getCurrentBatteryState(): BatteryStateSnapshot {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val tempDecicelsius = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperature = tempDecicelsius / 10f

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0
        val healthString = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        return BatteryStateSnapshot(batteryPct, isCharging, temperature, healthString)
    }

    fun getUsageStatsForTimeframe(timeMs: Long): List<AppBatteryUsage> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - timeMs

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        
        val usageList = mutableListOf<AppBatteryUsage>()
        var totalForegroundTime = 0L

        for (stat in stats) {
            if (stat.totalTimeInForeground > 0) {
                totalForegroundTime += stat.totalTimeInForeground
                usageList.add(
                    AppBatteryUsage(
                        packageName = stat.packageName,
                        foregroundTimeMs = stat.totalTimeInForeground,
                        totalTimeInForeground = stat.totalTimeInForeground
                    )
                )
            }
        }

        // Calculate a naive relative battery percentage based on foreground time proportion
        if (totalForegroundTime > 0) {
            return usageList.map { app ->
                app.copy(
                    batteryPercentageConsumed = (app.foregroundTimeMs.toFloat() / totalForegroundTime.toFloat()) * 100f
                )
            }.sortedByDescending { it.batteryPercentageConsumed }
        }

        return usageList
    }

    suspend fun saveCurrentSnapshot() = withContext(Dispatchers.IO) {
        val currentState = getCurrentBatteryState()
        // Save the last 24h as a broad snapshot reference
        val last24hUsage = getUsageStatsForTimeframe(24 * 60 * 60 * 1000L)
        val usageJson = gson.toJson(last24hUsage)
        
        val entity = BatteryUsageEntity(
            timestamp = System.currentTimeMillis(),
            batteryLevel = currentState.level,
            isCharging = currentState.isCharging,
            temperature = currentState.temperature,
            healthStatus = currentState.healthStatus,
            appUsageJson = usageJson
        )
        batteryUsageDao.insertBatteryUsage(entity)
        
        // Keep only last 30 days to avoid bloat
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
        batteryUsageDao.deleteOldUsage(thirtyDaysAgo)
    }
    
    suspend fun getRecentHistory(): List<BatteryUsageEntity> = withContext(Dispatchers.IO) {
        batteryUsageDao.getRecentBatteryUsage(24) // e.g. last 24 snapshots
    }
}
