package com.example.securityapp.ui.screens.battery

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import com.example.securityapp.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securityapp.data.repository.AppBatteryUsage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryAppDetailScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pm = context.packageManager

    var appUsage by remember { mutableStateOf<AppBatteryUsage?>(null) }
    var appName by remember { mutableStateOf(packageName) }
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is BatteryUiState.Success) {
            val successState = uiState as BatteryUiState.Success
            appUsage = successState.appUsageList.find { it.packageName == packageName }
        }
        
        try {
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appName = pm.getApplicationLabel(appInfo).toString()
            appIcon = pm.getApplicationIcon(packageName)
        } catch (e: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Battery Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingVals ->
        if (appUsage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingVals)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // App Header
                if (appIcon != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(drawable = appIcon),
                        contentDescription = appName,
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )
                } else {
                    Box(modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Animated usage breakdown
                UsageInfoCard(appUsage!!)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Animated Bar Chart (mocked up distribution for demonstration)
                HourlyUsageChart(appUsage!!)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Actions
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_USAGE_SETTINGS)
                        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Icon(Icons.Default.BatterySaver, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restrict Background Usage")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open App Settings")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Uninstall App")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        } else if (uiState is BatteryUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingVals), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingVals), contentAlignment = Alignment.Center) {
                Text("App data not found. It may have been uninstalled or used no battery.", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun UsageInfoCard(usage: AppBatteryUsage) {
    val hrs = TimeUnit.MILLISECONDS.toHours(usage.foregroundTimeMs)
    val mins = TimeUnit.MILLISECONDS.toMinutes(usage.foregroundTimeMs) - TimeUnit.HOURS.toMinutes(hrs)
    
    val timeString = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Usage Breakdown", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Foreground Time", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timeString, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Drain", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("%.1f%%", usage.batteryPercentageConsumed), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun HourlyUsageChart(usage: AppBatteryUsage) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }
    
    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "chart_anim"
    )

    // Generate some mock distribution for visual effect based on total time
    val dataPoints = remember {
        List(24) { (Math.random() * usage.batteryPercentageConsumed).toFloat() }
    }
    
    val maxVal = dataPoints.maxOrNull() ?: 1f

    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text("Last 24 Hours Activity", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width / (dataPoints.size * 1.5f)
                val spacing = barWidth * 0.5f
                
                dataPoints.forEachIndexed { index, value ->
                    val x = index * (barWidth + spacing)
                    val barHeight = (value / maxVal) * size.height * animProgress
                    
                    drawRoundRect(
                        color = if (value > maxVal * 0.7f) Danger else primaryColor,
                        topLeft = androidx.compose.ui.geometry.Offset(x, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
    }
}
