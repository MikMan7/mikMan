package com.example.data.model

enum class UserStatus(val labelAr: String, val labelEn: String) {
    ACTIVE("نشط", "Active"),
    WARNING("اقترب من الحد", "Warning"),
    THROTTLED("محدد السرعة", "Throttled"),
    EXCEEDED("تجاوز الباقة", "Exceeded"),
    SUSPENDED("موقوف", "Suspended")
}

enum class AlertSeverity(val labelAr: String, val labelEn: String) {
    INFO("معلومات", "Info"),
    WARNING("تحذير", "Warning"),
    CRITICAL("حرج", "Critical")
}

enum class AppLanguage {
    ARABIC,
    ENGLISH
}

enum class NavigationTab(val titleAr: String, val titleEn: String) {
    DASHBOARD("الرئيسية", "Dashboard"),
    USERS("المستخدمين", "Users"),
    ALERTS("التنبيهات", "Alerts"),
    REPORTS("التقارير", "Reports"),
    ROUTERS("الموجّهات", "Routers")
}

data class DailyTrafficPoint(
    val day: Int,
    val downloadGb: Double,
    val uploadGb: Double
)

data class MonthlyReportData(
    val monthYear: String,
    val totalDownloadGb: Double,
    val totalUploadGb: Double,
    val totalQuotaGb: Double,
    val totalUsers: Int,
    val exceededUsersCount: Int,
    val topUsers: List<UserReportItem>,
    val dailyTrend: List<DailyTrafficPoint>
)

data class UserReportItem(
    val id: Long,
    val name: String,
    val ip: String,
    val quotaGb: Double,
    val usedDownloadGb: Double,
    val usedUploadGb: Double,
    val totalUsedGb: Double,
    val percentageUsed: Double,
    val status: UserStatus
)
