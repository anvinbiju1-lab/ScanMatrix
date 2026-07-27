package com.example.securityapp.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.securityapp.R
import com.example.securityapp.ui.utils.bounceClick
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onFinishOnboarding: () -> Unit) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentStep++
        }
    }

    val steps = mutableListOf(
        OnboardingStep(
            title = "Welcome to ScanMatrix",
            description = "Your all-in-one privacy and security monitor. All analysis is done safely on-device to respect your privacy.",
            illustrationRes = R.drawable.onboarding_welcome,
            buttonText = "Next",
            action = { currentStep++ }
        ),
        OnboardingStep(
            title = "Usage Access Required",
            description = "To monitor which apps are using data and sensitive permissions, we need Usage Access.",
            illustrationRes = R.drawable.onboarding_usage,
            buttonText = "Grant Usage Access",
            action = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
                currentStep++
            }
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        steps.add(
            OnboardingStep(
                title = "All Files Access",
                description = "Required to scan your entire device for hidden threats, malicious APKs, and dangerous files.",
                illustrationRes = R.drawable.onboarding_storage,
                buttonText = "Grant Storage Access",
                action = {
                    if (!Environment.isExternalStorageManager()) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                    currentStep++
                }
            )
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        steps.add(
            OnboardingStep(
                title = "Notifications Required",
                description = "We need permission to alert you when a scan finishes or a background threat is detected.",
                illustrationRes = R.drawable.onboarding_welcome, // Reuse or generic
                buttonText = "Allow Notifications",
                action = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        currentStep++
                    }
                }
            )
        )
    }

    steps.add(
        OnboardingStep(
            title = "All Set!",
            description = "Your device is ready to be monitored. You can grant other optional permissions inside the app.",
            illustrationRes = R.drawable.onboarding_success,
            buttonText = "Get Started",
            action = onFinishOnboarding
        )
    )

    if (currentStep >= steps.size) {
        onFinishOnboarding()
        return
    }

    val step = steps[currentStep]

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            } else {
                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }.using(SizeTransform(clip = false))
        },
        label = "onboarding_transition"
    ) { stepIndex ->
        val step = steps[stepIndex]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = step.illustrationRes),
                contentDescription = null,
                modifier = Modifier.size(180.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = step.action,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick()
            ) {
                Text(text = step.buttonText, fontSize = 18.sp)
            }
            
            if (currentStep > 0 && currentStep < steps.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { currentStep++ }) {
                    Text("Skip for now")
                }
            }
        }
    }
}

data class OnboardingStep(
    val title: String,
    val description: String,
    val illustrationRes: Int,
    val buttonText: String,
    val action: () -> Unit
)
