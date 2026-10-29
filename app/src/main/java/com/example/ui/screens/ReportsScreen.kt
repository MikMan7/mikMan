package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.DailyTrafficPoint
import com.example.data.model.MonthlyReportData
import com.example.ui.MainViewModel
import com.example.ui.components.QuotaProgressBar
import com.example.ui.components.StatMetricCard
import com.example.ui.components.UserStatusChip
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDark
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val reportData by viewModel.monthlyReport.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Export Actions
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "التقارير الشهرية المفصلة" else "Detailed Monthly Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic)
                                "تحليل دقيق لاستهلاك المشتركين وتصدير ملفات البيانات"
                            else
                                "Accurate consumption audit & data export",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Selector Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.availableMonths) { month ->
                        val isSelected = selectedMonth == month
                        val label = when (month) {
                            viewModel.currentMonthKey -> if (isArabic) "الشهر الحالي ($month)" else "Current ($month)"
                            "2026-09" -> if (isArabic) "سبتمبر 2026" else "September 2026"
                            "2026-08" -> if (isArabic) "أغسطس 2026" else "August 2026"
                            else -> month
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CyanAccent else NavyCard)
                                .border(
                                    1.dp,
                                    if (isSelected) CyanAccent else NavyCardLight,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectMonth(month) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: Export CSV & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.exportReport(context, asCsv = true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_csv_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تصدير ملف CSV" else "Export CSV",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.exportReport(context, asCsv = false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_summary_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "مشاركة الملخص" else "Share Summary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        reportData?.let { report ->
            // KPI Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        title = if (isArabic) "إجمالي حركة البيانات" else "Total Traffic",
                        value = String.format(Locale.US, "%.1f", report.totalDownloadGb + report.totalUploadGb),
                        unit = "GB",
                        icon = Icons.Default.Speed,
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )

                    StatMetricCard(
                        title = if (isArabic) "المتجاوزين للحد" else "Quota Breaches",
                        value = "${report.exceededUsersCount}",
                        unit = if (isArabic) "مشترك" else "users",
                        icon = Icons.Default.Warning,
                        accentColor = if (report.exceededUsersCount > 0) AlertRed else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        title = if (isArabic) "تحميل (Download)" else "Download",
                        value = String.format(Locale.US, "%.1f", report.totalDownloadGb),
                        unit = "GB",
                        icon = Icons.Default.ArrowDownward,
                        accentColor = CyanDark,
                        modifier = Modifier.weight(1f)
                    )

                    StatMetricCard(
                        title = if (isArabic) "رفع (Upload)" else "Upload",
                        value = String.format(Locale.US, "%.1f", report.totalUploadGb),
                        unit = "GB",
                        icon = Icons.Default.ArrowUpward,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily Trend Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isArabic) "منحنى الاستهلاك اليومي للشهر" else "Daily Consumption Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "حجم البيانات المستهلكة لكل يوم (GB)" else "Data volume consumed per day (GB)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Mini Bars
                        DailyBarsGraph(points = report.dailyTrend)
                    }
                }
            }

            // Section Header: Detailed Breakdown Table
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "جدول تفاصيل المشتركين (${report.topUsers.size})" else "Users Breakdown (${report.topUsers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // User Breakdown Rows
            items(report.topUsers) { userItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userItem.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = userItem.ip,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            UserStatusChip(status = userItem.status, isArabic = isArabic)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        QuotaProgressBar(percentage = userItem.percentageUsed)

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    if (isArabic) "تحميل: %.1f GB • رفع: %.1f GB" else "DL: %.1f GB • UL: %.1f GB",
                                    userItem.usedDownloadGb,
                                    userItem.usedUploadGb
                                ),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.1f / %.1f GB (%.0f%%)",
                                    userItem.totalUsedGb,
                                    userItem.quotaGb,
                                    userItem.percentageUsed
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userItem.percentageUsed >= 100.0) AlertRed else CyanAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBarsGraph(
    points: List<DailyTrafficPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = points.maxOfOrNull { it.downloadGb + it.uploadGb }?.coerceAtLeast(1.0) ?: 1.0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { p ->
            val total = p.downloadGb + p.uploadGb
            val heightFraction = (total / maxVal).toFloat().coerceIn(0.08f, 1f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFraction)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (p.day % 7 == 0) AlertRed.copy(alpha = 0.8f) else CyanDark
                        )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("1", fontSize = 10.sp, color = Color.Gray)
        Text("7", fontSize = 10.sp, color = Color.Gray)
        Text("14", fontSize = 10.sp, color = Color.Gray)
        Text("21", fontSize = 10.sp, color = Color.Gray)
        Text("${points.size}", fontSize = 10.sp, color = Color.Gray)
    }
}
