package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.AlertEntity
import com.example.data.db.entity.MonthlyRecordEntity
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.RouterEntity
import com.example.data.db.entity.UserUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface MikroTikDao {
    // Routers
    @Query("SELECT * FROM routers ORDER BY id ASC")
    fun getAllRouters(): Flow<List<RouterEntity>>

    @Query("SELECT * FROM routers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultRouter(): RouterEntity?

    @Query("SELECT * FROM routers WHERE id = :id")
    suspend fun getRouterById(id: Long): RouterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: RouterEntity): Long

    @Update
    suspend fun updateRouter(router: RouterEntity)

    @Delete
    suspend fun deleteRouter(router: RouterEntity)

    // Network Users
    @Query("SELECT * FROM network_users ORDER BY (usedDownloadGb + usedUploadGb) DESC")
    fun getAllUsers(): Flow<List<NetworkUserEntity>>

    @Query("SELECT * FROM network_users WHERE routerId = :routerId ORDER BY (usedDownloadGb + usedUploadGb) DESC")
    fun getUsersByRouterId(routerId: Long): Flow<List<NetworkUserEntity>>

    @Query("SELECT * FROM network_users WHERE id = :id")
    suspend fun getUserById(id: Long): NetworkUserEntity?

    @Query("SELECT * FROM network_users WHERE ipAddress = :ip LIMIT 1")
    suspend fun getUserByIp(ip: String): NetworkUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: NetworkUserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<NetworkUserEntity>)

    @Update
    suspend fun updateUser(user: NetworkUserEntity)

    @Delete
    suspend fun deleteUser(user: NetworkUserEntity)

    @Query("UPDATE network_users SET usedDownloadGb = 0.0, usedUploadGb = 0.0, status = 'ACTIVE' WHERE id = :userId")
    suspend fun resetUserQuota(userId: Long)

    @Query("UPDATE network_users SET quotaGb = quotaGb + :extraGb WHERE id = :userId")
    suspend fun addExtraQuota(userId: Long, extraGb: Double)

    // Alerts
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT COUNT(*) FROM alerts WHERE isResolved = 0")
    fun getUnresolvedAlertCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isResolved = 1, actionTaken = :action WHERE id = :id")
    suspend fun resolveAlert(id: Long, action: String)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()

    // Monthly Records
    @Query("SELECT * FROM monthly_records WHERE monthYear = :monthYear ORDER BY (usedDownloadGb + usedUploadGb) DESC")
    fun getMonthlyRecords(monthYear: String): Flow<List<MonthlyRecordEntity>>

    @Query("SELECT DISTINCT monthYear FROM monthly_records ORDER BY monthYear DESC")
    fun getAvailableMonths(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyRecords(records: List<MonthlyRecordEntity>)

    // UserUsage Operations
    @Query("SELECT * FROM user_usage WHERE userId = :userId ORDER BY recordedTimestamp DESC")
    fun getUsageByUserId(userId: Long): Flow<List<UserUsage>>

    @Query("SELECT * FROM user_usage WHERE dateKey = :dateKey ORDER BY totalMb DESC")
    fun getUsageByDate(dateKey: String): Flow<List<UserUsage>>

    @Query("SELECT * FROM user_usage ORDER BY recordedTimestamp DESC LIMIT 100")
    fun getAllUsageRecords(): Flow<List<UserUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserUsage(usage: UserUsage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserUsages(usages: List<UserUsage>)

    @Query("SELECT SUM(totalMb) FROM user_usage WHERE userId = :userId")
    suspend fun getTotalMbForUser(userId: Long): Double?

    @Query("DELETE FROM user_usage WHERE userId = :userId")
    suspend fun deleteUsageForUser(userId: Long)
}
