package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.db.dao.MikroTikDao
import com.example.data.db.entity.AlertEntity
import com.example.data.db.entity.MonthlyRecordEntity
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.RouterEntity
import com.example.data.db.entity.UserUsage
import com.example.data.model.AlertSeverity
import com.example.data.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromUserStatus(status: UserStatus): String = status.name

    @TypeConverter
    fun toUserStatus(value: String): UserStatus = runCatching { UserStatus.valueOf(value) }.getOrDefault(UserStatus.ACTIVE)

    @TypeConverter
    fun fromAlertSeverity(severity: AlertSeverity): String = severity.name

    @TypeConverter
    fun toAlertSeverity(value: String): AlertSeverity = runCatching { AlertSeverity.valueOf(value) }.getOrDefault(AlertSeverity.WARNING)
}

@Database(
    entities = [
        RouterEntity::class,
        NetworkUserEntity::class,
        AlertEntity::class,
        MonthlyRecordEntity::class,
        UserUsage::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mikroTikDao(): MikroTikDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mikrotrack_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            populateInitialData(getInstance(context).mikroTikDao())
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(dao: MikroTikDao) {
            // Seed main router
            val routerId = dao.insertRouter(
                RouterEntity(
                    id = 1,
                    name = "MikroTik Core Gateway (CCR2004)",
                    ip = "192.168.88.1",
                    port = 80,
                    username = "admin",
                    password = "",
                    useSsl = false,
                    isDefault = true,
                    routerModel = "CCR2004-16G-2S+",
                    routerOsVersion = "v7.16.1",
                    uptime = "42d 11:34:20",
                    cpuLoad = 14,
                    totalRamMb = 4096,
                    freeRamMb = 3450
                )
            )

            // Seed secondary router
            dao.insertRouter(
                RouterEntity(
                    id = 2,
                    name = "Branch Hotspot Router (hEX S)",
                    ip = "192.168.10.1",
                    port = 443,
                    username = "admin",
                    password = "",
                    useSsl = true,
                    isDefault = false,
                    routerModel = "RB760iGS (hEX S)",
                    routerOsVersion = "v7.15.3",
                    uptime = "14d 07:12:05",
                    cpuLoad = 28,
                    totalRamMb = 256,
                    freeRamMb = 168
                )
            )

            // Seed initial network users with realistic data
            val initialUsers = listOf(
                NetworkUserEntity(
                    routerId = routerId,
                    name = "أحمد المنصوري (مكتب الإدارة)",
                    ipAddress = "192.168.88.101",
                    macAddress = "D4:6E:0E:12:88:A1",
                    hotspotUser = "admin_ahmed",
                    quotaGb = 80.0,
                    usedDownloadGb = 76.4,
                    usedUploadGb = 2.8,
                    maxDownloadMbps = 25.0,
                    maxUploadMbps = 5.0,
                    currentDownloadMbps = 3.4,
                    currentUploadMbps = 0.8,
                    alertThresholdPercent = 90,
                    status = UserStatus.WARNING
                ),
                NetworkUserEntity(
                    routerId = routerId,
                    name = "سارة العلي (قسم التصميم والمونتاج)",
                    ipAddress = "192.168.88.102",
                    macAddress = "BC:D0:74:9A:F3:11",
                    hotspotUser = "designer_sara",
                    quotaGb = 150.0,
                    usedDownloadGb = 148.2,
                    usedUploadGb = 6.1,
                    maxDownloadMbps = 40.0,
                    maxUploadMbps = 15.0,
                    currentDownloadMbps = 18.2,
                    currentUploadMbps = 4.1,
                    alertThresholdPercent = 85,
                    status = UserStatus.EXCEEDED
                ),
                NetworkUserEntity(
                    routerId = routerId,
                    name = "محمد الخالد (قسم المحاسبة)",
                    ipAddress = "192.168.88.103",
                    macAddress = "E0:D5:5E:2B:11:44",
                    hotspotUser = "acc_mohammed",
                    quotaGb = 40.0,
                    usedDownloadGb = 22.1,
                    usedUploadGb = 1.9,
                    maxDownloadMbps = 15.0,
                    maxUploadMbps = 3.0,
                    currentDownloadMbps = 0.4,
                    currentUploadMbps = 0.1,
                    alertThresholdPercent = 80,
                    status = UserStatus.ACTIVE
                ),
                NetworkUserEntity(
                    routerId = routerId,
                    name = "خالد الزهراني (خادم النسخ الاحتياطي)",
                    ipAddress = "192.168.88.200",
                    macAddress = "00:15:5D:89:FE:33",
                    hotspotUser = "backup_srv",
                    quotaGb = 300.0,
                    usedDownloadGb = 210.5,
                    usedUploadGb = 45.2,
                    maxDownloadMbps = 60.0,
                    maxUploadMbps = 30.0,
                    currentDownloadMbps = 0.0,
                    currentUploadMbps = 0.0,
                    alertThresholdPercent = 85,
                    status = UserStatus.ACTIVE
                ),
                NetworkUserEntity(
                    routerId = routerId,
                    name = "ريما الشمري (خدمة العملاء)",
                    ipAddress = "192.168.88.105",
                    macAddress = "F8:E4:3B:55:6A:90",
                    hotspotUser = "support_rima",
                    quotaGb = 50.0,
                    usedDownloadGb = 49.3,
                    usedUploadGb = 3.1,
                    maxDownloadMbps = 10.0,
                    maxUploadMbps = 2.0,
                    currentDownloadMbps = 0.9,
                    currentUploadMbps = 0.2,
                    alertThresholdPercent = 90,
                    status = UserStatus.THROTTLED
                ),
                NetworkUserEntity(
                    routerId = routerId,
                    name = "عمر الهاشمي (قسم المبيعات)",
                    ipAddress = "192.168.88.106",
                    macAddress = "4C:EB:D6:77:88:29",
                    hotspotUser = "sales_omar",
                    quotaGb = 60.0,
                    usedDownloadGb = 35.8,
                    usedUploadGb = 2.4,
                    maxDownloadMbps = 15.0,
                    maxUploadMbps = 4.0,
                    currentDownloadMbps = 1.8,
                    currentUploadMbps = 0.3,
                    alertThresholdPercent = 85,
                    status = UserStatus.ACTIVE
                )
            )
            dao.insertUsers(initialUsers)

            // Seed alerts for over-limit users
            dao.insertAlert(
                AlertEntity(
                    userId = 2,
                    userName = "سارة العلي (قسم التصميم والمونتاج)",
                    routerId = routerId,
                    title = "تجاوز الحد الأقصى للباقة (100%!)",
                    message = "تم استهلاك 154.3 GB من أصل 150.0 GB المسموح بها شهرياً على IP 192.168.88.102",
                    severity = AlertSeverity.CRITICAL,
                    timestamp = System.currentTimeMillis() - 25 * 60 * 1000,
                    isResolved = false
                )
            )

            dao.insertAlert(
                AlertEntity(
                    userId = 1,
                    userName = "أحمد المنصوري (مكتب الإدارة)",
                    routerId = routerId,
                    title = "تنبيه اقتراب نفاد الحصة (98%)",
                    message = "المستخدم استهلك 79.2 GB من حصته 80.0 GB المقررة.",
                    severity = AlertSeverity.WARNING,
                    timestamp = System.currentTimeMillis() - 2 * 3600 * 1000,
                    isResolved = false
                )
            )

            dao.insertAlert(
                AlertEntity(
                    userId = 5,
                    userName = "ريما الشمري (خدمة العملاء)",
                    routerId = routerId,
                    title = "تم تطبيق خنق السرعة تلقائياً",
                    message = "تم تخفيض سرعة التحميل إلى 1 Mbps نظراً لتجاوز 90% من الحصة الشهرية.",
                    severity = AlertSeverity.INFO,
                    timestamp = System.currentTimeMillis() - 5 * 3600 * 1000,
                    isResolved = true,
                    actionTaken = "تم خنق السرعة إلى 1Mbps"
                )
            )

            // Seed past monthly archive records for reporting
            val pastMonths = listOf("2026-09", "2026-08")
            val pastRecords = mutableListOf<MonthlyRecordEntity>()
            pastMonths.forEachIndexed { index, month ->
                pastRecords.add(
                    MonthlyRecordEntity(
                        monthYear = month,
                        userId = 1,
                        userName = "أحمد المنصوري",
                        ipAddress = "192.168.88.101",
                        quotaGb = 80.0,
                        usedDownloadGb = 72.5 - (index * 4),
                        usedUploadGb = 3.1,
                        exceeded = false
                    )
                )
                pastRecords.add(
                    MonthlyRecordEntity(
                        monthYear = month,
                        userId = 2,
                        userName = "سارة العلي",
                        ipAddress = "192.168.88.102",
                        quotaGb = 150.0,
                        usedDownloadGb = 158.0 + (index * 5),
                        usedUploadGb = 7.4,
                        exceeded = true
                    )
                )
                pastRecords.add(
                    MonthlyRecordEntity(
                        monthYear = month,
                        userId = 3,
                        userName = "محمد الخالد",
                        ipAddress = "192.168.88.103",
                        quotaGb = 40.0,
                        usedDownloadGb = 31.2,
                        usedUploadGb = 2.0,
                        exceeded = false
                    )
                )
                pastRecords.add(
                    MonthlyRecordEntity(
                        monthYear = month,
                        userId = 4,
                        userName = "خالد الزهراني (خادم النسخ)",
                        ipAddress = "192.168.88.200",
                        quotaGb = 300.0,
                        usedDownloadGb = 278.4,
                        usedUploadGb = 55.0,
                        exceeded = true
                    )
                )
            }
            dao.insertMonthlyRecords(pastRecords)

            // Seed initial UserUsage logs for internet consumption tracking
            val initialUsages = mutableListOf<UserUsage>()
            val dateKeys = listOf("2026-10-29", "2026-10-28", "2026-10-27", "2026-10-26")
            
            // Seed usage for user 1 (Ahmed)
            dateKeys.forEachIndexed { idx, date ->
                initialUsages.add(
                    UserUsage(
                        userId = 1,
                        routerId = routerId,
                        dateKey = date,
                        downloadMb = 3200.0 - (idx * 400.0),
                        uploadMb = 210.0,
                        sessionDurationMinutes = 240,
                        peakRateMbps = 18.5,
                        recordedTimestamp = System.currentTimeMillis() - (idx * 86400000L)
                    )
                )
            }

            // Seed usage for user 2 (Sara)
            dateKeys.forEachIndexed { idx, date ->
                initialUsages.add(
                    UserUsage(
                        userId = 2,
                        routerId = routerId,
                        dateKey = date,
                        downloadMb = 8500.0 + (idx * 600.0),
                        uploadMb = 820.0,
                        sessionDurationMinutes = 360,
                        peakRateMbps = 35.0,
                        recordedTimestamp = System.currentTimeMillis() - (idx * 86400000L)
                    )
                )
            }

            // Seed usage for user 3 (Mohammed)
            dateKeys.forEachIndexed { idx, date ->
                initialUsages.add(
                    UserUsage(
                        userId = 3,
                        routerId = routerId,
                        dateKey = date,
                        downloadMb = 1100.0 - (idx * 150.0),
                        uploadMb = 95.0,
                        sessionDurationMinutes = 180,
                        peakRateMbps = 12.0,
                        recordedTimestamp = System.currentTimeMillis() - (idx * 86400000L)
                    )
                )
            }

            dao.insertUserUsages(initialUsages)
        }
    }
}
