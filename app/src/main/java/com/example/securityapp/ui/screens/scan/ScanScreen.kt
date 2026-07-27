package com.example.securityapp.ui.screens.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securityapp.data.repository.FullScanSummary
import com.example.securityapp.data.repository.RiskLevel
import com.example.securityapp.data.repository.ScanResult
import com.example.securityapp.data.repository.ThreatType
import com.example.securityapp.ui.theme.*
import com.example.securityapp.ui.utils.bounceClick

@Composable
fun ScanScreen(
    autoStart: Boolean = false,
    onNavigateBack: () -> Unit = {},
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoSanitize by viewModel.autoSanitizeEnabled.collectAsState()
    
    LaunchedEffect(autoStart) {
        if (autoStart) {
            viewModel.startQuickScan()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(SurfaceBase)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScanHeader(onNavigateBack)
            
            Crossfade(targetState = uiState, label = "scan_state") { state ->
                when (state) {
                    is ScanUiState.Idle -> IdleScanContent(
                        onQuickScan = { viewModel.startQuickScan() },
                        onFullScan = { viewModel.startFullScan() }
                    )
                    is ScanUiState.Scanning -> ScanningContent(state)
                    is ScanUiState.Finished -> ScanResultsContent(
                        state = state, 
                        autoSanitizeEnabled = autoSanitize,
                        onAutoSanitizeToggle = { viewModel.toggleAutoSanitize(it) },
                        onRescan = { viewModel.startQuickScan() }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanHeader(onNavigateBack: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "SCANMATRIX",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Primary,
                letterSpacing = 4.sp
            )
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceHigh)
                .padding(2.dp)
                .clip(CircleShape)
                .background(SurfaceBright)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.align(Alignment.Center).size(24.dp))
        }
    }
}

@Composable
fun IdleScanContent(onQuickScan: () -> Unit, onFullScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        DataCoreAnimation(isScanning = false)

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "CORE SHIELD ACTIVE",
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            "Ready for system deep-check",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .bounceClick { onQuickScan() }
                    .background(Brush.linearGradient(listOf(Primary, Secondary)), shape = RoundedCornerShape(16.dp)),
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = OnPrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("START QUICK SCAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = OnPrimary)
                }
            }

            // Full Scan Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .bounceClick { onFullScan() },
                color = SurfaceLow,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridView, contentDescription = null, tint = Secondary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("FULL DEVICE SCAN", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = OnSurface)
                            Text("72% More Thorough", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ScanningContent(state: ScanUiState.Scanning) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        DataCoreAnimation(isScanning = true)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "ANALYZING DATA CORE",
            style = MaterialTheme.typography.labelMedium,
            color = Secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "${(state.progress * 100).toInt()}% COMPLETED",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = OnSurface
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Scanning Stepper Log
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            color = SurfaceLow,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ScanStepItem("Initializing Core Indices", isDone = state.progress > 0.1f)
                ScanStepItem("Analyzing System Permissions", isDone = state.progress > 0.4f)
                ScanStepItem("Deep Storage Verification", isDone = state.progress > 0.8f, current = state.currentFolder)
                ScanStepItem("Neutralizing Ghost Processes", isDone = state.progress >= 1.0f)
            }
        }
    }
}

@Composable
fun ScanResultsContent(
    state: ScanUiState.Finished, 
    autoSanitizeEnabled: Boolean,
    onAutoSanitizeToggle: (Boolean) -> Unit,
    onRescan: () -> Unit
) {
    val highRisk = state.results.count { it.riskLevel == RiskLevel.HIGH }
    val resultColor = if (highRisk > 0) Danger else Success
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = resultColor.copy(alpha = 0.1f), style = Stroke(width = 4.dp.toPx()))
                    }
                    Icon(
                        if (highRisk > 0) Icons.Default.GppMaybe else Icons.Default.Shield,
                        contentDescription = null,
                        tint = resultColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    if (highRisk > 0) "THREATS INTERCEPTED" else "SYSTEM INTEGRITY VERIFIED",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (highRisk > 0) "$highRisk High-risk vulnerabilities identified." else "Your device data core is fully secured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onRescan,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Rescan System", color = OnSurface)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LIVE THREAT LOG", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 2.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-Sanitize", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoSanitizeEnabled, 
                        onCheckedChange = onAutoSanitizeToggle, 
                        modifier = Modifier.scale(0.6f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Success,
                            checkedTrackColor = Success.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        itemsIndexed(state.results) { index, result ->
            ThreatLogItem(result) {
                if (result.app != null) {
                    // Open App Info
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${result.app.packageName}")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else if (result.filePath != null) {
                    // Open File / Folder
                    try {
                        val file = java.io.File(result.filePath)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            // This is a simplified intent, in real apps you need FileProvider
                            // But for this security audit, jumping to settings or generic view is often enough
                            setDataAndType(android.net.Uri.fromFile(file), "*/*")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        // Fallback to app settings if it's a "system setting" threat
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Cannot open: ${result.filePath}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun DataCoreAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "core")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (isScanning) 4000 else 10000, easing = LinearEasing)),
        label = "rotate"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        // Hexagonal Rings (3 layers)
        for (i in 0..2) {
            val scale = 1f - (i * 0.2f)
            val alpha = 0.6f - (i * 0.2f)
            val speedMult = if (i % 2 == 0) 1f else -1.2f
            
            Canvas(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale * if (isScanning) pulse else 1f)
                    .rotate(rotation * speedMult)
                    .alpha(alpha)
            ) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Primary, Secondary, Color.Transparent)),
                    startAngle = 0f,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        
        // Center Core
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(if (isScanning) Secondary.copy(alpha = 0.2f) else Primary.copy(alpha = 0.1f), CircleShape)
                .padding(12.dp)
                .background(if (isScanning) Secondary else Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isScanning) Icons.Default.Radar else Icons.Default.Security,
                contentDescription = null,
                tint = OnPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Floating Particles (Mockup with small points)
        if (isScanning) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Drawing a few random points that orbit or pulse
                // Simplified for now
            }
        }
    }
}

@Composable
fun ScanStepItem(label: String, isDone: Boolean, current: String? = null) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isDone) Success else SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = OnSecondary, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isDone) OnSurface else OnSurfaceVariant,
                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal
            )
            if (!isDone && current != null) {
                Text(
                    current,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ThreatLogItem(result: ScanResult, onClick: () -> Unit) {
    val accent = when (result.riskLevel) {
        RiskLevel.HIGH -> Danger
        RiskLevel.MEDIUM -> Warning
        else -> Success
    }
    val badgeText = when (result.riskLevel) {
        RiskLevel.HIGH -> "DANGER"
        RiskLevel.MEDIUM -> "ALERT"
        else -> "SAFE"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .bounceClick(),
        color = SurfaceHigh.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.fileName ?: result.app?.appName ?: "System Process",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        result.filePath ?: "Encrypted Kernel Memory",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Surface(
                    color = accent.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                }
            }
        }
    }
}
