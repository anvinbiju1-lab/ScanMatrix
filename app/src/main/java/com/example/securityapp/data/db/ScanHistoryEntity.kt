package com.example.securityapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.securityapp.data.repository.RiskLevel

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String, // "QUICK" or "FULL"
    val totalAppsScanned: Int,
    val totalFilesScanned: Int,
    val highRiskFound: Int,
    val mediumRiskFound: Int,
    val lowRiskFound: Int
)
