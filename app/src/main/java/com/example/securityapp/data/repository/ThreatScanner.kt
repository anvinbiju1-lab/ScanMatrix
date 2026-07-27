package com.example.securityapp.data.repository

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class RiskLevel { HIGH, MEDIUM, LOW, SAFE }

enum class ThreatType {
    NONE, BLACKLISTED_HASH, BLACKLISTED_PACKAGE, DANGEROUS_PERMS, HIDDEN_FILE, MISMATCHED_EXT,
    ARCHIVE_CONTAINS_APK, PDF_SCRIPT, DEX_JAR, DEVICE_SETTING, PRIVILEGED_ACCESS
}

data class ScanResult(
    val app: AppDomainModel?,
    val fileName: String?,
    val filePath: String?,
    val riskLevel: RiskLevel,
    val reasons: List<String>,
    val threatType: ThreatType,
    val isTrusted: Boolean = false
)

data class FullScanSummary(
    val totalScanned: Int,
    val threatsHigh: Int,
    val threatsMedium: Int,
    val threatsLow: Int,
    val safeCount: Int,
    val timeTakenSeconds: Long
)

@Singleton
class ThreatScanner @Inject constructor(
    private val appRepository: AppRepository,
    private val virusTotalRepository: VirusTotalRepository,
    @ApplicationContext private val context: Context
) {
    private fun getThreatDefinitions(): JSONObject {
        return try {
            val fileStr = context.assets.open("threat_definitions.json").bufferedReader().use { it.readText() }
            JSONObject(fileStr)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    suspend fun performScan(
        isDeepScan: Boolean,
        onPhaseChange: (String) -> Unit,
        onProgress: (Float) -> Unit,
        onCurrentFolder: (String) -> Unit = {},
        onFileCount: (Int) -> Unit = {}
    ): Pair<List<ScanResult>, FullScanSummary?> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<ScanResult>()
        val threatDefs = getThreatDefinitions()

        val blacklistedPackages = threatDefs.optJSONArray("malicious_packages")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
        
        val maliciousHashes = threatDefs.optJSONArray("malicious_apk_hashes")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it).lowercase() }
        } ?: emptyList()
        
        val maliciousPdfKeywords = threatDefs.optJSONArray("malicious_pdf_keywords")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        // --- STAGE 1: Installed Apps ---
        onPhaseChange("Stage 1 of 4: Scanning Installed Apps")
        val apps = appRepository.getInstalledApps().filter { !it.isSystemApp }
        
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledAccessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .map { it.resolveInfo.serviceInfo.packageName }
            
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val activeAdmins = devicePolicyManager.activeAdmins?.map { it.packageName } ?: emptyList()

        apps.forEachIndexed { index, app ->
            delay(10) // Simulate slight processing delay for UI
            onProgress((index + 1).toFloat() / apps.size)

            val reasons = mutableListOf<String>()
            var riskLevel = RiskLevel.SAFE
            var threatType = ThreatType.NONE

            if (blacklistedPackages.contains(app.packageName)) {
                reasons.add("Matches known malicious package name.")
                riskLevel = RiskLevel.HIGH
                threatType = ThreatType.BLACKLISTED_PACKAGE
            }
            
            if (enabledAccessibilityServices.contains(app.packageName)) {
                reasons.add("App has Accessibility Service enabled (potential screen reader/keylogger risk).")
                if (riskLevel != RiskLevel.HIGH) { riskLevel = RiskLevel.MEDIUM; threatType = ThreatType.PRIVILEGED_ACCESS }
            }
            
            if (activeAdmins.contains(app.packageName)) {
                reasons.add("App is a Device Administrator (can lock screen, wipe data).")
                if (riskLevel != RiskLevel.HIGH) { riskLevel = RiskLevel.HIGH; threatType = ThreatType.PRIVILEGED_ACCESS }
            }

            val perms = app.grantedPermissions
            val hasCamera = perms.contains("android.permission.CAMERA")
            val hasMic = perms.contains("android.permission.RECORD_AUDIO")
            val hasLocation = perms.contains("android.permission.ACCESS_FINE_LOCATION")
            val hasSms = perms.contains("android.permission.READ_SMS")

            if (hasCamera && hasMic && hasLocation && hasSms) {
                reasons.add("Highly invasive permission combo (Camera + Mic + Location + SMS).")
                if (riskLevel != RiskLevel.HIGH) { riskLevel = RiskLevel.HIGH; threatType = ThreatType.DANGEROUS_PERMS }
            } else if (app.riskBadge == RiskBadge.HIGH) {
                reasons.add("Multiple sensitive permissions granted.")
                if (riskLevel == RiskLevel.SAFE) { riskLevel = RiskLevel.MEDIUM; threatType = ThreatType.DANGEROUS_PERMS }
            }
            
            if (app.installSource == "Unknown / Manual Install") {
                reasons.add("App installed outside of official app stores (sideloaded).")
                if (riskLevel == RiskLevel.SAFE) { riskLevel = RiskLevel.LOW; threatType = ThreatType.NONE }
            }

            if (riskLevel == RiskLevel.SAFE) reasons.add("No obvious threats found.")

            results.add(ScanResult(app, null, null, riskLevel, reasons, threatType))
        }

        // --- STAGE 2: Device Risk Settings ---
        if (isDeepScan) {
            onPhaseChange("Stage 2 of 4: Checking Device Security Settings")
            onProgress(0f)
            delay(500)
            
            val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            if (adbEnabled) {
                results.add(ScanResult(null, "USB Debugging Enabled", null, RiskLevel.MEDIUM, 
                    listOf("Allows computer to execute commands on your device. Disable when not developing apps."), ThreatType.DEVICE_SETTING))
            }
            
            val unknownSources = Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
            if (unknownSources) {
                results.add(ScanResult(null, "Unknown Sources Allowed", null, RiskLevel.HIGH, 
                    listOf("Device can install apps outside of Play Store globally. High risk of malware."), ThreatType.DEVICE_SETTING))
            }
            
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isDeviceSecure) {
                results.add(ScanResult(null, "No Screen Lock", null, RiskLevel.HIGH, 
                    listOf("No PIN, Password, or Pattern set. Anyone with physical access can view your data."), ThreatType.DEVICE_SETTING))
            }
            onProgress(1f)
            
            // --- STAGE 3: Storage File Scan ---
            onPhaseChange("Stage 3 of 4: Scanning Storage for Hidden Threats")
            onProgress(0f)
            
            if (Environment.isExternalStorageManager()) {
                val rootDir = Environment.getExternalStorageDirectory()
                var fileCount = 0
                val totalFilesEstimation = 5000f // Rough estimation for progress
                var allowOnlineChecks = true // Prevent blasting VT if rate limited
                
                suspend fun scanDirectory(dir: File) {
                    val files = dir.listFiles() ?: return
                    for (file in files) {
                        fileCount++
                        if (fileCount % 50 == 0) { // Update UI periodically
                            onCurrentFolder(file.parent ?: "")
                            onFileCount(fileCount)
                            onProgress(Math.min(fileCount / totalFilesEstimation, 0.99f))
                        }
                        
                        if (file.isDirectory) {
                            if (file.name.startsWith(".")) {
                                val reasons = mutableListOf("Hidden directory found.")
                                var risk = RiskLevel.LOW
                                if (file.listFiles()?.any { it.name.endsWith(".apk") } == true) {
                                    reasons.add("Contains hidden APK files.")
                                    risk = RiskLevel.HIGH
                                }
                                results.add(ScanResult(null, file.name, file.absolutePath, risk, reasons, ThreatType.HIDDEN_FILE))
                            }
                            if (!file.name.startsWith("Android")) { // Skip deep system Android/data folder to save time
                                scanDirectory(file)
                            }
                        } else {
                            if (file.name.startsWith(".")) {
                                results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.MEDIUM, listOf("Hidden file found outside standard folders."), ThreatType.HIDDEN_FILE))
                            } else if (file.name.endsWith(".apk", true) || file.name.endsWith(".xapk", true)) {
                                val hash = getFileSha256(file)
                                var isMalicious = maliciousHashes.contains(hash)
                                var cloudChecked = false
                                var reason = "APK file found but hash matches no known threats."
                                
                                if (!isMalicious && allowOnlineChecks) {
                                    val vtResult = virusTotalRepository.checkFileHash(hash)
                                    when (vtResult) {
                                        is VirusTotalResult.Scanned -> {
                                            cloudChecked = true
                                            if (vtResult.isMalicious) {
                                                isMalicious = true
                                                reason = "VirusTotal detected this file as malicious! (${vtResult.maliciousVotes} engines flagged it)"
                                            } else {
                                                reason = "Safe: Scanned by VirusTotal engines."
                                            }
                                        }
                                        is VirusTotalResult.RateLimited -> {
                                            allowOnlineChecks = false
                                        }
                                        else -> {}
                                    }
                                } else if (isMalicious) {
                                    reason = "File hash matches known local malware database."
                                }

                                if (isMalicious) {
                                    results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.HIGH, listOf(reason), ThreatType.BLACKLISTED_HASH))
                                } else {
                                    results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.SAFE, listOf(reason), ThreatType.NONE))
                                }
                            } else if (file.name.endsWith(".dex", true) || file.name.endsWith(".jar", true)) {
                                results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.MEDIUM, listOf("Android executable (DEX/JAR) found in standard storage. This is highly suspicious."), ThreatType.DEX_JAR))
                            } else if (file.name.endsWith(".pdf", true)) {
                                if (checkPdfForScripts(file, maliciousPdfKeywords)) {
                                    results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.HIGH, listOf("PDF file contains embedded JavaScript or launch commands."), ThreatType.PDF_SCRIPT))
                                }
                            } else if (file.name.endsWith(".zip", true) || file.name.endsWith(".rar", true)) {
                                if (checkArchiveForExecutables(file)) {
                                    results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.HIGH, listOf("Archive contains hidden APK or DEX executables inside."), ThreatType.ARCHIVE_CONTAINS_APK))
                                }
                            } else {
                                // Mismatched extension check (APK disguised as image)
                                if (file.length() > 1024 * 1024 && (file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true))) {
                                    if (isApkMismatched(file)) {
                                        results.add(ScanResult(null, file.name, file.absolutePath, RiskLevel.HIGH, listOf("File extension is an image but headers indicate it is an executable APK!"), ThreatType.MISMATCHED_EXT))
                                    }
                                }
                            }
                        }
                    }
                }
                scanDirectory(rootDir)
                onProgress(1f)
            } else {
                results.add(ScanResult(null, "Storage Permission Missing", null, RiskLevel.MEDIUM, listOf("Deep scan couldn't read all files. Grant 'All files access' permission."), ThreatType.DEVICE_SETTING))
            }
        }

        // --- STAGE 4: Finalizing Results ---
        onPhaseChange("Stage 4 of 4: Finalizing Results")
        delay(500)
        
        val sortedResults = results.sortedByDescending { it.riskLevel.ordinal }.reversed() // Sort High -> Safe
        
        val summary = if (isDeepScan) {
            FullScanSummary(
                totalScanned = sortedResults.size,
                threatsHigh = sortedResults.count { it.riskLevel == RiskLevel.HIGH },
                threatsMedium = sortedResults.count { it.riskLevel == RiskLevel.MEDIUM },
                threatsLow = sortedResults.count { it.riskLevel == RiskLevel.LOW },
                safeCount = sortedResults.count { it.riskLevel == RiskLevel.SAFE },
                timeTakenSeconds = (System.currentTimeMillis() - startTime) / 1000
            )
        } else null

        Pair(sortedResults, summary)
    }

    private fun getFileSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun checkPdfForScripts(file: File, keywords: List<String>): Boolean {
        try {
            if (file.length() > 50 * 1024 * 1024) return false // Prevent OOM, don't scan huge PDFs deeply
            FileInputStream(file).bufferedReader().use { reader ->
                val buffer = CharArray(16384) // Scan first 16KB usually where headers/metadata are
                val charsRead = reader.read(buffer, 0, buffer.size)
                if (charsRead > 0) {
                    val content = String(buffer, 0, charsRead)
                    for (kw in keywords) {
                        if (content.contains(kw, ignoreCase = true)) return true
                    }
                }
            }
        } catch (e: Exception) {}
        return false
    }
    
    private fun checkArchiveForExecutables(file: File): Boolean {
        if (file.name.endsWith(".rar", true)) {
            // RAR checking requires external library, fallback to heuristic by looking at file magic bytes structure if possible.
            // For this implementation, we will skip native RAR deep extraction without libs, but flag it as low risk elsewhere if needed.
            return false 
        }
        
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                var entriesChecked = 0
                while (entry != null && entriesChecked < 100) { // Limit to 100 entries to prevent Zip bomb DOS
                    if (entry.name.endsWith(".apk", true) || entry.name.endsWith(".dex", true)) {
                        return true
                    }
                    entriesChecked++
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {}
        return false
    }
    
    private fun isApkMismatched(file: File): Boolean {
        try {
            FileInputStream(file).use { fis ->
                val bytes = ByteArray(4)
                if (fis.read(bytes) == 4) {
                    // Check ZIP/APK magic bytes: 50 4B 03 04 (PK..)
                    if (bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {}
        return false
    }
}
