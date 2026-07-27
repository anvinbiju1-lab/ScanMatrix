package com.example.securityapp.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.app.usage.UsageStatsManager
import java.util.Calendar
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class RiskBadge { HIGH, MEDIUM, LOW }

data class AppDomainModel(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val requestedPermissions: List<String>,
    val grantedPermissions: List<String>,
    val isSystemApp: Boolean,
    val lastUsedTime: Long = 0L,
    val installSource: String = "Unknown",
    val riskBadge: RiskBadge = RiskBadge.LOW
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getInstalledApps(): List<AppDomainModel> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages: List<PackageInfo> = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val usageStatsMap = try {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
                ?.associateBy { it.packageName } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        
        packages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            
            val requestedPermissions = mutableListOf<String>()
            val grantedPermissions = mutableListOf<String>()
            
            packageInfo.requestedPermissions?.let { reqPerms ->
                val flags = packageInfo.requestedPermissionsFlags
                reqPerms.forEachIndexed { i, perm ->
                    requestedPermissions.add(perm)
                    if (flags != null && i < flags.size && (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        grantedPermissions.add(perm)
                    }
                }
            }
            
            val lastUsed = usageStatsMap[packageInfo.packageName]?.lastTimeUsed ?: 0L
            
            val installer = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(packageInfo.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(packageInfo.packageName)
                }
            } catch (e: Exception) { null }
            
            val installSource = when {
                installer == "com.android.vending" -> "Play Store"
                installer == "com.amazon.venezia" -> "Amazon Appstore"
                installer == "com.sec.android.app.samsungapps" -> "Galaxy Store"
                installer == null && isSystem -> "Pre-installed"
                installer != null -> installer
                else -> "Unknown / Manual Install"
            }
            
            val hasCamera = grantedPermissions.contains("android.permission.CAMERA")
            val hasMic = grantedPermissions.contains("android.permission.RECORD_AUDIO")
            val hasLocation = grantedPermissions.contains("android.permission.ACCESS_FINE_LOCATION") || 
                              grantedPermissions.contains("android.permission.ACCESS_COARSE_LOCATION")
            val hasContacts = grantedPermissions.contains("android.permission.READ_CONTACTS") ||
                              grantedPermissions.contains("android.permission.WRITE_CONTACTS")
            val hasSms = grantedPermissions.contains("android.permission.READ_SMS") ||
                         grantedPermissions.contains("android.permission.SEND_SMS") ||
                         grantedPermissions.contains("android.permission.RECEIVE_SMS")
                         
            var riskScore = 0
            if (hasCamera) riskScore += 2
            if (hasMic) riskScore += 2
            if (hasLocation) riskScore += 1
            if (hasContacts) riskScore += 1
            if (hasSms) riskScore += 2
            
            val riskBadge = when {
                riskScore >= 4 -> RiskBadge.HIGH
                riskScore in 2..3 -> RiskBadge.MEDIUM
                else -> RiskBadge.LOW
            }
            
            AppDomainModel(
                packageName = packageInfo.packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                icon = pm.getApplicationIcon(appInfo),
                requestedPermissions = requestedPermissions,
                grantedPermissions = grantedPermissions,
                isSystemApp = isSystem,
                lastUsedTime = lastUsed,
                installSource = installSource,
                riskBadge = riskBadge
            )
        }.sortedBy { it.appName.lowercase() }
    }
}
