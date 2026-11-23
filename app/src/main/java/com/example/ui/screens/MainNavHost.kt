package com.example.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.PjokViewModel

object AppRoutes {
    const val BERANDA = "beranda"
    const val SISWA = "siswa"
    const val ABSENSI = "absensi"
    const val REKAP = "rekap"
    const val PENGATURAN = "pengaturan"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val bottomTabs = listOf(
    BottomNavTab(
        route = AppRoutes.BERANDA,
        label = "Beranda",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_beranda"
    ),
    BottomNavTab(
        route = AppRoutes.SISWA,
        label = "Siswa",
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups,
        testTag = "nav_siswa"
    ),
    BottomNavTab(
        route = AppRoutes.ABSENSI,
        label = "Absensi",
        selectedIcon = Icons.AutoMirrored.Filled.FactCheck,
        unselectedIcon = Icons.AutoMirrored.Outlined.FactCheck,
        testTag = "nav_absensi"
    ),
    BottomNavTab(
        route = AppRoutes.REKAP,
        label = "Rekap",
        selectedIcon = Icons.Filled.Assessment,
        unselectedIcon = Icons.Outlined.Assessment,
        testTag = "nav_rekap"
    )
)

@Composable
fun MainAppRoot(
    viewModel: PjokViewModel
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val attendanceState by viewModel.attendanceState.collectAsStateWithLifecycle()
    val recapFilter by viewModel.recapFilter.collectAsStateWithLifecycle()
    val recapReport by viewModel.recapReport.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != AppRoutes.PENGATURAN

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceCard,
                    tonalElevation = 6.dp
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyPrimary,
                                selectedTextColor = NavyPrimary,
                                indicatorColor = EmeraldContainer,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.BERANDA,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.BERANDA) {
                DashboardScreen(
                    settings = settings,
                    classes = classes,
                    summary = dashboardSummary,
                    onStartAttendance = { classId ->
                        viewModel.loadAttendanceForClass(classId, meetingId = null)
                        navigateToTab(AppRoutes.ABSENSI)
                    },
                    onOpenMeeting = { classId, meetingId ->
                        viewModel.loadAttendanceForClass(classId, meetingId = meetingId)
                        navigateToTab(AppRoutes.ABSENSI)
                    },
                    onOpenSettings = {
                        navController.navigate(AppRoutes.PENGATURAN)
                    }
                )
            }
            composable(AppRoutes.SISWA) {
                StudentsScreen(
                    students = students,
                    viewModel = viewModel
                )
            }
            composable(AppRoutes.ABSENSI) {
                AttendanceScreen(
                    state = attendanceState,
                    settings = settings,
                    viewModel = viewModel
                )
            }
            composable(AppRoutes.REKAP) {
                RecapScreen(
                    filter = recapFilter,
                    report = recapReport,
                    settings = settings,
                    viewModel = viewModel
                )
            }
            composable(AppRoutes.PENGATURAN) {
                SettingsScreen(
                    settings = settings,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
