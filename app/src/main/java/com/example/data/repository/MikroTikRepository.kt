package com.example.data.repository

import com.example.data.db.dao.MikroTikDao
import com.example.data.db.entity.AlertEntity
import com.example.data.db.entity.MonthlyRecordEntity
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.RouterEntity
import com.example.data.db.entity.UserUsage
import com.example.data.model.AlertSeverity
import com.example.data.model.DailyTrafficPoint
import com.example.data.model.MonthlyReportData
import com.example.data.model.UserReportItem
import com.example.data.model.UserStatus
import com.example.data.service.MikroTikApiService
import com.example.data.service.RouterSystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MikroTikRepository(
    private val dao: MikroTikDao,
    private val apiService: MikroTikApiService = MikroTikApiService()
) {
    val routers: Flow<List<RouterEntity>> = dao.getAllRouters()
    val users: Flow<List<NetworkUserEntity>> = dao.getAllUsers()
    val alerts: Flow<List<AlertEntity>> = dao.getAllAlerts()
    val unresolvedAlertCount: Flow<Int> = dao.getUnresolvedAlertCount()
    val allUsageRecords: Flow<List<UserUsage>> = dao.getAllUsageRecords()

    fun getUsageForUser(userId: Long): Flow<List<UserUsage>> = dao.getUsageByUserId(userId)

    suspend fun recordUserUsage(
        userId: Long,
        routerId: Long,
        downloadMb: Double,
        uploadMb: Double,
        sessionMinutes: Int = 60,
        peakRateMbps: Double = 0.0
    ) {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        dao.insertUserUsage(
            UserUsage(
                userId = userId,
                routerId = routerId,
                dateKey = todayKey,
                downloadMb = downloadMb,
                uploadMb = uploadMb,
                sessionDurationMinutes = sessionMinutes,
                peakRateMbps = peakRateMbps,
                recordedTimestamp = System.currentTimeMillis()
            )
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isSimulating = true

    init {
        // Start background traffic updater & quota watcher
        scope.launch {
            while (isActive) {
                delay(3000)
                if (isSimulating) {
                    simulateNetworkTrafficTick()
                }
            }
        }
    }

    fun setSimulationEnabled(enabled: Boolean) {
        isSimulating = enabled
    }

    private suspend fun simulateNetworkTrafficTick() {
        val currentUsers = users.firstOrNull() ?: return
        if (currentUsers.isEmpty()) return

        val updatedList = currentUsers.map { user ->
            // Random variation of active download and upload speed
            val activeActivity = Random.nextBoolean()
            val newDownSpeed = if (activeActivity && user.status != UserStatus.SUSPENDED) {
                val maxLimit = if (user.status == UserStatus.THROTTLED) 1.0 else user.maxDownloadMbps
                (Random.nextDouble(0.1, maxLimit)).coerceAtLeast(0.0)
            } else 0.0

            val newUpSpeed = if (activeActivity && user.status != UserStatus.SUSPENDED) {
                val maxLimit = if (user.status == UserStatus.THROTTLED) 0.5 else user.maxUploadMbps
                (Random.nextDouble(0.05, maxLimit)).coerceAtLeast(0.0)
            } else 0.0

            // Accumulate a small amount of MB in data consumption
            val addedDownGb = (newDownSpeed * 3.0 / 8000.0) // 3 seconds in GB
            val addedUpGb = (newUpSpeed * 3.0 / 8000.0)

            val newUsedDown = user.usedDownloadGb + addedDownGb
            val newUsedUp = user.usedUploadGb + addedUpGb
            val totalUsed = newUsedDown + newUsedUp
            val percentage = if (user.quotaGb > 0) (totalUsed / user.quotaGb) * 100.0 else 0.0

            var newStatus = user.status
            if (user.status != UserStatus.SUSPENDED && user.status != UserStatus.THROTTLED) {
                if (percentage >= 100.0) {
                    newStatus = UserStatus.EXCEEDED
                    checkAndTriggerAlert(
                        userId = user.id,
                        userName = user.name,
                        routerId = user.routerId,
                        title = "تجاوز الحد الأقصى للباقة (100%!)",
                        message = "تم استهلاك %.2f GB من أصل %.2f GB لـ %s على %s".format(
                            Locale.US, totalUsed, user.quotaGb, user.name, user.ipAddress
                        ),
                        severity = AlertSeverity.CRITICAL
                    )
                } else if (percentage >= user.alertThresholdPercent.toDouble()) {
                    newStatus = UserStatus.WARNING
                    checkAndTriggerAlert(
                        userId = user.id,
                        userName = user.name,
                        routerId = user.routerId,
                        title = "تنبيه اقتراب نفاد الحصة (${user.alertThresholdPercent}%)",
                        message = "استهلك المشترك %s نسبة %.1f%% من رصيده الشهري.".format(
                            Locale.US, user.name, percentage
                        ),
                        severity = AlertSeverity.WARNING
                    )
                }
            }

            user.copy(
                currentDownloadMbps = (newDownSpeed * 10).toInt() / 10.0,
                currentUploadMbps = (newUpSpeed * 10).toInt() / 10.0,
                usedDownloadGb = (newUsedDown * 100).toInt() / 100.0,
                usedUploadGb = (newUsedUp * 100).toInt() / 100.0,
                status = newStatus,
                lastActiveTimestamp = System.currentTimeMillis()
            )
        }

        updatedList.forEach { dao.updateUser(it) }

        // Update default router CPU and metrics slightly for realism
        val defaultRouter = dao.getDefaultRouter()
        if (defaultRouter != null) {
            val totalSpeed = updatedList.sumOf { it.currentDownloadMbps + it.currentUploadMbps }
            val cpu = (10 + (totalSpeed * 0.8)).toInt().coerceIn(8, 92)
            dao.updateRouter(defaultRouter.copy(cpuLoad = cpu, lastSyncTimestamp = System.currentTimeMillis()))
        }
    }

    private suspend fun checkAndTriggerAlert(
        userId: Long,
        userName: String,
        routerId: Long,
        title: String,
        message: String,
        severity: AlertSeverity
    ) {
        val existing = dao.getUnresolvedAlerts().firstOrNull() ?: emptyList()
        val alreadyAlerted = existing.any { it.userId == userId && it.severity == severity }
        if (!alreadyAlerted) {
            dao.insertAlert(
                AlertEntity(
                    userId = userId,
                    userName = userName,
                    routerId = routerId,
                    title = title,
                    message = message,
                    severity = severity,
                    timestamp = System.currentTimeMillis(),
                    isResolved = false
                )
            )
        }
    }

    suspend fun testRouterConnection(router: RouterEntity): Result<RouterSystemInfo> {
        return apiService.testConnection(
            ip = router.ip,
            port = router.port,
            username = router.username,
            password = router.password,
            useSsl = router.useSsl
        )
    }

    suspend fun saveRouter(router: RouterEntity): Long {
        return if (router.id == 0L) {
            dao.insertRouter(router)
        } else {
            dao.updateRouter(router)
            router.id
        }
    }

    suspend fun deleteRouter(router: RouterEntity) {
        dao.deleteRouter(router)
    }

    suspend fun saveUser(user: NetworkUserEntity): Long {
        return if (user.id == 0L) {
            dao.insertUser(user)
        } else {
            dao.updateUser(user)
            user.id
        }
    }

    suspend fun deleteUser(user: NetworkUserEntity) {
        dao.deleteUser(user)
    }

    suspend fun resetUserQuota(userId: Long) {
        dao.resetUserQuota(userId)
        // Auto resolve unresolved alerts for this user
        val userAlerts = dao.getUnresolvedAlerts().firstOrNull()?.filter { it.userId == userId } ?: emptyList()
        userAlerts.forEach { dao.resolveAlert(it.id, "تم إعادة تعيين العداد لصفر") }
    }

    suspend fun addExtraQuota(userId: Long, extraGb: Double) {
        dao.addExtraQuota(userId, extraGb)
        val user = dao.getUserById(userId)
        if (user != null) {
            val total = user.totalUsedGb
            val newQuota = user.quotaGb + extraGb
            val percentage = (total / newQuota) * 100.0
            val status = if (percentage < user.alertThresholdPercent) UserStatus.ACTIVE else UserStatus.WARNING
            dao.updateUser(user.copy(quotaGb = newQuota, status = status))
        }
        val userAlerts = dao.getUnresolvedAlerts().firstOrNull()?.filter { it.userId == userId } ?: emptyList()
        userAlerts.forEach { dao.resolveAlert(it.id, "تمت إضافة رصيد إضافي +${extraGb}GB") }
    }

    suspend fun throttleUser(userId: Long, throttleSpeedMbps: Double = 1.0) {
        val user = dao.getUserById(userId) ?: return
        dao.updateUser(
            user.copy(
                status = UserStatus.THROTTLED,
                maxDownloadMbps = throttleSpeedMbps,
                maxUploadMbps = (throttleSpeedMbps / 2.0).coerceAtLeast(0.5)
            )
        )
        val userAlerts = dao.getUnresolvedAlerts().firstOrNull()?.filter { it.userId == userId } ?: emptyList()
        userAlerts.forEach { dao.resolveAlert(it.id, "تم تطبيق خنق السرعة إلى $throttleSpeedMbps Mbps") }
    }

    suspend fun toggleSuspendUser(userId: Long) {
        val user = dao.getUserById(userId) ?: return
        val newStatus = if (user.status == UserStatus.SUSPENDED) UserStatus.ACTIVE else UserStatus.SUSPENDED
        dao.updateUser(user.copy(status = newStatus, currentDownloadMbps = 0.0, currentUploadMbps = 0.0))
    }

    suspend fun resolveAlert(alertId: Long, action: String) {
        dao.resolveAlert(alertId, action)
    }

    suspend fun clearAllAlerts() {
        dao.clearAllAlerts()
    }

    suspend fun getMonthlyReport(monthYear: String): MonthlyReportData {
        val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        if (monthYear == currentMonthKey) {
            // Live current month calculation from active users
            val currentUsers = users.firstOrNull() ?: emptyList()
            val totalDown = currentUsers.sumOf { it.usedDownloadGb }
            val totalUp = currentUsers.sumOf { it.usedUploadGb }
            val totalQuota = currentUsers.sumOf { it.quotaGb }
            val exceededCount = currentUsers.count { it.usagePercentage >= 100.0 }

            val topList = currentUsers
                .sortedByDescending { it.totalUsedGb }
                .map {
                    UserReportItem(
                        id = it.id,
                        name = it.name,
                        ip = it.ipAddress,
                        quotaGb = it.quotaGb,
                        usedDownloadGb = it.usedDownloadGb,
                        usedUploadGb = it.usedUploadGb,
                        totalUsedGb = it.totalUsedGb,
                        percentageUsed = it.usagePercentage,
                        status = it.status
                    )
                }

            // Generate realistic daily trend points for this month
            val dailyTrend = (1..28).map { day ->
                val factor = (0.7 + (day % 7) * 0.1)
                DailyTrafficPoint(
                    day = day,
                    downloadGb = ((totalDown / 28.0) * factor * 10).toInt() / 10.0,
                    uploadGb = ((totalUp / 28.0) * factor * 10).toInt() / 10.0
                )
            }

            return MonthlyReportData(
                monthYear = monthYear,
                totalDownloadGb = totalDown,
                totalUploadGb = totalUp,
                totalQuotaGb = totalQuota,
                totalUsers = currentUsers.size,
                exceededUsersCount = exceededCount,
                topUsers = topList,
                dailyTrend = dailyTrend
            )
        } else {
            // Query past monthly records
            val records = dao.getMonthlyRecords(monthYear).firstOrNull() ?: emptyList()
            val totalDown = records.sumOf { it.usedDownloadGb }
            val totalUp = records.sumOf { it.usedUploadGb }
            val totalQuota = records.sumOf { it.quotaGb }
            val exceededCount = records.count { it.exceeded }

            val topList = records.map {
                val totalUsed = it.usedDownloadGb + it.usedUploadGb
                val pct = if (it.quotaGb > 0) (totalUsed / it.quotaGb) * 100.0 else 0.0
                UserReportItem(
                    id = it.userId,
                    name = it.userName,
                    ip = it.ipAddress,
                    quotaGb = it.quotaGb,
                    usedDownloadGb = it.usedDownloadGb,
                    usedUploadGb = it.usedUploadGb,
                    totalUsedGb = totalUsed,
                    percentageUsed = pct,
                    status = if (it.exceeded) UserStatus.EXCEEDED else UserStatus.ACTIVE
                )
            }

            val dailyTrend = (1..30).map { day ->
                val factor = (0.6 + (day % 5) * 0.15)
                DailyTrafficPoint(
                    day = day,
                    downloadGb = ((totalDown / 30.0) * factor * 10).toInt() / 10.0,
                    uploadGb = ((totalUp / 30.0) * factor * 10).toInt() / 10.0
                )
            }

            return MonthlyReportData(
                monthYear = monthYear,
                totalDownloadGb = totalDown,
                totalUploadGb = totalUp,
                totalQuotaGb = totalQuota,
                totalUsers = records.size,
                exceededUsersCount = exceededCount,
                topUsers = topList,
                dailyTrend = dailyTrend
            )
        }
    }
}
