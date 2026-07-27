package com.example.securityapp.ui.screens.network

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.securityapp.ui.screens.connections.ConnectionLog

@Composable
fun NetworkScreen(viewModel: NetworkViewModel = hiltViewModel()) {
    val scrollState = rememberScrollState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val dnsLogs by viewModel.dnsLogs.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBase)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {
            NetworkHeader()

            // Hero Section: Connection Status
            ConnectionMonitorHero(isVpnActive) { enabled ->
                viewModel.toggleVpn(enabled)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Threat Score Gauge & Info
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThreatScoreCard(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Live Connection Log
            LiveTrafficSection(dnsLogs)

            Spacer(modifier = Modifier.height(32.dp))

            // Network Stats Bento Grid
            NetworkStatsGrid()
        }
    }
}

@Composable
fun NetworkHeader() {
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
fun ConnectionMonitorHero(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = SurfaceLow,
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Large background icon
            Icon(
                Icons.Default.Hub,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .size(160.dp)
                    .alpha(0.05f),
                tint = Secondary
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "SECURITY ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    letterSpacing = 2.sp
                )
                Text(
                    "Connection Monitor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulseScale)
                                .background(Secondary.copy(alpha = 0.3f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Secondary, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "ACTIVE & GUARDED",
                        style = MaterialTheme.typography.labelMedium,
                        color = Secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern Toggle Switch
                Surface(
                    color = SurfaceHigh,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.bounceClick { onToggle(!isActive) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isActive) "Monitor ON" else "Monitor OFF",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isActive,
                            onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnPrimary,
                                checkedTrackColor = Primary,
                                uncheckedThumbColor = OnSurfaceVariant,
                                uncheckedTrackColor = SurfaceBright
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreatScoreCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SurfaceHigh,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SurfaceBright,
                        style = Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.linearGradient(listOf(Primary, Secondary)),
                        startAngle = -90f,
                        sweepAngle = 330f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("98", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("THREAT SCORE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Network integrity is currently at optimal levels.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LiveTrafficSection(logs: List<ConnectionLog>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Live Traffic Analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge("REAL-TIME", SurfaceHigh, OnSurfaceVariant)
                StatusBadge("UDP/TCP", Secondary.copy(alpha = 0.1f), Secondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp).background(SurfaceHigh.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for connection data...", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                logs.takeLast(10).reversed().forEach { log ->
                    TrafficLogItem(
                        domain = log.domain,
                        type = if (log.isSuspicious) "HTTPS • PORT 53" else "QUIC • Port 443",
                        status = if (log.isSuspicious) "BLOCKED" else "VERIFIED",
                        accent = if (log.isSuspicious) Danger else Success,
                        icon = if (log.isSuspicious) Icons.Default.Dangerous else Icons.Default.VerifiedUser
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficLogItem(domain: String, type: String, status: String, accent: Color, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceHigh.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accent,
                        size = Size(4.dp.toPx(), size.height),
                        topLeft = Offset(0f, 0f)
                    )
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(domain, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (accent == Danger) Danger else OnSurface)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Text(type, style = MaterialTheme.typography.labelSmall, color = if (accent == Danger) Danger.copy(alpha = 0.8f) else OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(3.dp).background(if (accent == Danger) Danger.copy(alpha = 0.3f) else OnSurfaceVariant.copy(alpha = 0.4f), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(status, style = MaterialTheme.typography.labelSmall, color = if (accent == Danger) Danger.copy(alpha = 0.8f) else OnSurfaceVariant)
                    }
                }
            }
            Icon(
                if (accent == Success) Icons.Default.CheckCircle else if (accent == Danger) Icons.Default.Cancel else Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, shape = CircleShape) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
fun NetworkStatsGrid() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NetworkStatCard(Modifier.weight(1f), "Bandwidth", "12.4 Mbps", Icons.Default.Speed, Primary)
            NetworkStatCard(Modifier.weight(1f), "Nodes Active", "24 Nodes", Icons.Default.Router, Secondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NetworkStatCard(Modifier.weight(1f), "Threats Today", "03 Intercepted", Icons.Default.SecurityUpdateWarning, Danger)
            NetworkStatCard(Modifier.weight(1f), "SSL Uptime", "100%", Icons.Default.LockReset, Success)
        }
    }
}

@Composable
fun NetworkStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Surface(
        modifier = modifier,
        color = SurfaceLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
        }
    }
}
