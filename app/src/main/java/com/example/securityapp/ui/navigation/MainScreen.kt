package com.example.securityapp.ui.navigation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.securityapp.ui.screens.connections.ConnectionsScreen
import com.example.securityapp.ui.screens.dashboard.DashboardScreen
import com.example.securityapp.ui.screens.network.NetworkScreen
import com.example.securityapp.ui.screens.onboarding.OnboardingScreen
import com.example.securityapp.ui.screens.permissions.AppDetailScreen
import com.example.securityapp.ui.screens.permissions.PermissionsScreen
import com.example.securityapp.ui.screens.scan.ScanScreen
import com.example.securityapp.ui.screens.battery.BatteryUsageScreen
import com.example.securityapp.ui.screens.battery.BatteryAppDetailScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var showOnboarding by remember {
        mutableStateOf(sharedPrefs.getBoolean("show_onboarding", true))
    }

    if (showOnboarding) {
        OnboardingScreen(onFinishOnboarding = {
            sharedPrefs.edit().putBoolean("show_onboarding", false).apply()
            showOnboarding = false
        })
    } else {
        Scaffold(
            bottomBar = { BottomNavigationBar(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                exitTransition = { fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                popEnterTransition = { fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                popExitTransition = { fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
            ) {                
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onNavigateToScan = { navController.navigate("${Screen.Scan.route}?autoStart=true") },
                        onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                        onNavigateToDataUsage = { navController.navigate(Screen.Network.route) }
                    )
                }
                composable(Screen.Permissions.route) { PermissionsScreen(navController = navController) }
                composable(Screen.Network.route) { NetworkScreen() }
                composable(Screen.Scan.route) {
                    ScanScreen(autoStart = false)
                }
                composable(
                    route = "${Screen.Scan.route}?autoStart={autoStart}",
                    arguments = listOf(navArgument("autoStart") {
                        type = NavType.BoolType
                        defaultValue = false
                    })
                ) { backStackEntry ->
                    val autoStart = backStackEntry.arguments?.getBoolean("autoStart") ?: false
                    ScanScreen(autoStart = autoStart)
                }
                composable(Screen.Connections.route) { ConnectionsScreen() }
                composable(Screen.BatteryUsage.route) {
                    BatteryUsageScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onAppClick = { packageName ->
                            navController.navigate(Screen.BatteryDetail.createRoute(packageName))
                        }
                    )
                }
                composable(
                    route = Screen.BatteryDetail.route,
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
                    BatteryAppDetailScreen(
                        packageName = packageName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "app_detail/{packageName}",
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
                    AppDetailScreen(
                        packageName = packageName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "icon_scale"
                )
                
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    animationSpec = tween(300),
                    label = "icon_color"
                )
                
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f) // Ensure each item takes equal space
                        .clip(CircleShape)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            if (!isSelected) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated Pill Background
                    this@Row.AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                        exit = fadeOut(tween(200)) + scaleOut(tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp * scale)
                        )
                        this@Row.AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                text = screen.title,
                                color = iconColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
