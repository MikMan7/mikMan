package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.model.AppLanguage
import com.example.data.model.NavigationTab
import com.example.data.model.UserStatus
import com.example.ui.MainViewModel
import com.example.ui.components.FilterChip
import com.example.ui.components.StatMetricCard
import com.example.ui.components.UserCard
import com.example.ui.components.UserEditorDialog
import com.example.ui.components.UserUsageHistoryDialog
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDark
import com.example.ui.theme.CyanLight
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen
import java.util.Locale

enum class UserSortOption {
    HIGHEST_USAGE,
    DOWNLOAD_SPEED,
    REMAINING_QUOTA,
    NAME
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    // Room database live state
    val users by viewModel.filteredUsers.collectAsState()
    val defaultRouter by viewModel.defaultRouter.collectAsState()
    val unresolvedCount by viewModel.unresolvedAlertCount.collectAsState()
    val usageLogs by viewModel.userUsageLogs.collectAsState()

    // Local dashboard UI state for interactive sorting, filtering, and user management
    var localSearchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf<UserStatus?>(null) }
    var sortOption by remember { mutableStateOf(UserSortOption.HIGHEST_USAGE) }
    var showHighUsageOnly by remember { mutableStateOf(false) }

    // Dialog states
    var showAddUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<NetworkUserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<NetworkUserEntity?>(null) }
    var userForUsageLogs by remember { mutableStateOf<NetworkUserEntity?>(null) }

    // Aggregate statistics from Room database
    val totalDownloadGb = users.sumOf { it.usedDownloadGb }
    val totalUploadGb = users.sumOf { it.usedUploadGb }
    val totalTrafficGb = totalDownloadGb + totalUploadGb
    val liveDownSpeed = users.sumOf { it.currentDownloadMbps }
    val liveUpSpeed = users.sumOf { it.currentUploadMbps }
    val exceededCount = users.count { it.usagePercentage >= 100.0 }
    val warningCount = users.count { it.usagePercentage >= 80.0 && it.usagePercentage < 100.0 }

    // Filter and sort connected users locally on the dashboard
    val displayedUsers = users
        .filter { user ->
            val matchesQuery = localSearchQuery.isBlank() ||
                    user.name.contains(localSearchQuery, ignoreCase = true) ||
                    user.ipAddress.contains(localSearchQuery, ignoreCase = true) ||
                    user.hotspotUser.contains(localSearchQuery, ignoreCase = true)
            val matchesStatus = selectedFilterStatus == null || user.status == selectedFilterStatus
            val matchesHighUsage = !showHighUsageOnly || user.usagePercentage >= 80.0
            matchesQuery && matchesStatus && matchesHighUsage
        }
        .sortedWith { u1, u2 ->
            when (sortOption) {
                UserSortOption.HIGHEST_USAGE -> u2.totalUsedGb.compareTo(u1.totalUsedGb)
                UserSortOption.DOWNLOAD_SPEED -> u2.currentDownloadMbps.compareTo(u1.currentDownloadMbps)
                UserSortOption.REMAINING_QUOTA -> u1.remainingGb.compareTo(u2.remainingGb)
                UserSortOption.NAME -> u1.name.compareTo(u2.name)
            }
        }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Banner - MikroTik Gateway Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mikrotik_banner_1793289567612),
                        contentDescription = "MikroTik NOC Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xF00A1128),
                                        Color(0xB30A1128),
                                        Color(0x330A1128)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "الموجّه الرئيسي متصل ومراقب" else "Core Gateway Connected",
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyanDark.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = defaultRouter?.routerOsVersion ?: "v7.16",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column {
                            Text(
                                text = defaultRouter?.name ?: "MikroTik CCR2004",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${defaultRouter?.ip ?: "192.168.88.1"} • Uptime: ${defaultRouter?.uptime ?: "42d"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanLight
                            )
                        }
                    }
                }
            }
        }

        // Live Throughput Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_bandwidth_card"),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanDark.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "حركة البيانات المباشرة (Live Traffic)" else "Live WAN Throughput",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SuccessGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isArabic) "بث حي" else "LIVE",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Download Speed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyCardLight)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CyanAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Download",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "سرعة التحميل" else "Download",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f Mbps", liveDownSpeed),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent
                                    )
                                }
                            }
                        }

                        // Upload Speed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavyCardLight)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AlertAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Upload",
                                        tint = AlertAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "سرعة الرفع" else "Upload",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f Mbps", liveUpSpeed),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = if (isArabic) "إجمالي الاستهلاك الشهري" else "Monthly Data",
                    value = String.format(Locale.US, "%.1f", totalTrafficGb),
                    unit = "GB",
                    icon = Icons.Default.Speed,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )

                StatMetricCard(
                    title = if (isArabic) "المشتركين المتصلين" else "Connected Users",
                    value = "${users.size}",
                    unit = if (isArabic) "مستخدم" else "users",
                    icon = Icons.Default.People,
                    accentColor = SuccessGreen,
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
                    title = if (isArabic) "تجاوزوا الحصة" else "Exceeded Quota",
                    value = "$exceededCount",
                    unit = if (isArabic) "مشترك" else "users",
                    icon = Icons.Default.Warning,
                    accentColor = if (exceededCount > 0) AlertRed else SuccessGreen,
                    modifier = Modifier.weight(1f)
                )

                StatMetricCard(
                    title = if (isArabic) "حمل المعالج CPU" else "Router CPU",
                    value = "${defaultRouter?.cpuLoad ?: 14}%",
                    unit = "",
                    icon = Icons.Default.Memory,
                    accentColor = CyanDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Alert Banner if unresolved alerts exist
        if (unresolvedCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTab(NavigationTab.ALERTS) }
                        .testTag("dashboard_alert_banner"),
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AlertRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "يوجد $unresolvedCount تنبيهات بحاجة لاتخاذ إجراء!" else "$unresolvedCount active alerts require attention!",
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isArabic) "اضغط هنا لمراجعة المشتركين المتجاوزين وإعادة الضبط أو خنق السرعة" else "Tap to review quota overages and take instant action",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // CONNECTED USERS & CURRENT DATA USAGE
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connected_users_header_card"),
                colors = CardDefaults.cardColors(containerColor = NavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanDark.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Title & Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "المستخدمين المتصلين واستهلاك البيانات" else "Connected Users & Data Usage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyanDark.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${displayedUsers.size}/${users.size}",
                                        color = CyanAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (isArabic) "مراقبة استهلاك البيانات المباشر المسجلة في قاعدة بيانات Room" else "Live data consumption tracked via local Room database",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                selectedUserForEdit = null
                                showAddUserDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("dashboard_add_user_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add User",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "إضافة مشترك" else "Add User",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Search Bar on Dashboard
                    OutlinedTextField(
                        value = localSearchQuery,
                        onValueChange = { localSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_user_search"),
                        placeholder = {
                            Text(
                                text = if (isArabic) "ابحث عن مشترك، عنوان IP، أو هوتسبوت..." else "Search user by name, IP, or Hotspot...",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (localSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { localSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = NavyCardLight,
                            focusedContainerColor = NavyCardLight,
                            unfocusedContainerColor = NavyCardLight
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isSelected = selectedFilterStatus == null && !showHighUsageOnly
                            FilterChip(
                                label = if (isArabic) "الكل (${users.size})" else "All (${users.size})",
                                isSelected = isSelected,
                                onClick = {
                                    selectedFilterStatus = null
                                    showHighUsageOnly = false
                                }
                            )
                        }

                        item {
                            val isSelected = showHighUsageOnly
                            FilterChip(
                                label = if (isArabic) "استهلاك عالي >80% ($warningCount)" else "High Usage >80% ($warningCount)",
                                isSelected = isSelected,
                                onClick = {
                                    showHighUsageOnly = !showHighUsageOnly
                                    selectedFilterStatus = null
                                }
                            )
                        }

                        item {
                            val isSelected = selectedFilterStatus == UserStatus.EXCEEDED
                            FilterChip(
                                label = if (isArabic) "تجاوزوا الحصة ($exceededCount)" else "Exceeded ($exceededCount)",
                                isSelected = isSelected,
                                onClick = {
                                    selectedFilterStatus = if (isSelected) null else UserStatus.EXCEEDED
                                    showHighUsageOnly = false
                                }
                            )
                        }

                        item {
                            val isSelected = selectedFilterStatus == UserStatus.ACTIVE
                            FilterChip(
                                label = if (isArabic) "نشط" else "Active",
                                isSelected = isSelected,
                                onClick = {
                                    selectedFilterStatus = if (isSelected) null else UserStatus.ACTIVE
                                    showHighUsageOnly = false
                                }
                            )
                        }

                        item {
                            val isSelected = selectedFilterStatus == UserStatus.THROTTLED
                            FilterChip(
                                label = if (isArabic) "مخنوق السرعة" else "Throttled",
                                isSelected = isSelected,
                                onClick = {
                                    selectedFilterStatus = if (isSelected) null else UserStatus.THROTTLED
                                    showHighUsageOnly = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sort selector bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = CyanLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "الترتيب حسب:" else "Sort by:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sortOption == UserSortOption.HIGHEST_USAGE) CyanDark else Color.Transparent)
                                    .clickable { sortOption = UserSortOption.HIGHEST_USAGE }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "الأعلى استهلاكاً" else "Usage",
                                    fontSize = 11.sp,
                                    color = if (sortOption == UserSortOption.HIGHEST_USAGE) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sortOption == UserSortOption.DOWNLOAD_SPEED) CyanDark else Color.Transparent)
                                    .clickable { sortOption = UserSortOption.DOWNLOAD_SPEED }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "السرعة" else "Speed",
                                    fontSize = 11.sp,
                                    color = if (sortOption == UserSortOption.DOWNLOAD_SPEED) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (sortOption == UserSortOption.REMAINING_QUOTA) CyanDark else Color.Transparent)
                                    .clickable { sortOption = UserSortOption.REMAINING_QUOTA }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "المتبقي" else "Left",
                                    fontSize = 11.sp,
                                    color = if (sortOption == UserSortOption.REMAINING_QUOTA) Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Empty state if no users match filters
        if (displayedUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "لا يوجد مشتركين مطابقين لمعايير البحث الحالية" else "No users match your criteria",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Connected Users List with Live Data Usage
            items(displayedUsers, key = { it.id }) { user ->
                UserCard(
                    user = user,
                    isArabic = isArabic,
                    onResetQuota = { viewModel.resetUserQuota(user.id) },
                    onAddExtraQuota = { viewModel.addExtraQuota(user.id, 10.0) },
                    onThrottle = { viewModel.throttleUser(user.id, 1.0) },
                    onToggleSuspend = { viewModel.toggleSuspendUser(user.id) },
                    onViewUsageLogs = {
                        viewModel.selectUserForUsageLogs(user.id)
                        userForUsageLogs = user
                    },
                    onEdit = {
                        selectedUserForEdit = user
                        showAddUserDialog = true
                    },
                    onDelete = { userToDelete = user }
                )
            }
        }

        // Quick Export CTA Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = NavyCardLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "جاهز لتصدير التقرير الشهري؟" else "Export Monthly Report?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isArabic) "صدّر تقرير CSV مفصل لجميع المشتركين بضغطة زر واحدة." else "Export complete CSV usage breakdown with one tap.",
                            fontSize = 11.sp,
                            color = CyanLight
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.selectTab(NavigationTab.REPORTS) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isArabic) "عرض التقارير" else "Reports",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Add / Edit User Dialog directly accessible from Dashboard
    if (showAddUserDialog) {
        UserEditorDialog(
            user = selectedUserForEdit,
            isArabic = isArabic,
            onDismiss = { showAddUserDialog = false },
            onSave = { updatedUser ->
                viewModel.saveUser(updatedUser)
                showAddUserDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = if (isArabic) "حذف المشترك" else "Delete User",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic)
                        "هل أنت متأكد من حذف المشترك '${userToDelete?.name}' وإلغاء مراقبته من قاعدة البيانات؟"
                    else
                        "Are you sure you want to delete '${userToDelete?.name}' from the database?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { viewModel.deleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text(if (isArabic) "نعم، حذف" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Usage History Dialog (Room DB UserUsage entity records)
    if (userForUsageLogs != null) {
        UserUsageHistoryDialog(
            user = userForUsageLogs!!,
            usageLogs = usageLogs,
            isArabic = isArabic,
            onDismiss = {
                userForUsageLogs = null
                viewModel.selectUserForUsageLogs(null)
            }
        )
    }
}
