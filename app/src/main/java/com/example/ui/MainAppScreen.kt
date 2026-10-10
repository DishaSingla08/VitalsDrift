package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddReadingBottomSheet
import com.example.ui.components.MetricExplanationDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DoctorReportScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.AppThemeMode

enum class AppDestination(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Home, "nav_dashboard"),
    ANALYTICS("Velocity & Drift", Icons.AutoMirrored.Filled.TrendingUp, "nav_analytics"),
    HISTORY("History", Icons.Default.History, "nav_history"),
    REPORT("Generate Report", Icons.Default.Description, "nav_report")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: VitalsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var isAddReadingSheetOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back button: return to Dashboard if on sub-screen
    if (currentDestination != AppDestination.DASHBOARD) {
        BackHandler {
            currentDestination = AppDestination.DASHBOARD
        }
    }

    // React to feedback messages
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "VitalsDrift",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        // Theme Toggle Icon Button (Sun / Moon)
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = when (uiState.themeMode) {
                                    AppThemeMode.LIGHT -> Icons.Default.DarkMode
                                    AppThemeMode.DARK -> Icons.Default.LightMode
                                    AppThemeMode.SYSTEM -> Icons.Default.DarkMode
                                },
                                contentDescription = "Toggle Theme (Light / Dark)",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("main_top_app_bar")
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        AppDestination.values().forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination == destination,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = { Text(destination.title, fontSize = 11.sp) },
                                modifier = Modifier.testTag(destination.tag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Wide Screen Navigation Rail
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_nav_rail"),
                        header = {
                            FloatingActionButton(
                                onClick = { isAddReadingSheetOpen = true },
                                modifier = Modifier.padding(vertical = 12.dp).testTag("rail_add_btn"),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Log BP")
                            }
                        }
                    ) {
                        AppDestination.values().forEach { destination ->
                            NavigationRailItem(
                                selected = currentDestination == destination,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title
                                    )
                                },
                                label = { Text(destination.title, fontSize = 11.sp) },
                                modifier = Modifier.testTag(destination.tag)
                            )
                        }
                    }
                }

                // Active Screen Content
                when (currentDestination) {
                    AppDestination.DASHBOARD -> {
                        DashboardScreen(
                            uiState = uiState,
                            onOpenLogSheet = { isAddReadingSheetOpen = true },
                            onSelectRange = { viewModel.setChartRange(it) },
                            onDeleteReading = { viewModel.deleteReading(it) },
                            onNavigateToReport = { currentDestination = AppDestination.REPORT },
                            onExplainMetric = { viewModel.explainMetric(it) },
                            onSyncWearables = { viewModel.syncWearables() }
                        )
                    }
                    AppDestination.ANALYTICS -> {
                        AnalyticsScreen(uiState = uiState)
                    }
                    AppDestination.HISTORY -> {
                        HistoryScreen(
                            uiState = uiState,
                            onDeleteReading = { viewModel.deleteReading(it) },
                            onSeedSampleData = { viewModel.seedSampleData() },
                            onClearAllData = { viewModel.clearAllData() },
                            onOpenLogSheet = { isAddReadingSheetOpen = true }
                        )
                    }
                    AppDestination.REPORT -> {
                        DoctorReportScreen(
                            uiState = uiState,
                            onSelectPeriodDays = { viewModel.setReportPeriodDays(it) },
                            onOpenPdf = { ctx -> viewModel.openPdfReport(ctx) },
                            onSharePdf = { ctx -> viewModel.sharePdfReport(ctx) }
                        )
                    }
                }
            }
        }
    }

    // UI Translation Modal ("Explain This Metric")
    uiState.activeMetricExplanation?.let { metricType ->
        MetricExplanationDialog(
            metricType = metricType,
            onDismiss = { viewModel.dismissMetricExplanation() }
        )
    }

    // Logging Bottom Sheet
    if (isAddReadingSheetOpen) {
        AddReadingBottomSheet(
            onDismiss = { isAddReadingSheetOpen = false },
            onSaveReading = { sys, dia, hr, ctx, tod, arm, notes ->
                viewModel.saveReading(
                    systolic = sys,
                    diastolic = dia,
                    heartRate = hr,
                    activityContext = ctx,
                    timeOfDay = tod,
                    arm = arm,
                    notes = notes
                )
                isAddReadingSheetOpen = false
            }
        )
    }
}
