package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PackageScannerRepository
import com.example.data.RootStatusDetector
import com.example.data.UserPreferencesRepository
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.components.OnboardingDisclaimerScreen
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.theme.ActiScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appCtx = applicationContext
        val repository = PackageScannerRepository(appCtx)
        val rootDetector = RootStatusDetector(appCtx)
        val preferencesRepository = UserPreferencesRepository(appCtx)

        setContent {
            ActiScanTheme {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(
                        repository = repository,
                        rootDetector = rootDetector,
                        preferencesRepository = preferencesRepository
                    )
                )
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
                var pendingExportFilteredOnly by remember { mutableStateOf(false) }
                var showOnboardingPrivacy by remember { mutableStateOf(false) }

                val createTxtDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/plain")
                ) { uri ->
                    if (uri != null) {
                        mainViewModel.exportReport(
                            uri = uri,
                            exportFilteredOnly = pendingExportFilteredOnly
                        )
                    }
                }

                when (uiState.disclaimerAccepted) {
                    null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    false -> {
                        OnboardingDisclaimerScreen(
                            onAccept = { mainViewModel.acceptDisclaimer() },
                            onOpenPrivacyPolicy = { showOnboardingPrivacy = true }
                        )
                        if (showOnboardingPrivacy) {
                            PrivacyPolicyDialog(
                                onDismiss = { showOnboardingPrivacy = false }
                            )
                        }
                    }

                    true -> {
                        MainScreen(
                            viewModel = mainViewModel,
                            onRequestExportTxt = { exportFilteredOnly ->
                                pendingExportFilteredOnly = exportFilteredOnly
                                createTxtDocumentLauncher.launch(
                                    mainViewModel.suggestedExportFileName()
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
