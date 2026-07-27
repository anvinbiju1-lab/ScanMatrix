package com.example.securityapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BatteryUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatteryUsage(usage: BatteryUsageEntity)

    @Query("SELECT * FROM battery_usage ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBatteryUsage(limit: Int): List<BatteryUsageEntity>

    @Query("SELECT * FROM battery_usage WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getBatteryUsageSince(since: Long): List<BatteryUsageEntity>
    
    @Query("DELETE FROM battery_usage WHERE timestamp < :olderThan")
    suspend fun deleteOldUsage(olderThan: Long)
}
