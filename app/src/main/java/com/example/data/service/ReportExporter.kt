package com.example.data.service

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.MonthlyReportData
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReportExporter {

    fun generateCsv(report: MonthlyReportData, isArabic: Boolean): String {
        val sb = StringBuilder()
        if (isArabic) {
            sb.appendLine("تقرير استهلاك بيانات شبكة ميكروتك - شهر ${report.monthYear}")
            sb.appendLine("إجمالي التحميل (GB),إجمالي الرفع (GB),إجمالي الاستهلاك (GB),إجمالي المستخدمين,المتجاوزين للحد")
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%.2f,%.2f,%.2f,%d,%d",
                    report.totalDownloadGb,
                    report.totalUploadGb,
                    report.totalDownloadGb + report.totalUploadGb,
                    report.totalUsers,
                    report.exceededUsersCount
                )
            )
            sb.appendLine()
            sb.appendLine("م,اسم المستخدم,عنوان IP,الباقة المحددة (GB),تم تحميله (GB),تم رفعه (GB),إجمالي الاستهلاك (GB),المتبقي (GB),نسبة الاستهلاك %,الحالة")
            report.topUsers.forEachIndexed { index, user ->
                val remaining = (user.quotaGb - user.totalUsedGb).coerceAtLeast(0.0)
                sb.appendLine(
                    String.format(
                        Locale.US,
                        "%d,\"%s\",%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f%%,%s",
                        index + 1,
                        user.name,
                        user.ip,
                        user.quotaGb,
                        user.usedDownloadGb,
                        user.usedUploadGb,
                        user.totalUsedGb,
                        remaining,
                        user.percentageUsed,
                        user.status.labelAr
                    )
                )
            }
        } else {
            sb.appendLine("MikroTik Network Data Usage Report - Month ${report.monthYear}")
            sb.appendLine("Total Download (GB),Total Upload (GB),Total Traffic (GB),Total Users,Exceeded Quota")
            sb.appendLine(
                String.format(
                    Locale.US,
                    "%.2f,%.2f,%.2f,%d,%d",
                    report.totalDownloadGb,
                    report.totalUploadGb,
                    report.totalDownloadGb + report.totalUploadGb,
                    report.totalUsers,
                    report.exceededUsersCount
                )
            )
            sb.appendLine()
            sb.appendLine("#,User Name,IP Address,Quota (GB),Download (GB),Upload (GB),Total Used (GB),Remaining (GB),Usage %,Status")
            report.topUsers.forEachIndexed { index, user ->
                val remaining = (user.quotaGb - user.totalUsedGb).coerceAtLeast(0.0)
                sb.appendLine(
                    String.format(
                        Locale.US,
                        "%d,\"%s\",%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f%%,%s",
                        index + 1,
                        user.name,
                        user.ip,
                        user.quotaGb,
                        user.usedDownloadGb,
                        user.usedUploadGb,
                        user.totalUsedGb,
                        remaining,
                        user.percentageUsed,
                        user.status.labelEn
                    )
                )
            }
        }
        return sb.toString()
    }

    fun generateTextSummary(report: MonthlyReportData, isArabic: Boolean): String {
        val total = report.totalDownloadGb + report.totalUploadGb
        return if (isArabic) {
            """
            📊 تقرير استهلاك شبكة ميكروتك (MikroTrack)
            🗓 الشهر: ${report.monthYear}
            ━━━━━━━━━━━━━━━━━━━
            🌐 إجمالي استهلاك الشبكة: ${String.format(Locale.US, "%.2f", total)} GB
            📥 إجمالي التحميل: ${String.format(Locale.US, "%.2f", report.totalDownloadGb)} GB
            📤 إجمالي الرفع: ${String.format(Locale.US, "%.2f", report.totalUploadGb)} GB
            👥 عدد المشتركين النشطين: ${report.totalUsers}
            ⚠️ تجاوزوا الباقة المقررة: ${report.exceededUsersCount} مستخدم
            
            🏆 أعلى 5 مستخدمين استهلاكاً:
            ${report.topUsers.take(5).mapIndexed { i, u ->
                "${i + 1}. ${u.name} (${u.ip}): ${String.format(Locale.US, "%.2f", u.totalUsedGb)} GB من ${u.quotaGb} GB (${String.format(Locale.US, "%.0f", u.percentageUsed)}%)"
            }.joinToString("\n")}
            
            📌 تم إنشاء التقرير بواسطة تطبيق إدارة ميكروتك MikroTrack
            """.trimIndent()
        } else {
            """
            📊 MikroTik Network Usage Report (MikroTrack)
            🗓 Month: ${report.monthYear}
            ━━━━━━━━━━━━━━━━━━━
            🌐 Total Network Traffic: ${String.format(Locale.US, "%.2f", total)} GB
            📥 Total Download: ${String.format(Locale.US, "%.2f", report.totalDownloadGb)} GB
            📤 Total Upload: ${String.format(Locale.US, "%.2f", report.totalUploadGb)} GB
            👥 Active Users: ${report.totalUsers}
            ⚠️ Quota Exceeded: ${report.exceededUsersCount} users
            
            🏆 Top 5 Consumers:
            ${report.topUsers.take(5).mapIndexed { i, u ->
                "${i + 1}. ${u.name} (${u.ip}): ${String.format(Locale.US, "%.2f", u.totalUsedGb)} GB / ${u.quotaGb} GB (${String.format(Locale.US, "%.0f", u.percentageUsed)}%)"
            }.joinToString("\n")}
            
            📌 Generated by MikroTrack Network Manager
            """.trimIndent()
        }
    }

    fun shareReport(context: Context, report: MonthlyReportData, isArabic: Boolean, asCsv: Boolean) {
        if (asCsv) {
            try {
                val csvContent = generateCsv(report, isArabic)
                val fileName = "mikrotik_report_${report.monthYear.replace("-", "_")}.csv"
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { it.write(csvContent.toByteArray()) }

                val authority = "${context.packageName}.fileprovider"
                val contentUri = FileProvider.getUriForFile(context, authority, file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, if (isArabic) "تقرير استهلاك ميكروتك - ${report.monthYear}" else "MikroTik Report - ${report.monthYear}")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, generateTextSummary(report, isArabic))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, if (isArabic) "تصدير ملف CSV" else "Export CSV Report"))
            } catch (e: Exception) {
                // Fallback to text sharing if file provider is not configured
                shareText(context, generateTextSummary(report, isArabic), isArabic)
            }
        } else {
            shareText(context, generateTextSummary(report, isArabic), isArabic)
        }
    }

    private fun shareText(context: Context, text: String, isArabic: Boolean) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, if (isArabic) "تقرير استهلاك شبكة ميكروتك" else "MikroTik Network Usage Report")
        }
        context.startActivity(Intent.createChooser(intent, if (isArabic) "مشاركة التقرير" else "Share Report"))
    }
}
