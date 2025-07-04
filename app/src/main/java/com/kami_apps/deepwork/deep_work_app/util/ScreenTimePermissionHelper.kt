package com.kami_apps.deepwork.deep_work_app.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext

class ScreenTimePermissionHelper(private val context: Context) {
    
    /**
     * Usage Access permission'ının verilip verilmediğini kontrol eder
     */
    fun hasUsageAccessPermission(): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * System Alert Window permission'ını kontrol eder
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Tüm gerekli izinlerin verilip verilmediğini kontrol eder
     */
    fun hasAllPermissions(): Boolean {
        return hasUsageAccessPermission() && hasOverlayPermission()
    }
    
    /**
     * Usage Access Settings Intent'ini oluşturur
     */
    fun createUsageAccessIntent(): Intent {
        return try {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        } catch (e: Exception) {
            // Fallback
            Intent(Settings.ACTION_SETTINGS)
        }
    }
    
    /**
     * Overlay Permission Settings Intent'ini oluşturur
     */
    fun createOverlayPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
            } catch (e: Exception) {
                Intent(Settings.ACTION_SETTINGS)
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }
}

/**
 * Compose için Screen Time Permission Hook'u
 */
@Composable
fun rememberScreenTimePermissionLauncher(
    onResult: (Boolean) -> Unit
): ScreenTimePermissionLauncher {
    val context = LocalContext.current
    val helper = remember { ScreenTimePermissionHelper(context) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Settings'den dönüldüğünde permission durumunu kontrol et
        onResult(helper.hasUsageAccessPermission())
    }
    
    return remember { 
        ScreenTimePermissionLauncher(helper, launcher) 
    }
}

/**
 * Screen Time Permission Launcher wrapper class'ı
 */
class ScreenTimePermissionLauncher(
    private val helper: ScreenTimePermissionHelper,
    private val launcher: ActivityResultLauncher<Intent>
) {
    
    fun hasUsageAccessPermission(): Boolean = helper.hasUsageAccessPermission()
    
    fun hasOverlayPermission(): Boolean = helper.hasOverlayPermission()
    
    fun hasAllPermissions(): Boolean = helper.hasAllPermissions()
    
    fun requestUsageAccessPermission() {
        if (helper.hasUsageAccessPermission()) {
            return
        }
        launcher.launch(helper.createUsageAccessIntent())
    }
    
    fun requestOverlayPermission() {
        if (helper.hasOverlayPermission()) {
            return
        }
        launcher.launch(helper.createOverlayPermissionIntent())
    }
} 