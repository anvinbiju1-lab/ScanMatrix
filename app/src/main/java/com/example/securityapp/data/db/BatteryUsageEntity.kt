package com.example.securityapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_usage")
data class BatteryUsageEntity(
    @PrimaryKey val timestamp: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val healthStatus: String,
    // Store JSON string of top draining apps to preserve history
    val appUsageJson: String 
)
