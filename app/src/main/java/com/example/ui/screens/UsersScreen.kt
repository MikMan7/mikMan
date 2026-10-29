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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.UserUsage
import com.example.data.model.AppLanguage
import com.example.data.model.UserStatus
import com.example.ui.MainViewModel
import com.example.ui.components.FilterChip
import com.example.ui.components.UserCard
import com.example.ui.components.UserEditorDialog
import com.example.ui.components.UserUsageHistoryDialog
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
fun UsersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    val users by viewModel.filteredUsers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val usageLogs by viewModel.userUsageLogs.collectAsState()

    var showUserDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<NetworkUserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<NetworkUserEntity?>(null) }
    var userForUsageLogs by remember { mutableStateOf<NetworkUserEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_search_input"),
                placeholder = {
                    Text(
                        text = if (isArabic) "البحث بالاسم أو عنوان IP أو اسم المشترك..." else "Search by name, IP, or Hotspot user...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyanAccent
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = NavyCardLight,
                    focusedContainerColor = NavyCard,
                    unfocusedContainerColor = NavyCard
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter status chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSelected = filterStatus == null
                    FilterChip(
                        label = if (isArabic) "الكل (${users.size})" else "All (${users.size})",
                        isSelected = isSelected,
                        onClick = { viewModel.setFilterStatus(null) }
                    )
                }

                items(UserStatus.values()) { status ->
                    val isSelected = filterStatus == status
                    FilterChip(
                        label = if (isArabic) status.labelAr else status.labelEn,
                        isSelected = isSelected,
                        onClick = { viewModel.setFilterStatus(if (isSelected) null else status) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Cards List
            if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isArabic) "لا يوجد مستخدمين مطابقين للبحث" else "No users match your filter",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(users, key = { it.id }) { user ->
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
                                showUserDialog = true
                            },
                            onDelete = { userToDelete = user }
                        )
                    }
                }
            }
        }

        // FAB to Add User
        FloatingActionButton(
            onClick = {
                selectedUserForEdit = null
                showUserDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_user_fab"),
            containerColor = CyanAccent,
            contentColor = Color.Black
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add User")
        }
    }

    // Add / Edit User Dialog
    if (showUserDialog) {
        UserEditorDialog(
            user = selectedUserForEdit,
            isArabic = isArabic,
            onDismiss = { showUserDialog = false },
            onSave = { updatedUser ->
                viewModel.saveUser(updatedUser)
                showUserDialog = false
            }
        )
    }

    // Delete confirmation dialog
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
                        "هل أنت متأكد من حذف المشترك '${userToDelete?.name}' وإلغاء مراقبته؟"
                    else
                        "Are you sure you want to delete '${userToDelete?.name}' and stop tracking quota?"
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

    // Usage History Dialog (UserUsage Room Entity records)
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
