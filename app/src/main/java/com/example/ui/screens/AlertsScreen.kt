package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.AlertEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.SeverityBadge
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    val alerts by viewModel.alerts.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Unresolved, 1: All, 2: Resolved

    val filteredAlerts = remember(alerts, selectedTab) {
        when (selectedTab) {
            0 -> alerts.filter { !it.isResolved }
            1 -> alerts
            2 -> alerts.filter { it.isResolved }
            else -> alerts
        }
    }

    val unresolvedCount = alerts.count { !it.isResolved }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("alerts_screen")
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isArabic) "مركز التنبيهات الفورية" else "Instant Alert Center",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isArabic)
                        "مراقبة لحظية لتجاوز باقات البيانات وحدود السرعة"
                    else
                        "Real-time monitoring of data quota limits & threshold breaches",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (alerts.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.clearAllAlerts() },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "مسح الكل" else "Clear All",
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavyCard,
            contentColor = CyanAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyanAccent
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabic) "غير محلولة" else "Unresolved",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                        if (unresolvedCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AlertRed)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unresolvedCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (isArabic) "الكل (${alerts.size})" else "All (${alerts.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )

            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = if (isArabic) "تمت معالجتها" else "Resolved",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Alerts List
        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isArabic) "لا توجد تنبيهات نشطة حالياً" else "No active alerts at this time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic)
                            "جميع المشتركين ضمن حدود الاستهلاك المحددة."
                        else
                            "All connected users are within their data allowances.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertCard(
                        alert = alert,
                        isArabic = isArabic,
                        onResolve = { action -> viewModel.resolveAlert(alert.id, action) },
                        onResetQuota = { alert.userId?.let { viewModel.resetUserQuota(it) } },
                        onAddExtraQuota = { alert.userId?.let { viewModel.addExtraQuota(it, 10.0) } },
                        onThrottle = { alert.userId?.let { viewModel.throttleUser(it, 1.0) } }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: AlertEntity,
    isArabic: Boolean,
    onResolve: (String) -> Unit,
    onResetQuota: () -> Unit,
    onAddExtraQuota: () -> Unit,
    onThrottle: () -> Unit
) {
    val borderColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertRed.copy(alpha = 0.6f)
        AlertSeverity.WARNING -> AlertAmber.copy(alpha = 0.5f)
        AlertSeverity.INFO -> InfoBlue.copy(alpha = 0.4f)
    }

    val icon = when (alert.severity) {
        AlertSeverity.CRITICAL -> Icons.Default.Error
        AlertSeverity.WARNING -> Icons.Default.Warning
        AlertSeverity.INFO -> Icons.Default.Info
    }

    val iconTint = when (alert.severity) {
        AlertSeverity.CRITICAL -> AlertRed
        AlertSeverity.WARNING -> AlertAmber
        AlertSeverity.INFO -> InfoBlue
    }

    val timeFormatted = remember(alert.timestamp) {
        SimpleDateFormat("hh:mm a • yyyy-MM-dd", Locale.getDefault()).format(Date(alert.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_card_${alert.id}"),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        border = if (!alert.isResolved) androidx.compose.foundation.BorderStroke(1.2.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                SeverityBadge(severity = alert.severity, isArabic = isArabic)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body
            Text(
                text = alert.message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (alert.userName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${if (isArabic) "المشترك المعني:" else "Target User:"} ${alert.userName}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                if (alert.isResolved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (alert.actionTaken.isNotEmpty()) alert.actionTaken else (if (isArabic) "تمت المعالجة" else "Resolved"),
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Quick Resolution Actions for Unresolved Alerts
            if (!alert.isResolved && alert.userId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reset
                    OutlinedButton(
                        onClick = onResetQuota,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = if (isArabic) "تصفير" else "Reset", fontSize = 10.sp)
                    }

                    // +10GB
                    OutlinedButton(
                        onClick = onAddExtraQuota,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "+10GB", fontSize = 10.sp)
                    }

                    // Throttle
                    OutlinedButton(
                        onClick = onThrottle,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertAmber)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = if (isArabic) "خنق" else "Throttle", fontSize = 10.sp)
                    }

                    // Dismiss
                    Button(
                        onClick = { onResolve(if (isArabic) "تم الإغلاق بواسطة المدير" else "Dismissed by admin") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyCardLight)
                    ) {
                        Text(text = if (isArabic) "تجاهل" else "Dismiss", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
