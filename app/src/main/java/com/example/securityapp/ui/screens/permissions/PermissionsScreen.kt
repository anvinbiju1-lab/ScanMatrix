package com.example.securityapp.ui.screens.permissions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.securityapp.data.repository.AppDomainModel
import com.example.securityapp.data.repository.RiskBadge
import com.example.securityapp.ui.theme.*
import com.example.securityapp.ui.utils.bounceClick

@Composable
fun PermissionsScreen(
    navController: NavController,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trustedApps by viewModel.trustedApps.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(SurfaceBase)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PermissionsHeader()

            when (val state = uiState) {
                is PermissionsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is PermissionsUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Hero: Privacy Health
                        PrivacyHealthHero()

                        Spacer(modifier = Modifier.height(24.dp))

                        // Filter Chips
                        FilterSection(
                            currentFilter = state.currentFilter,
                            onFilterSelected = { viewModel.filterApps(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Insights / Auto-Revoke Banner
                        AutoRevokeBanner()

                        Spacer(modifier = Modifier.height(24.dp))

                        // App List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            item {
                                Text(
                                    "APP ECOSYSTEM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    letterSpacing = 2.sp
                                )
                            }
                            items(state.apps) { app ->
                                AppRiskCard(
                                    app = app,
                                    isTrusted = trustedApps.contains(app.packageName),
                                    onClick = { navController.navigate("app_detail/${app.packageName}") }
                                )
                            }
                        }
                    }
                }
                is PermissionsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = Danger)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsHeader() {
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
fun PrivacyHealthHero() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = SurfaceLow,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 84% Secure Gauge
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = SurfaceHigh, style = Stroke(width = 8.dp.toPx()))
                    drawArc(
                        brush = Brush.linearGradient(listOf(Primary, Success)),
                        startAngle = -90f,
                        sweepAngle = 302f, // 84%
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("84%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("SECURE", style = MaterialTheme.typography.labelSmall, color = Success, fontWeight = FontWeight.Bold)
                }
            }
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "3 apps require immediate review due to excessive background access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FilterSection(currentFilter: PermissionFilter, onFilterSelected: (PermissionFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(PermissionFilter.values()) { filter ->
            val isSelected = currentFilter == filter
            
            Surface(
                modifier = Modifier.bounceClick { onFilterSelected(filter) },
                color = if (isSelected) Primary.copy(alpha = 0.1f) else SurfaceHigh,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.5f)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        // Pulsing Dot
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                            label = "alpha"
                        )
                        Box(modifier = Modifier.size(6.dp).alpha(pulseAlpha).background(Primary, CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = filter.name.replace("_", " "),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Primary else OnSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AutoRevokeBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .bounceClick {
                try {
                    // Try to open the system's auto-revoke / unused permissions setting
                    val intent = android.content.Intent("android.intent.action.AUTO_REVOKE_PERMISSIONS").apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general app settings if specific intent fails
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            },
        color = SurfaceHigh,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(Success.copy(alpha = 1f), CircleShape), // Solid color for better visibility
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Automatic Revoke Active", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("Guarding against idle permissions", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
        }
    }
}

@Composable
fun AppRiskCard(app: AppDomainModel, isTrusted: Boolean, onClick: () -> Unit) {
    val badgeColor = when (app.riskBadge) {
        RiskBadge.HIGH -> Danger
        RiskBadge.MEDIUM -> Warning
        RiskBadge.LOW -> Success
    }
    val badgeText = when (app.riskBadge) {
        RiskBadge.HIGH -> "CRITICAL"
        RiskBadge.MEDIUM -> "MODERATE"
        RiskBadge.LOW -> "SECURE"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .bounceClick { onClick() },
        color = SurfaceHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        border = if (app.riskBadge == RiskBadge.HIGH) androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = app.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceBright)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${app.grantedPermissions.size} Permissions active",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
                if (isTrusted) {
                    Text("Verified Ecosystem", style = MaterialTheme.typography.labelSmall, color = Primary, modifier = Modifier.padding(top = 2.dp), fontWeight = FontWeight.Bold)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = badgeColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
