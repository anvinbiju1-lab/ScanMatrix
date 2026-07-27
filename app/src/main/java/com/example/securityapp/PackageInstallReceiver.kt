package com.example.securityapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PackageInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val data = intent.data
            val packageName = data?.schemeSpecificPart
            if (packageName != null) {
                // In a full implementation, we would queue a WorkManager task to scan the newly installed app.
                // For this demo, we'll just show a Toast indicating detection.
                Toast.makeText(context, "Security App: Scanning new install $packageName", Toast.LENGTH_LONG).show()
                // A background scan would be triggered here.
            }
        }
    }
}
