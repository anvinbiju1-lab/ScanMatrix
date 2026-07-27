package com.example.securityapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard   : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Permissions : Screen("permissions", "Permissions", Icons.Default.GppGood)
    object Network     : Screen("network", "Network", Icons.Default.Hub)
    object Scan        : Screen("scan", "Scan", Icons.Default.Radar)
    object Connections : Screen("connections", "Connects", Icons.Default.VpnKey)
    
    // Non-nav items
    object BatteryUsage : Screen("battery_usage", "Battery Usage", Icons.Default.BatteryStd)
    object BatteryDetail : Screen("battery_detail/{packageName}", "Battery Detail", Icons.Default.BatteryStd) {
        fun createRoute(packageName: String) = "battery_detail/$packageName"
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Permissions,
    Screen.Network,
    Screen.Scan,
    Screen.Connections
)
