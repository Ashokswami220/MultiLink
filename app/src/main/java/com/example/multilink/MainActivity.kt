package com.example.multilink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.multilink.ui.navigation.MultiLinkNavApp
import com.example.multilink.ui.theme.MultiLinkTheme
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    // 1. Define permissions
    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ FIXED DEBUG BLOCK ⭐
        try {
            // Flag to get signatures (signatures is deprecated but works for debugging)
            val flags = PackageManager.GET_SIGNATURES
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }

            // ⭐ FIX: Check if signatures is not null before looping
            @Suppress("DEPRECATION")
            val signatures = info.signatures

            signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val hexString = StringBuilder()
                for (b in digest) {
                    hexString.append(String.format("%02X:", b))
                }
                // Remove last colon
                if (hexString.isNotEmpty()) {
                    hexString.setLength(hexString.length - 1)
                }

                // PRINT THE REAL KEY TO LOGCAT
                android.util.Log.e("REAL_SHA1", "Your App is Signed with: $hexString")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()


        if (!hasPermissions()) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }

        // Deep Link Handling
        var initialJoinCode: String? = null
        val data: Uri? = intent?.data

        if (data != null && data.scheme == "https" && data.host == "multilink-aa2228.web.app" && data.pathSegments.size > 1) {
            initialJoinCode = data.pathSegments[1]
        }

        setContent {
            MultiLinkTheme {
                MultiLinkNavApp(startJoinCode = initialJoinCode)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}