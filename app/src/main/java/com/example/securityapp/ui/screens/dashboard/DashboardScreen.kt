package com.example.securityapp.ui.screens.dashboard

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securityapp.ui.theme.*
import com.example.securityapp.ui.utils.bounceClick
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onNavigateToScan: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToDataUsage: () -> Unit = {},
    onNavigateToBattery: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBase)) {
        // Aesthetic Glow in background
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .blur(80.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header
            DashboardHeader(onProfileClick = { showAboutDialog = true })

            when (val state = uiState) {
                is DashboardUiState.Loading, is DashboardUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is DashboardUiState.Error -> {
                    Text(
                        "Error: ${state.message}", 
                        color = Danger, 
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is DashboardUiState.Success -> {
                    // Score Hero
                    SecurityScoreHero(score = state.score, status = state.status)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bento Grid Stats
                    DashboardBentoGrid(
                        highRiskCount = state.highRiskCount,
                        dataGb = state.dataTodayGb,
                        onHighRiskClick = onNavigateToPermissions,
                        onDataUsageClick = onNavigateToDataUsage
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons Cluster
                    ActionButtonsSection(
                        onScan = { 
                            viewModel.onQuickScanClicked()
                            onNavigateToScan() 
                        },
                        onPermissions = onNavigateToPermissions,
                        onDataUsage = onNavigateToDataUsage
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Network Integrity Bonus Section
                    NetworkIntegritySection(onWifiClick = onNavigateToDataUsage)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Developer Watermark
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEVELOPED BY ANVIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant.copy(alpha = 0.4f),
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(68.dp)) // Padding for bottom nav
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(onProfileClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "SCANMATRIX",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Primary,
                letterSpacing = 4.sp
            )
        }
        
        // Profile Mockup
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceHigh)
                .padding(1.dp)
                .clip(CircleShape)
                .background(SurfaceBright)
                .bounceClick { onProfileClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).size(20.dp)
            )
        }
    }
}

@Composable
fun SecurityScoreHero(score: Int, status: String) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "score"
    )
    
    val animatedSweep by animateFloatAsState(
        targetValue = score * 3.6f, // 360 degrees
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "sweep"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Outer Ring (Decorative)
            Canvas(modifier = Modifier.size(220.dp).alpha(0.3f)) {
                drawCircle(
                    color = SurfaceHigh,
                    style = Stroke(width = 12.dp.toPx())
                )
            }
            
            // Progress Ring
            Canvas(modifier = Modifier.size(220.dp)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Primary,
                        0.85f to Primary,
                        1f to Color.Transparent
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner Content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedScore",
                    style = MaterialTheme.typography.displayLarge,
                    color = Primary
                )
                Text(
                    text = "SECURITY INDEX",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Chip
        Surface(
            color = Danger.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Danger,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    color = Danger,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "System scan identified vulnerabilities in permissions and background data.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun DashboardBentoGrid(
    highRiskCount: Int,
    dataGb: Float,
    onHighRiskClick: () -> Unit = {},
    onDataUsageClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BentoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.GppMaybe,
            value = "$highRiskCount",
            label = "High Risk Apps",
            iconBg = Danger.copy(alpha = 0.2f),
            iconTint = Danger,
            onClick = onHighRiskClick
        )
        BentoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.DataUsage,
            value = "%.1f".format(dataGb),
            unit = "GB",
            label = "Data Today",
            iconBg = Secondary.copy(alpha = 0.2f),
            iconTint = Secondary,
            onClick = onDataUsageClick
        )
    }
}

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    unit: String = "",
    label: String,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        color = SurfaceHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurface
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = " $unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    onScan: () -> Unit,
    onPermissions: () -> Unit,
    onDataUsage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "THREAT RESPONSE",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = "Quick Scan",
                icon = Icons.Default.Bolt,
                containerColor = Primary,
                contentColor = Color.White,
                onClick = onScan
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                label = "Permissions",
                icon = Icons.Default.LockReset,
                containerColor = SurfaceHigh,
                contentColor = OnSurface,
                onClick = onPermissions
            )
        }

        QuickActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = "Optimize Data Usage",
            icon = Icons.Default.SignalCellularNoSim,
            containerColor = SurfaceHigh,
            contentColor = OnSurface,
            onClick = onDataUsage
        )
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp).bounceClick(),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NetworkIntegritySection(onWifiClick: () -> Unit = {}) {
    Surface(
        onClick = onWifiClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = SurfaceHigh,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NETWORK INTEGRITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Wi-Fi Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Success, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Encrypted Connection Detected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
