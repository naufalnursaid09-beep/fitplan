package com.fitplan.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitplan.app.ui.common.ScreenColumn
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.home.HomeScreen
import com.fitplan.app.ui.schedule.ScheduleScreen
import com.fitplan.app.ui.settings.SettingsScreen
import com.fitplan.app.ui.theme.FitPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitPlanTheme {
                AskNotificationPermissionOnce()
                FitPlanRoot()
            }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Hari Ini", Icons.Filled.Home),
    Schedule("schedule", "Jadwal", Icons.Filled.CalendarMonth),
    Food("food", "Makanan", Icons.Filled.Restaurant),
    Progress("progress", "Progres", Icons.Filled.ShowChart),
    Settings("settings", "Atur", Icons.Filled.Settings),
}

@Composable
private fun FitPlanRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun go(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { go(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = nav, startDestination = Tab.Home.route) {
                composable(Tab.Home.route) {
                    HomeScreen(onOpenSettings = { go(Tab.Settings.route) })
                }
                composable(Tab.Schedule.route) { ScheduleScreen() }
                composable(Tab.Food.route) { ComingSoon("Makanan", "tahap (d)") }
                composable(Tab.Progress.route) { ComingSoon("Progres", "tahap (e)") }
                composable(Tab.Settings.route) { SettingsScreen() }
            }
        }
    }
}

/** Android 13+: minta izin notifikasi saat aplikasi pertama kali dibuka. */
@Composable
private fun AskNotificationPermissionOnce() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun ComingSoon(title: String, stage: String) {
    ScreenColumn(title) {
        SectionCard {
            Text("Fitur ini sedang dibangun ($stage).", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
