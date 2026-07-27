package com.example.securityapp.ui.screens.battery

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securityapp.data.repository.AppBatteryUsage
import com.example.securityapp.data.repository.BatteryStateSnapshot
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryUsageScreen(
    onNavigateBack: () -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndLoadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        CenterAlignedTopAppBar(
            title = { Text("Battery Usage", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        when (val state = uiState) {
            is BatteryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is BatteryUiState.PermissionRequired -> {
                PermissionRequiredView(viewModel)
            }
            is BatteryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is BatteryUiState.Success -> {
                BatteryContent(state, viewModel, onAppClick)
            }
        }
    }
}

@Composable
private fun PermissionRequiredView(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryAlert,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Usage Access Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "ScanMatrix needs Usage Access permission to analyze per-app battery drain correctly. We only process this data locally.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.requestUsagePermission(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.checkPermissionAndLoadData() }) {
            Text("I've already granted it")
        }
    }
}

@Composable
private fun BatteryContent(
    state: BatteryUiState.Success, 
    viewModel: BatteryViewModel,
    onAppClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            BatterySummaryCard(state.snapshot)
        }
        
        item {
            TimeframeFilters(state.currentTimeframe, viewModel::setTimeframeFilter)
        }
        
        if (state.appUsageList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No significant battery usage detected for this period.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            itemsIndexed(state.appUsageList) { index, appUsage ->
                AnimatedAppUsageItem(appUsage, index, onAppClick)
            }
        }
    }
}

@Composable
private fun BatterySummaryCard(snapshot: BatteryStateSnapshot) {
    var animationPlayed by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) snapshot.level / 100f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "battery_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Background Track
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawArc(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // Progress Arc
                val arcColor = when {
                    snapshot.level > 20 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                }
                
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawArc(
                        color = arcColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (snapshot.isCharging) "Charging" else "Discharging",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BatteryStatItem("Health", snapshot.healthStatus, Icons.Default.Favorite)
                BatteryStatItem("Temp", "${snapshot.temperature}°C", Icons.Default.Thermostat)
            }
        }
    }
}

@Composable
private fun BatteryStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TimeframeFilters(currentFilter: TimeframeFilter, onFilterSelect: (TimeframeFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TimeframeFilter.values()) { filter ->
            val isSelected = currentFilter == filter
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "chip_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "chip_text"
            )
            
            Surface(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(18.dp),
                color = bgColor,
                onClick = { onFilterSelect(filter) }
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(filter.label, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun AnimatedAppUsageItem(appUsage: AppBatteryUsage, index: Int, onAppClick: (String) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val pm = context.packageManager
    
    val appName = remember(appUsage.packageName) {
        try {
            val appInfo = pm.getApplicationInfo(appUsage.packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appUsage.packageName
        }
    }
    
    val appIcon = remember(appUsage.packageName) {
        try {
            pm.getApplicationIcon(appUsage.packageName)
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            onClick = { onAppClick(appUsage.packageName) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(drawable = appIcon),
                        contentDescription = appName,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    )
                } else {
                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(appName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    
                    val isHighDrain = appUsage.batteryPercentageConsumed > 20f
                    val badgeColor = if (isHighDrain) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Animated Usage Bar
                    var barAnimationPlayed by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { barAnimationPlayed = true }
                    val animatedWidth by animateFloatAsState(
                        targetValue = if (barAnimationPlayed) appUsage.batteryPercentageConsumed / 100f else 0f,
                        animationSpec = tween(1000, easing = FastOutSlowInEasing),
                        label = "usage_bar"
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(animatedWidth).fillMaxHeight().background(badgeColor, RoundedCornerShape(2.dp)))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    String.format("%.1f%%", appUsage.batteryPercentageConsumed),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
