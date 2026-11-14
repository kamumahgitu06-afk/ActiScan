package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PackageScannerRepository
import com.example.data.RootStatusDetector
import com.example.data.UserPreferencesRepository
import com.example.model.AppTypeFilter
import com.example.model.ExportedActivityItem
import com.example.model.RootDetectionResult
import com.example.model.StatusFilter
import com.example.model.TestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScanStats(
    val totalPackagesScanned: Int = 0,
    val totalExported: Int = 0,
    val noPermissionCount: Int = 0,
    val launchedCount: Int = 0,
    val deniedCount: Int = 0,
    val requiresParamsCount: Int = 0
)

data class MainUiState(
    val disclaimerAccepted: Boolean? = null,
    val showRootStatus: Boolean = true,
    val rootDetection: RootDetectionResult = RootDetectionResult(),
    val isScanning: Boolean = true,
    val isExporting: Boolean = false,
    val allItems: List<ExportedActivityItem> = emptyList(),
    val filteredItems: List<ExportedActivityItem> = emptyList(),
    val stats: ScanStats = ScanStats(),
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val appTypeFilter: AppTypeFilter = AppTypeFilter.ALL,
    val lastScanMillis: Long = 0L,
    val scanError: String? = null
) {
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() ||
            statusFilter != StatusFilter.ALL ||
            appTypeFilter != AppTypeFilter.ALL
}

sealed interface MainUiEvent {
    data class ShowSnackbar(val message: String) : MainUiEvent
}

class MainViewModel(
    private val repository: PackageScannerRepository,
    private val rootDetector: RootStatusDetector,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _allItems = MutableStateFlow<List<ExportedActivityItem>>(emptyList())
    private val _totalPackages = MutableStateFlow(0)
    private val _lastScanMillis = MutableStateFlow(0L)
    private val _isScanning = MutableStateFlow(true)
    private val _isExporting = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _appTypeFilter = MutableStateFlow(AppTypeFilter.ALL)
    private val _scanError = MutableStateFlow<String?>(null)
    private val _rootDetection = MutableStateFlow(RootDetectionResult())

    private val _events = MutableSharedFlow<MainUiEvent>()
    val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

    private val iconCache = LruCache<String, ImageBitmap>(120)

    private data class FilterCriteria(
        val query: String,
        val status: StatusFilter,
        val appType: AppTypeFilter
    )

    private data class PrefsAndRoot(
        val disclaimerAccepted: Boolean?,
        val showRootStatus: Boolean,
        val rootDetection: RootDetectionResult
    )

    private val filterFlow = combine(
        _searchQuery,
        _statusFilter,
        _appTypeFilter
    ) { query, status, appType ->
        FilterCriteria(query, status, appType)
    }

    private val prefsAndRootFlow = combine(
        preferencesRepository.disclaimerAccepted,
        preferencesRepository.showRootStatus,
        _rootDetection
    ) { accepted, showRoot, rootResult ->
        PrefsAndRoot(
            disclaimerAccepted = accepted,
            showRootStatus = showRoot,
            rootDetection = rootResult
        )
    }

    private val scanMetaFlow = combine(
        _isScanning,
        _isExporting,
        _totalPackages,
        _lastScanMillis,
        _scanError
    ) { isScanning, isExporting, totalPackages, lastScanMillis, scanError ->
        arrayOf<Any?>(isScanning, isExporting, totalPackages, lastScanMillis, scanError)
    }

    val uiState: StateFlow<MainUiState> = combine(
        prefsAndRootFlow,
        scanMetaFlow,
        _allItems,
        filterFlow
    ) { prefsAndRoot, meta, items, criteria ->
        val isScanning = meta[0] as Boolean
        val isExporting = meta[1] as Boolean
        val totalPackages = meta[2] as Int
        val lastScanMillis = meta[3] as Long
        val scanError = meta[4] as String?

        val filtered = applyFilters(items, criteria)
        val stats = ScanStats(
            totalPackagesScanned = totalPackages,
            totalExported = items.size,
            noPermissionCount = items.count { it.hasNoPermission },
            launchedCount = items.count { it.testStatus is TestStatus.Launched },
            deniedCount = items.count { it.testStatus is TestStatus.Denied },
            requiresParamsCount = items.count { it.testStatus is TestStatus.RequiresParameters }
        )

        MainUiState(
            disclaimerAccepted = prefsAndRoot.disclaimerAccepted,
            showRootStatus = prefsAndRoot.showRootStatus,
            rootDetection = prefsAndRoot.rootDetection,
            isScanning = isScanning,
            isExporting = isExporting,
            allItems = items,
            filteredItems = filtered,
            stats = stats,
            searchQuery = criteria.query,
            statusFilter = criteria.status,
            appTypeFilter = criteria.appType,
            lastScanMillis = lastScanMillis,
            scanError = scanError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        detectRootStatus()
        scanPackages()
    }

    private fun detectRootStatus() {
        viewModelScope.launch {
            _rootDetection.value = rootDetector.detect()
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            preferencesRepository.setDisclaimerAccepted(true)
        }
    }

    fun setShowRootStatus(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowRootStatus(show)
        }
    }

    fun scanPackages() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _rootDetection.value = rootDetector.detect()
            runCatching {
                repository.scanExportedActivities()
            }.onSuccess { result ->
                val previousStatuses = _allItems.value.associate { it.id to it.testStatus }
                val merged = result.items.map { item ->
                    val prev = previousStatuses[item.id]
                    if (prev != null) item.copy(testStatus = prev) else item
                }
                _allItems.value = merged
                _totalPackages.value = result.totalPackagesScanned
                _lastScanMillis.value = result.scanTimestampMillis
                _isScanning.value = false
            }.onFailure { err ->
                _scanError.value = err.localizedMessage ?: "Gagal memindai paket aplikasi"
                _isScanning.value = false
            }
        }
    }

    fun testSingleActivity(item: ExportedActivityItem) {
        val resultStatus = repository.testSingleActivity(item)
        _allItems.update { list ->
            list.map { current ->
                if (current.id == item.id) current.copy(testStatus = resultStatus) else current
            }
        }
        viewModelScope.launch {
            when (resultStatus) {
                TestStatus.Launched -> {
                    _events.emit(
                        MainUiEvent.ShowSnackbar(
                            "LAUNCHED: ${item.shortActivityName} berhasil diluncurkan"
                        )
                    )
                }
                is TestStatus.Denied -> {
                    _events.emit(
                        MainUiEvent.ShowSnackbar(
                            "DENIED: ${item.shortActivityName} ditolak sistem (SecurityException)"
                        )
                    )
                }
                is TestStatus.RequiresParameters -> {
                    _events.emit(
                        MainUiEvent.ShowSnackbar(
                            "REQUIRES_PARAMETERS: ${resultStatus.reason}"
                        )
                    )
                }
                TestStatus.NotTested -> Unit
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun onAppTypeFilterChange(filter: AppTypeFilter) {
        _appTypeFilter.value = filter
    }

    fun suggestedExportFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "actiscan_audit_$stamp.txt"
    }

    fun exportReport(uri: Uri, exportFilteredOnly: Boolean) {
        viewModelScope.launch {
            _isExporting.value = true
            val state = uiState.value
            val itemsToExport = if (exportFilteredOnly) state.filteredItems else state.allItems
            val content = withContext(Dispatchers.Default) {
                repository.buildTxtReport(
                    items = itemsToExport,
                    totalPackagesScanned = state.stats.totalPackagesScanned,
                    scanTimestampMillis = state.lastScanMillis,
                    rootStatus = state.rootDetection.status
                )
            }
            val result = repository.writeReportToUri(uri, content)
            _isExporting.value = false
            result.onSuccess {
                _events.emit(
                    MainUiEvent.ShowSnackbar("Berhasil export ${itemsToExport.size} activity ke .txt")
                )
            }.onFailure { err ->
                _events.emit(
                    MainUiEvent.ShowSnackbar(
                        "Gagal export .txt: ${err.localizedMessage ?: "Unknown error"}"
                    )
                )
            }
        }
    }

    suspend fun getAppIcon(packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            val drawable = repository.loadAppIcon(packageName) ?: return@withContext null
            val bitmap: Bitmap = drawable.toBitmap(width = 72, height = 72)
            val imageBitmap = bitmap.asImageBitmap()
            iconCache.put(packageName, imageBitmap)
            imageBitmap
        }
    }

    private fun applyFilters(
        items: List<ExportedActivityItem>,
        criteria: FilterCriteria
    ): List<ExportedActivityItem> {
        val trimmedQuery = criteria.query.trim().lowercase(Locale.ROOT)
        return items.filter { item ->
            val matchesAppType = when (criteria.appType) {
                AppTypeFilter.ALL -> true
                AppTypeFilter.USER_ONLY -> !item.isSystemApp
                AppTypeFilter.SYSTEM_ONLY -> item.isSystemApp
            }
            if (!matchesAppType) return@filter false

            val matchesStatus = when (criteria.status) {
                StatusFilter.ALL -> true
                StatusFilter.NO_PERMISSION -> item.hasNoPermission
                StatusFilter.HAS_PERMISSION -> !item.hasNoPermission
                StatusFilter.LAUNCHED -> item.testStatus is TestStatus.Launched
                StatusFilter.DENIED -> item.testStatus is TestStatus.Denied
            }
            if (!matchesStatus) return@filter false

            if (trimmedQuery.isEmpty()) return@filter true

            item.appName.lowercase(Locale.ROOT).contains(trimmedQuery) ||
                item.packageName.lowercase(Locale.ROOT).contains(trimmedQuery) ||
                item.activityName.lowercase(Locale.ROOT).contains(trimmedQuery) ||
                (item.permission?.lowercase(Locale.ROOT)?.contains(trimmedQuery) == true) ||
                (item.hasNoPermission && "tanpa izin".contains(trimmedQuery))
        }
    }

    class Factory(
        private val repository: PackageScannerRepository,
        private val rootDetector: RootStatusDetector,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, rootDetector, preferencesRepository) as T
        }
    }
}
