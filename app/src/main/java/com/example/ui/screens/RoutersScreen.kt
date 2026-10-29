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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.db.entity.RouterEntity
import com.example.data.model.AppLanguage
import com.example.ui.ConnectionTestState
import com.example.ui.MainViewModel
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDark
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen

@Composable
fun RoutersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isArabic = language == AppLanguage.ARABIC

    val routers by viewModel.routers.collectAsState()
    val testState by viewModel.testConnectionState.collectAsState()

    var showRouterDialog by remember { mutableStateOf(false) }
    var selectedRouterForEdit by remember { mutableStateOf<RouterEntity?>(null) }
    var routerToDelete by remember { mutableStateOf<RouterEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("routers_screen")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isArabic) "إدارة موجّهات ميكروتك" else "MikroTik Routers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic)
                            "التحكم بالبوابات ونقاط الوصول ومراقبة الموارد"
                        else
                            "Gateway management & hardware telemetry",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Connection Status Banner if active
            when (testState) {
                is ConnectionTestState.Testing -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyCardLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isArabic) "جارٍ اختبار الاتصال بـ RouterOS API..." else "Testing connection to RouterOS API...",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                is ConnectionTestState.Success -> {
                    val info = (testState as ConnectionTestState.Success).info
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "نجح الاتصال بالموجّه!" else "Connected Successfully!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "${info.boardName} • ${info.version} • CPU: ${info.cpuLoad}%",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.clearTestState() }) {
                                Icon(Icons.Default.Check, contentDescription = "OK", tint = SuccessGreen)
                            }
                        }
                    }
                }
                is ConnectionTestState.Error -> {
                    val msg = (testState as ConnectionTestState.Error).message
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = AlertRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "تعذر الاتصال بالموجّه الحقيقي" else "Cannot reach physical router",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = AlertRed
                                    )
                                    Text(
                                        text = "$msg (التطبيق يعمل بالمحاكاة المباشرة)",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.clearTestState() }) {
                                Icon(Icons.Default.Check, contentDescription = "Dismiss", tint = AlertRed)
                            }
                        }
                    }
                }
                ConnectionTestState.Idle -> Unit
            }

            // Routers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(routers, key = { it.id }) { router ->
                    RouterCard(
                        router = router,
                        isArabic = isArabic,
                        onTestConnection = { viewModel.testRouterConnection(router) },
                        onEdit = {
                            selectedRouterForEdit = router
                            showRouterDialog = true
                        },
                        onDelete = { routerToDelete = router },
                        onSetDefault = {
                            viewModel.saveRouter(router.copy(isDefault = true))
                        }
                    )
                }
            }
        }

        // FAB to Add Router
        FloatingActionButton(
            onClick = {
                selectedRouterForEdit = null
                showRouterDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_router_fab"),
            containerColor = CyanAccent,
            contentColor = Color.Black
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Router")
        }
    }

    // Add/Edit Router Dialog
    if (showRouterDialog) {
        RouterEditorDialog(
            router = selectedRouterForEdit,
            isArabic = isArabic,
            onDismiss = { showRouterDialog = false },
            onSave = { updatedRouter ->
                viewModel.saveRouter(updatedRouter)
                showRouterDialog = false
            }
        )
    }

    // Delete confirmation
    if (routerToDelete != null) {
        AlertDialog(
            onDismissRequest = { routerToDelete = null },
            title = {
                Text(
                    text = if (isArabic) "حذف الموجّه" else "Delete Router",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic)
                        "هل أنت متأكد من حذف الموجّه '${routerToDelete?.name}'؟"
                    else
                        "Are you sure you want to delete '${routerToDelete?.name}'?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        routerToDelete?.let { viewModel.deleteRouter(it) }
                        routerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text(if (isArabic) "نعم، حذف" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { routerToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun RouterCard(
    router: RouterEntity,
    isArabic: Boolean,
    onTestConnection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("router_card_${router.id}"),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        border = if (router.isDefault) androidx.compose.foundation.BorderStroke(1.2.dp, CyanAccent) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanDark.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = router.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${router.ip}:${router.port} • ${router.routerModel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (router.isDefault) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isArabic) "الرئيسي" else "Default",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Resource Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavyCardLight)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = if (isArabic) "إصدار النظام" else "RouterOS", fontSize = 10.sp, color = Color.Gray)
                    Text(text = router.routerOsVersion, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Column {
                    Text(text = if (isArabic) "حمل المعالج" else "CPU Load", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "${router.cpuLoad}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                }

                Column {
                    Text(text = if (isArabic) "الذاكرة المتاحة" else "Free RAM", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "${router.freeRamMb} MB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }

                Column {
                    Text(text = if (isArabic) "وقت التشغيل" else "Uptime", fontSize = 10.sp, color = Color.Gray)
                    Text(text = router.uptime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                ) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "فحص الاتصال (Ping/API)" else "Test Connection",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!router.isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = if (isArabic) "تعيين كافتراضي" else "Set as Default",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouterEditorDialog(
    router: RouterEntity?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (RouterEntity) -> Unit
) {
    var name by remember { mutableStateOf(router?.name ?: "") }
    var ip by remember { mutableStateOf(router?.ip ?: "192.168.88.1") }
    var port by remember { mutableStateOf(router?.port?.toString() ?: "80") }
    var username by remember { mutableStateOf(router?.username ?: "admin") }
    var password by remember { mutableStateOf(router?.password ?: "") }
    var model by remember { mutableStateOf(router?.routerModel ?: "MikroTik Router") }
    var useSsl by remember { mutableStateOf(router?.useSsl ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (router == null) {
                    if (isArabic) "إضافة موجّه جديد" else "Add MikroTik Router"
                } else {
                    if (isArabic) "تعديل إعدادات الموجّه" else "Edit Router"
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
                        label = { Text(if (isArabic) "اسم الموجّه (الاسم المستعار)" else "Router Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text(if (isArabic) "عنوان IP / Host" else "IP / Hostname") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(if (isArabic) "المنفذ Port" else "Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(if (isArabic) "اسم المستخدم (RouterOS)" else "Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(if (isArabic) "طراز الجهاز (Model)" else "Board Model") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "استخدام تشفير SSL (HTTPS)" else "Use SSL (HTTPS)",
                            fontSize = 13.sp
                        )
                        Switch(
                            checked = useSsl,
                            onCheckedChange = { useSsl = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: 80
                    val newRouter = (router ?: RouterEntity(
                        name = name.ifBlank { "MikroTik Router" },
                        ip = ip
                    )).copy(
                        name = name.ifBlank { "MikroTik Router" },
                        ip = ip,
                        port = p,
                        username = username,
                        password = password,
                        routerModel = model,
                        useSsl = useSsl
                    )
                    onSave(newRouter)
                },
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
