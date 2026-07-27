package com.example.securityapp.ui.screens.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.example.securityapp.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.securityapp.data.repository.AppDomainModel
import com.example.securityapp.data.repository.RiskBadge
import com.example.securityapp.ui.utils.bounceClick
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    onNavigateBack: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trustedApps by viewModel.trustedApps.collectAsState()
    
    val app = when (uiState) {
        is PermissionsUiState.Success -> (uiState as PermissionsUiState.Success).apps.find { it.packageName == packageName }
        else -> null
    }
    
    if (app == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isTrusted = trustedApps.contains(app.packageName)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Allowed", "Denied", "All")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            AppDetailBottomBar(
                app = app,
                isTrusted = isTrusted,
                onTrustToggle = { viewModel.toggleTrustApp(app.packageName) },
                onUninstall = {
                    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                    context.startActivity(intent)
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                    context.startActivity(intent)
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppDetailHeader(app = app)
                
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
                    when (tab) {
                        0 -> AllowedPermissionsList(app.grantedPermissions, app.packageName)
                        1 -> DeniedPermissionsList(app.requestedPermissions.filterNot { app.grantedPermissions.contains(it) })
                        2 -> AllPermissionsList(app.requestedPermissions, app.grantedPermissions)
                    }
                }
            }

            // Watermark
            Text(
                text = "developed by Anvin",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .alpha(0.4f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun AppDetailHeader(app: AppDomainModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = app.icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
            )
            
            val badgeColor = when (app.riskBadge) {
                RiskBadge.HIGH -> Danger
                RiskBadge.MEDIUM -> Warning
                RiskBadge.LOW -> Success
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = 8.dp, y = 8.dp)
                    .scale(if (app.riskBadge == RiskBadge.HIGH) pulseScale else 1f)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
                    .background(badgeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (app.riskBadge == RiskBadge.HIGH) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(app.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Installed from: ${app.installSource}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AllowedPermissionsList(permissions: List<String>, packageName: String) {
    val sensitivePerms = permissions.filter { isSensitive(it) }
    val normalPerms = permissions.filterNot { isSensitive(it) }
    val sortedPerms = sensitivePerms + normalPerms

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        if (sortedPerms.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No permissions allowed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(sortedPerms) { index, permission ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    visible = true
                }
                
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(300))
                ) {
                    PermissionItemCard(permission = permission, isGranted = true, packageName = packageName)
                }
            }
        }
    }
}

@Composable
fun DeniedPermissionsList(permissions: List<String>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        if (permissions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No permissions denied.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            itemsIndexed(permissions) { index, permission ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(tween(300))
                ) {
                    PermissionItemCard(permission = permission, isGranted = false, packageName = null)
                }
            }
        }
    }
}

@Composable
fun AllPermissionsList(requested: List<String>, granted: List<String>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(requested) { index, permission ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 30L)
                visible = true
            }
            AnimatedVisibility(visible = visible, enter = fadeIn()) {
                val isGranted = granted.contains(permission)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = permission.substringAfterLast("."),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isGranted) "ALLOWED" else "DENIED",
                        color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionItemCard(permission: String, isGranted: Boolean, packageName: String?) {
    val context = LocalContext.current
    
    val humanName = permission.substringAfterLast(".").replace("_", " ")
    val isSensitive = isSensitive(permission)
    val color = if (isSensitive && isGranted) Warning else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSensitive) Icons.Default.Info else Icons.Default.CheckCircle,
                contentDescription = null, tint = color, modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(humanName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                if (isSensitive) {
                    Text("Sensitive data access", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
            if (isGranted && packageName != null) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }, 
                    modifier = Modifier.height(36.dp).bounceClick(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = 0.5f))
                ) {
                    Text("Revoke", fontSize = 12.sp, color = Danger)
                }
            }
        }
    }
}

@Composable
fun AppDetailBottomBar(app: AppDomainModel, isTrusted: Boolean, onTrustToggle: () -> Unit, onUninstall: () -> Unit, onOpenSettings: () -> Unit) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Box(modifier = Modifier.bounceClick { onOpenSettings() }.padding(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Text("Settings", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Box(
                modifier = Modifier
                    .bounceClick(scaleDown = if (!app.isSystemApp) 0.9f else 1f) { if (!app.isSystemApp) onUninstall() }
                    .padding(8.dp)
                    .alpha(if (app.isSystemApp) 0.5f else 1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Uninstall", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Box(modifier = Modifier.bounceClick { onTrustToggle() }.padding(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = if (isTrusted) MaterialTheme.colorScheme.primary else Color.Gray)
                    Text(if (isTrusted) "Trusted ✓" else "Trust App", fontSize = 10.sp, color = if (isTrusted) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }
    }
}

private fun isSensitive(permission: String): Boolean {
    val sensitive = listOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_SMS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )
    return sensitive.contains(permission)
}
