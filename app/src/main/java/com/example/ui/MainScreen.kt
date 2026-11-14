package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AppTypeFilter
import com.example.model.ExportedActivityItem
import com.example.model.RootStatus
import com.example.model.StatusFilter
import com.example.ui.components.ActivityItemCard
import com.example.ui.components.DisclaimerInfoDialog
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.LocalAuditStatusColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestExportTxt: (exportFilteredOnly: Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportChoiceDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MainUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryGlow, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.06f),
                        radius = size.width * 0.85f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlow, Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.35f),
                        radius = size.width * 0.7f
                    )
                )
            },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Radar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("privacy_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PrivacyTip,
                            contentDescription = stringResource(R.string.privacy_policy_cd),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings_cd),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { viewModel.scanPackages() },
                        enabled = !state.isScanning,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.rescan_cd),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (state.hasActiveFilters && state.filteredItems.size != state.allItems.size) {
                                showExportChoiceDialog = true
                            } else {
                                onRequestExportTxt(false)
                            }
                        },
                        enabled = !state.isScanning && !state.isExporting && state.allItems.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(40.dp)
                            .testTag("export_txt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = stringResource(R.string.export_txt_cd),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.export_txt_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Telemetry Strip + Root Status Badge + Safety Notice + Search + Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStrip(stats = state.stats)

                AnimatedVisibility(visible = state.showRootStatus) {
                    RootStatusBadge(rootStatus = state.rootDetection.status)
                }

                SafetyBanner()

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.clear_search_cd)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input")
                )

                FilterChipsBar(
                    statusFilter = state.statusFilter,
                    appTypeFilter = state.appTypeFilter,
                    onStatusFilterChange = viewModel::onStatusFilterChange,
                    onAppTypeFilterChange = viewModel::onAppTypeFilterChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area
            when {
                state.isScanning -> {
                    ScanningState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                state.scanError != null -> {
                    ErrorState(
                        error = state.scanError!!,
                        onRetry = viewModel::scanPackages,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                state.filteredItems.isEmpty() -> {
                    EmptyResultsState(
                        onResetFilters = {
                            viewModel.onSearchQueryChange("")
                            viewModel.onStatusFilterChange(StatusFilter.ALL)
                            viewModel.onAppTypeFilterChange(AppTypeFilter.ALL)
                        },
                        hasActiveFilters = state.hasActiveFilters,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                else -> {
                    val onCopy: (ExportedActivityItem) -> Unit = { item ->
                        val componentStr = "${item.packageName}/${item.activityName}"
                        clipboardManager.setText(AnnotatedString(componentStr))
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.copied_component, item.shortActivityName)
                            )
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideLayout = maxWidth >= 720.dp
                        if (isWideLayout) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("exported_activities_grid")
                            ) {
                                itemsIndexed(
                                    items = state.filteredItems,
                                    key = { _, item -> item.id }
                                ) { index, item ->
                                    ActivityItemCard(
                                        item = item,
                                        index = index,
                                        loadIcon = viewModel::getAppIcon,
                                        onTestClick = { viewModel.testSingleActivity(item) },
                                        onCopyComponent = { onCopy(item) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("exported_activities_list")
                            ) {
                                itemsIndexed(
                                    items = state.filteredItems,
                                    key = { _, item -> item.id }
                                ) { index, item ->
                                    ActivityItemCard(
                                        item = item,
                                        index = index,
                                        loadIcon = viewModel::getAppIcon,
                                        onTestClick = { viewModel.testSingleActivity(item) },
                                        onCopyComponent = { onCopy(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showExportChoiceDialog = false },
            title = { Text("Export Laporan Audit .txt") },
            text = {
                Text(
                    "Filter sedang aktif. Pilih data yang ingin disimpan ke file .txt:"
                )
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showExportChoiceDialog = false
                        onRequestExportTxt(true)
                    }
                ) {
                    Text("Hasil Filter (${state.filteredItems.size})")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportChoiceDialog = false
                        onRequestExportTxt(false)
                    }
                ) {
                    Text("Semua (${state.allItems.size})")
                }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            showRootStatus = state.showRootStatus,
            rootDetection = state.rootDetection,
            onToggleShowRootStatus = viewModel::setShowRootStatus,
            onOpenPrivacyPolicy = { showPrivacyDialog = true },
            onOpenDisclaimer = { showDisclaimerDialog = true },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showDisclaimerDialog) {
        DisclaimerInfoDialog(onDismiss = { showDisclaimerDialog = false })
    }
}

@Composable
private fun RootStatusBadge(rootStatus: RootStatus) {
    val statusColors = LocalAuditStatusColors.current

    val container: Color
    val content: Color
    val border: Color
    val label: String
    val icon = when (rootStatus) {
        RootStatus.ROOTED -> {
            container = statusColors.warningContainer
            content = statusColors.onWarningContainer
            border = statusColors.warning.copy(alpha = 0.45f)
            label = stringResource(R.string.root_status_rooted)
            Icons.Rounded.WarningAmber
        }
        RootStatus.NON_ROOTED -> {
            container = statusColors.launchedContainer
            content = statusColors.onLaunchedContainer
            border = statusColors.launched.copy(alpha = 0.45f)
            label = stringResource(R.string.root_status_non_root)
            Icons.Rounded.VerifiedUser
        }
        RootStatus.UNKNOWN -> {
            container = statusColors.neutralContainer
            content = statusColors.onNeutralContainer
            border = statusColors.neutral.copy(alpha = 0.4f)
            label = stringResource(R.string.root_status_unknown)
            Icons.Rounded.Shield
        }
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("root_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
        }
    }
}

@Composable
private fun TelemetryStrip(stats: ScanStats) {
    val statusColors = LocalAuditStatusColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TelemetryCard(
            label = stringResource(R.string.stat_exported),
            value = stats.totalExported.toString(),
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            label = stringResource(R.string.stat_no_perm),
            value = stats.noPermissionCount.toString(),
            accentColor = statusColors.warning,
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            label = stringResource(R.string.stat_launched),
            value = stats.launchedCount.toString(),
            accentColor = statusColors.launched,
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            label = stringResource(R.string.stat_denied),
            value = stats.deniedCount.toString(),
            accentColor = statusColors.denied,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TelemetryCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SafetyBanner() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.safety_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.safety_banner_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilterChipsBar(
    statusFilter: StatusFilter,
    appTypeFilter: AppTypeFilter,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onAppTypeFilterChange: (AppTypeFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = statusFilter == StatusFilter.ALL,
                onClick = { onStatusFilterChange(StatusFilter.ALL) },
                label = { Text(stringResource(R.string.filter_all)) },
                modifier = Modifier.testTag("filter_all")
            )
        }
        item {
            FilterChip(
                selected = statusFilter == StatusFilter.NO_PERMISSION,
                onClick = { onStatusFilterChange(StatusFilter.NO_PERMISSION) },
                label = { Text(stringResource(R.string.filter_no_permission)) },
                modifier = Modifier.testTag("filter_no_permission")
            )
        }
        item {
            FilterChip(
                selected = statusFilter == StatusFilter.HAS_PERMISSION,
                onClick = { onStatusFilterChange(StatusFilter.HAS_PERMISSION) },
                label = { Text(stringResource(R.string.filter_has_permission)) },
                modifier = Modifier.testTag("filter_has_permission")
            )
        }
        item {
            FilterChip(
                selected = statusFilter == StatusFilter.LAUNCHED,
                onClick = { onStatusFilterChange(StatusFilter.LAUNCHED) },
                label = { Text(stringResource(R.string.filter_launched)) },
                modifier = Modifier.testTag("filter_launched")
            )
        }
        item {
            FilterChip(
                selected = statusFilter == StatusFilter.DENIED,
                onClick = { onStatusFilterChange(StatusFilter.DENIED) },
                label = { Text(stringResource(R.string.filter_denied)) },
                modifier = Modifier.testTag("filter_denied")
            )
        }
        item {
            FilterChip(
                selected = appTypeFilter == AppTypeFilter.USER_ONLY,
                onClick = {
                    onAppTypeFilterChange(
                        if (appTypeFilter == AppTypeFilter.USER_ONLY) {
                            AppTypeFilter.ALL
                        } else {
                            AppTypeFilter.USER_ONLY
                        }
                    )
                },
                label = { Text(stringResource(R.string.filter_user_apps)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("filter_user_apps")
            )
        }
        item {
            FilterChip(
                selected = appTypeFilter == AppTypeFilter.SYSTEM_ONLY,
                onClick = {
                    onAppTypeFilterChange(
                        if (appTypeFilter == AppTypeFilter.SYSTEM_ONLY) {
                            AppTypeFilter.ALL
                        } else {
                            AppTypeFilter.SYSTEM_ONLY
                        }
                    )
                },
                label = { Text(stringResource(R.string.filter_system_apps)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("filter_system_apps")
            )
        }
    }
}

@Composable
private fun ScanningState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                text = stringResource(R.string.scanning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.scanning_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = onRetry) {
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
private fun EmptyResultsState(
    hasActiveFilters: Boolean,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = stringResource(R.string.empty_results_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.empty_results_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasActiveFilters) {
                FilledTonalButton(onClick = onResetFilters) {
                    Text("Reset Filter")
                }
            }
        }
    }
}
