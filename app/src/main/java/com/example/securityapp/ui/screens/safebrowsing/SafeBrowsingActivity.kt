package com.example.securityapp.ui.screens.safebrowsing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.securityapp.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SafeBrowsingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var urlToScan: String? = null
        var isVpnBlock = false

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    urlToScan = intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }
            Intent.ACTION_VIEW -> {
                urlToScan = intent.dataString
            }
            "com.example.securityapp.ACTION_VPN_BLOCK" -> {
                urlToScan = intent.getStringExtra("BLOCKED_URL")
                isVpnBlock = true
            }
        }

        if (urlToScan == null) {
            finish()
            return
        }

        setContent {
            SecurityAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeBrowsingScreen(
                        url = urlToScan!!,
                        isForceBlocked = isVpnBlock,
                        onBlock = { finish() },
                        onProceed = {
                            if (!isVpnBlock) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToScan!!)).apply {
                                    // Prevent infinite loop of intercepting our own intent
                                    setPackage("com.android.chrome") // Hardcoded for demo
                                }
                                try {
                                    startActivity(browserIntent)
                                } catch (e: Exception) {
                                    // Ignore if chrome missing
                                }
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SafeBrowsingScreen(url: String, isForceBlocked: Boolean, onBlock: () -> Unit, onProceed: () -> Unit) {
    val isSuspicious = remember(url) { isForceBlocked || checkUrlHeuristics(url) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSuspicious) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSuspicious) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Danger,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Suspicious Link Detected!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Danger,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isForceBlocked) "Malicious connection blocked by ScanMatrix protection." else "The link you are trying to open has been flagged as potentially dangerous (phishing or malware).",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Link Checked: Safe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isSuspicious) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = url,
                modifier = Modifier.padding(16.dp),
                color = if (isSuspicious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (isSuspicious) {
            Button(
                onClick = onBlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
            ) {
                Text(if (isForceBlocked) "Close" else "Block & Do Not Open", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (!isForceBlocked) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onProceed) {
                    Text("Proceed Anyway (Not Recommended)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Button(
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Open Link", fontSize = 18.sp)
            }
        }
    }
}

fun checkUrlHeuristics(url: String): Boolean {
    // Basic heuristic checks
    try {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false
        
        // 1. Punycode (homograph attacks)
        if (host.contains("xn--")) return true
        
        // 2. Excessively long domain or suspicious TLDs
        if (host.length > 50) return true
        if (host.endsWith(".xyz") || host.endsWith(".tk")) return true
        
        // 3. Known malicious (mock)
        val blacklist = listOf("phishing-login.com", "secure-update-verify.net")
        if (blacklist.any { host.contains(it) }) return true

    } catch (e: Exception) {
        return true // Parse error -> suspicious
    }
    
    return false
}
