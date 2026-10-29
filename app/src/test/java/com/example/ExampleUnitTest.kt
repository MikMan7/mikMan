package com.example

import com.example.data.db.entity.NetworkUserEntity
import com.example.data.model.MonthlyReportData
import com.example.data.model.UserReportItem
import com.example.data.model.UserStatus
import com.example.data.service.ReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testUserQuotaCalculations() {
        val user = NetworkUserEntity(
            name = "Test User",
            ipAddress = "192.168.88.50",
            quotaGb = 100.0,
            usedDownloadGb = 75.0,
            usedUploadGb = 10.0
        )

        assertEquals(85.0, user.totalUsedGb, 0.01)
        assertEquals(85.0, user.usagePercentage, 0.01)
        assertEquals(15.0, user.remainingGb, 0.01)
    }

    @Test
    fun testReportCsvGeneration() {
        val report = MonthlyReportData(
            monthYear = "2026-10",
            totalDownloadGb = 250.0,
            totalUploadGb = 30.0,
            totalQuotaGb = 500.0,
            totalUsers = 1,
            exceededUsersCount = 0,
            topUsers = listOf(
                UserReportItem(
                    id = 1,
                    name = "مكتب الإدارة",
                    ip = "192.168.88.10",
                    quotaGb = 100.0,
                    usedDownloadGb = 60.0,
                    usedUploadGb = 5.0,
                    totalUsedGb = 65.0,
                    percentageUsed = 65.0,
                    status = UserStatus.ACTIVE
                )
            ),
            dailyTrend = emptyList()
        )

        val csvArabic = ReportExporter.generateCsv(report, isArabic = true)
        assertTrue(csvArabic.contains("تقرير استهلاك بيانات شبكة ميكروتك"))
        assertTrue(csvArabic.contains("مكتب الإدارة"))
        assertTrue(csvArabic.contains("192.168.88.10"))

        val csvEnglish = ReportExporter.generateCsv(report, isArabic = false)
        assertTrue(csvEnglish.contains("MikroTik Network Data Usage Report"))
        assertTrue(csvEnglish.contains("Total Download (GB)"))
    }

    @Test
    fun testUserUsageEntityCalculations() {
        val usage = com.example.data.db.entity.UserUsage(
            userId = 1,
            routerId = 1,
            dateKey = "2026-10-29",
            downloadMb = 2048.0,
            uploadMb = 512.0,
            sessionDurationMinutes = 120,
            peakRateMbps = 25.5
        )

        assertEquals(2560.0, usage.totalMb, 0.01)
        assertEquals(2.5, usage.totalGb, 0.01)
        assertEquals(120, usage.sessionDurationMinutes)
    }

    @Test
    fun testMikroTikSimpleQueueDto() {
        val queueDto = com.example.data.service.SimpleQueueDto(
            name = "queue-ahmed",
            target = "192.168.88.101/32",
            bytes = "10485760/104857600", // 10MB up / 100MB down
            rate = "512000/2048000"
        )

        assertEquals(10485760L, queueDto.uploadBytes)
        assertEquals(104857600L, queueDto.downloadBytes)
        assertEquals(512000L, queueDto.uploadRateBps)
        assertEquals(2048000L, queueDto.downloadRateBps)
    }
}
