package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.entity.AlertEntity
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.RouterEntity
import com.example.data.db.entity.UserUsage
import com.example.data.model.AppLanguage
import com.example.data.model.MonthlyReportData
import com.example.data.model.NavigationTab
import com.example.data.model.UserStatus
import com.example.data.repository.MikroTikRepository
import com.example.data.service.ReportExporter
import com.example.data.service.RouterSystemInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Success(val info: RouterSystemInfo) : ConnectionTestState
    data class Error(val message: String) : ConnectionTestState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MikroTikRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = MikroTikRepository(database.mikroTikDao())
    }

    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow<UserStatus?>(null)
    val filterStatus: StateFlow<UserStatus?> = _filterStatus.asStateFlow()

    val routers: StateFlow<List<RouterEntity>> = repository.routers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val rawUsers: StateFlow<List<NetworkUserEntity>> = repository.users.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered users
    val filteredUsers: StateFlow<List<NetworkUserEntity>> = combine(
        rawUsers,
        _searchQuery,
        _filterStatus
    ) { usersList, query, status ->
        usersList.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.name.contains(query, ignoreCase = true) ||
                    user.ipAddress.contains(query, ignoreCase = true) ||
                    user.hotspotUser.contains(query, ignoreCase = true)
            val matchesStatus = status == null || user.status == status
            matchesQuery && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val alerts: StateFlow<List<AlertEntity>> = repository.alerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unresolvedAlertCount: StateFlow<Int> = repository.unresolvedAlertCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val defaultRouter: StateFlow<RouterEntity?> = combine(routers) { list ->
        list.firstOrNull()?.find { it.isDefault } ?: list.firstOrNull()?.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Monthly Report State
    val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    private val _selectedMonth = MutableStateFlow(currentMonthKey)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val availableMonths = listOf(
        currentMonthKey,
        "2026-09",
        "2026-08"
    )

    private val _monthlyReport = MutableStateFlow<MonthlyReportData?>(null)
    val monthlyReport: StateFlow<MonthlyReportData?> = _monthlyReport.asStateFlow()

    private val _selectedUserIdForUsage = MutableStateFlow<Long?>(null)
    val selectedUserIdForUsage: StateFlow<Long?> = _selectedUserIdForUsage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val userUsageLogs: StateFlow<List<UserUsage>> = _selectedUserIdForUsage.flatMapLatest { id ->
        if (id != null) repository.getUsageForUser(id) else repository.allUsageRecords
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _testConnectionState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testConnectionState: StateFlow<ConnectionTestState> = _testConnectionState.asStateFlow()

    init {
        loadReportForMonth(currentMonthKey)
    }

    fun selectUserForUsageLogs(userId: Long?) {
        _selectedUserIdForUsage.value = userId
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun toggleLanguage() {
        _appLanguage.value = if (_appLanguage.value == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: UserStatus?) {
        _filterStatus.value = status
    }

    fun selectMonth(monthYear: String) {
        _selectedMonth.value = monthYear
        loadReportForMonth(monthYear)
    }

    fun loadReportForMonth(monthYear: String) {
        viewModelScope.launch {
            val report = repository.getMonthlyReport(monthYear)
            _monthlyReport.value = report
        }
    }

    fun resetUserQuota(userId: Long) {
        viewModelScope.launch {
            repository.resetUserQuota(userId)
            loadReportForMonth(_selectedMonth.value)
        }
    }

    fun addExtraQuota(userId: Long, extraGb: Double = 10.0) {
        viewModelScope.launch {
            repository.addExtraQuota(userId, extraGb)
            loadReportForMonth(_selectedMonth.value)
        }
    }

    fun throttleUser(userId: Long, throttleSpeedMbps: Double = 1.0) {
        viewModelScope.launch {
            repository.throttleUser(userId, throttleSpeedMbps)
        }
    }

    fun toggleSuspendUser(userId: Long) {
        viewModelScope.launch {
            repository.toggleSuspendUser(userId)
        }
    }

    fun saveUser(user: NetworkUserEntity) {
        viewModelScope.launch {
            repository.saveUser(user)
            loadReportForMonth(_selectedMonth.value)
        }
    }

    fun deleteUser(user: NetworkUserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
            loadReportForMonth(_selectedMonth.value)
        }
    }

    fun saveRouter(router: RouterEntity) {
        viewModelScope.launch {
            repository.saveRouter(router)
        }
    }

    fun deleteRouter(router: RouterEntity) {
        viewModelScope.launch {
            repository.deleteRouter(router)
        }
    }

    fun resolveAlert(alertId: Long, action: String) {
        viewModelScope.launch {
            repository.resolveAlert(alertId, action)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }

    fun testRouterConnection(router: RouterEntity) {
        viewModelScope.launch {
            _testConnectionState.value = ConnectionTestState.Testing
            val res = repository.testRouterConnection(router)
            res.fold(
                onSuccess = { info ->
                    _testConnectionState.value = ConnectionTestState.Success(info)
                },
                onFailure = { err ->
                    _testConnectionState.value = ConnectionTestState.Error(err.message ?: "فشل الاتصال بالموجّه")
                }
            )
        }
    }

    fun clearTestState() {
        _testConnectionState.value = ConnectionTestState.Idle
    }

    fun exportReport(context: Context, asCsv: Boolean) {
        val report = _monthlyReport.value ?: return
        val isArabic = _appLanguage.value == AppLanguage.ARABIC
        ReportExporter.shareReport(context, report, isArabic, asCsv)
    }
}
