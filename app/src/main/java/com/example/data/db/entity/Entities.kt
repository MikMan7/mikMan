package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AlertSeverity
import com.example.data.model.UserStatus

@Entity(tableName = "routers")
data class RouterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ip: String,
    val port: Int = 80,
    val username: String = "admin",
    val password: String = "",
    val useSsl: Boolean = false,
    val isDefault: Boolean = false,
    val routerModel: String = "RB4011iGS+5HacQ2HnD",
    val routerOsVersion: String = "7.15.2 (stable)",
    val uptime: String = "18d 04:22:15",
    val cpuLoad: Int = 12, // percentage
    val totalRamMb: Int = 1024,
    val freeRamMb: Int = 780,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "network_users")
data class NetworkUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routerId: Long = 1,
    val name: String,
    val ipAddress: String,
    val macAddress: String = "",
    val hotspotUser: String = "",
    val quotaGb: Double = 50.0,
    val usedDownloadGb: Double = 0.0,
    val usedUploadGb: Double = 0.0,
    val maxDownloadMbps: Double = 10.0,
    val maxUploadMbps: Double = 3.0,
    val currentDownloadMbps: Double = 0.0,
    val currentUploadMbps: Double = 0.0,
    val alertThresholdPercent: Int = 85, // Alert when reaches 85%
    val status: UserStatus = UserStatus.ACTIVE,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val cycleResetDay: Int = 1
) {
    val totalUsedGb: Double
        get() = usedDownloadGb + usedUploadGb

    val usagePercentage: Double
        get() = if (quotaGb > 0) ((totalUsedGb / quotaGb) * 100.0).coerceAtMost(100.0) else 0.0

    val remainingGb: Double
        get() = (quotaGb - totalUsedGb).coerceAtLeast(0.0)
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long? = null,
    val userName: String = "",
    val routerId: Long = 1,
    val title: String,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.WARNING,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val actionTaken: String = ""
)

@Entity(tableName = "monthly_records")
data class MonthlyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthYear: String, // e.g. "2026-10"
    val userId: Long,
    val userName: String,
    val ipAddress: String,
    val quotaGb: Double,
    val usedDownloadGb: Double,
    val usedUploadGb: Double,
    val exceeded: Boolean,
    val recordedTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_usage",
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["dateKey"])
    ]
)
data class UserUsage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val routerId: Long = 1,
    val dateKey: String, // e.g. "2026-10-29"
    val downloadMb: Double,
    val uploadMb: Double,
    val totalMb: Double = downloadMb + uploadMb,
    val sessionDurationMinutes: Int = 60,
    val peakRateMbps: Double = 0.0,
    val recordedTimestamp: Long = System.currentTimeMillis()
) {
    val totalGb: Double
        get() = totalMb / 1024.0
}

