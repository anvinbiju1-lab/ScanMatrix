package com.example.securityapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import java.io.File

class ApkInterceptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uri: Uri? = intent.data
        if (uri == null) {
            finish()
            return
        }

        // Mock scan logic for the APK file represented by the URI
        val isMalicious = uri.path?.contains("mod", ignoreCase = true) == true || 
                          uri.path?.contains("hack", ignoreCase = true) == true

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isMalicious) {
                        MaliciousWarningScreen(
                            onStop = { finish() },
                            onContinue = { forwardToInstaller(uri) }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            forwardToInstaller(uri)
                        }
                    }
                }
            }
        }
    }

    private fun forwardToInstaller(uri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            // Avoid routing back to ourselves!
            val resInfo = packageManager.queryIntentActivities(this, 0)
            val defaultInstaller = resInfo.firstOrNull { it.activityInfo.packageName != packageName }
            
            if (defaultInstaller != null) {
                setClassName(defaultInstaller.activityInfo.packageName, defaultInstaller.activityInfo.name)
            } else {
                setPackage("com.google.android.packageinstaller")
            }
        }
        
        try {
            startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not launch package installer", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}

@Composable
fun MaliciousWarningScreen(onStop: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = Danger,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Malicious APK Detected!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Danger
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Our deep scan indicates that this APK file contains known threats or suspicious modifications. Installing this could compromise your device.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Installation (Recommended)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onContinue) {
            Text("Install Anyway (Risky)", color = MaterialTheme.colorScheme.error)
        }
    }
}
