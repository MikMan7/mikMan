package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.UserUsage
import com.example.data.model.UserStatus
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDark
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyanAccent else NavyCard)
            .border(
                1.dp,
                if (isSelected) CyanAccent else NavyCardLight,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

@Composable
fun UserCard(
    user: NetworkUserEntity,
    isArabic: Boolean,
    onResetQuota: () -> Unit,
    onAddExtraQuota: () -> Unit,
    onThrottle: () -> Unit,
    onToggleSuspend: () -> Unit,
    onViewUsageLogs: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_card_${user.id}"),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        border = if (user.usagePercentage >= 100.0)
            androidx.compose.foundation.BorderStroke(1.2.dp, AlertRed.copy(alpha = 0.6f))
        else if (user.usagePercentage >= user.alertThresholdPercent.toDouble())
            androidx.compose.foundation.BorderStroke(1.dp, AlertAmber.copy(alpha = 0.5f))
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // User Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewUsageLogs() }
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "IP: ${user.ipAddress} • MAC: ${user.macAddress.ifEmpty { "Dynamic" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserStatusChip(status = user.status, isArabic = isArabic)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onViewUsageLogs,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("user_history_button_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = if (isArabic) "سجل الاستهلاك" else "Usage History",
                            tint = CyanDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit User",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete User",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quota Bar & Metrics
            QuotaProgressBar(percentage = user.usagePercentage)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(
                        Locale.US,
                        if (isArabic) "المستهلك: %.1f / %.1f GB" else "Used: %.1f / %.1f GB",
                        user.totalUsedGb,
                        user.quotaGb
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format(
                        Locale.US,
                        if (isArabic) "المتبقي: %.1f GB (%.0f%%)" else "Left: %.1f GB (%.0f%%)",
                        user.remainingGb,
                        user.usagePercentage
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (user.usagePercentage >= 100.0) AlertRed else CyanAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Speeds & Max Limits
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyCardLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.currentDownloadMbps} Mbps (الحد: ${user.maxDownloadMbps})",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = AlertAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.currentUploadMbps} Mbps",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset Quota
                OutlinedButton(
                    onClick = onResetQuota,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reset_quota_${user.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "تصفير" else "Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add +10GB
                OutlinedButton(
                    onClick = onAddExtraQuota,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_quota_${user.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
                ) {
                    Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+10GB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Throttle
                OutlinedButton(
                    onClick = onThrottle,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("throttle_${user.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (user.status == UserStatus.THROTTLED) AlertAmber else InfoBlue
                    )
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "خنق 1M" else "Throttle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Suspend / Resume
                OutlinedButton(
                    onClick = onToggleSuspend,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("suspend_${user.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (user.status == UserStatus.SUSPENDED) SuccessGreen else AlertRed
                    )
                ) {
                    Icon(
                        if (user.status == UserStatus.SUSPENDED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (user.status == UserStatus.SUSPENDED) {
                            if (isArabic) "تشغيل" else "Resume"
                        } else {
                            if (isArabic) "إيقاف" else "Pause"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun UserEditorDialog(
    user: NetworkUserEntity?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (NetworkUserEntity) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var ip by remember { mutableStateOf(user?.ipAddress ?: "192.168.88.") }
    var mac by remember { mutableStateOf(user?.macAddress ?: "") }
    var hotspotUser by remember { mutableStateOf(user?.hotspotUser ?: "") }
    var quotaGb by remember { mutableStateOf(user?.quotaGb?.toString() ?: "50") }
    var maxDown by remember { mutableStateOf(user?.maxDownloadMbps?.toString() ?: "15") }
    var maxUp by remember { mutableStateOf(user?.maxUploadMbps?.toString() ?: "4") }
    var alertThreshold by remember { mutableStateOf(user?.alertThresholdPercent?.toString() ?: "85") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (user == null) {
                    if (isArabic) "إضافة مشترك جديد" else "Add New User"
                } else {
                    if (isArabic) "تعديل بيانات المشترك" else "Edit User"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isArabic) "اسم المشترك" else "User Name") },
                        modifier = Modifier.fillMaxWidth().testTag("user_input_name"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text(if (isArabic) "عنوان IP المستهدف" else "Target IP") },
                        modifier = Modifier.fillMaxWidth().testTag("user_input_ip"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = hotspotUser,
                        onValueChange = { hotspotUser = it },
                        label = { Text(if (isArabic) "مستخدم الهوتسبوت (اختياري)" else "Hotspot User (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quotaGb,
                            onValueChange = { quotaGb = it },
                            label = { Text(if (isArabic) "الباقة (GB)" else "Quota (GB)") },
                            modifier = Modifier.weight(1f).testTag("user_input_quota"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = alertThreshold,
                            onValueChange = { alertThreshold = it },
                            label = { Text(if (isArabic) "تنبيه عند %" else "Alert Threshold %") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = maxDown,
                            onValueChange = { maxDown = it },
                            label = { Text(if (isArabic) "سرعة التحميل (M)" else "Max Down (M)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxUp,
                            onValueChange = { maxUp = it },
                            label = { Text(if (isArabic) "سرعة الرفع (M)" else "Max Up (M)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qGb = quotaGb.toDoubleOrNull() ?: 50.0
                    val dMbps = maxDown.toDoubleOrNull() ?: 15.0
                    val uMbps = maxUp.toDoubleOrNull() ?: 4.0
                    val threshold = alertThreshold.toIntOrNull() ?: 85

                    val newUser = (user ?: NetworkUserEntity(
                        name = name.ifBlank { "مشترك جديد" },
                        ipAddress = ip
                    )).copy(
                        name = name.ifBlank { "مشترك جديد" },
                        ipAddress = ip,
                        macAddress = mac,
                        hotspotUser = hotspotUser,
                        quotaGb = qGb,
                        maxDownloadMbps = dMbps,
                        maxUploadMbps = uMbps,
                        alertThresholdPercent = threshold
                    )
                    onSave(newUser)
                },
                modifier = Modifier.testTag("user_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text(if (isArabic) "حفظ" else "Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun UserUsageHistoryDialog(
    user: NetworkUserEntity,
    usageLogs: List<UserUsage>,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val totalRecordedMb = usageLogs.sumOf { it.totalMb }
    val totalRecordedGb = totalRecordedMb / 1024.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isArabic) "سجل استهلاك الإنترنت (Room DB)" else "Internet Usage Ledger (Room DB)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${user.name} • ${user.ipAddress}",
                    fontSize = 12.sp,
                    color = CyanAccent
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Summary Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCardLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) "المخزن بالقاعدة" else "Stored in DB",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f GB", totalRecordedGb),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) "عدد السجلات" else "Records",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${usageLogs.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) "الباقة المقررة" else "Quota Limit",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${user.quotaGb} GB",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (usageLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد سجلات استهلاك مخزنة بعد" else "No usage records stored yet",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(usageLogs, key = { it.id }) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = NavyCard),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = log.dateKey,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${log.sessionDurationMinutes} min • Peak: ${log.peakRateMbps} Mbps",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f MB", log.totalMb),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CyanAccent
                                        )
                                        Text(
                                            text = String.format(Locale.US, "↓ %.1f MB • ↑ %.1f MB", log.downloadMb, log.uploadMb),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text(
                    text = if (isArabic) "إغلاق" else "Close",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
