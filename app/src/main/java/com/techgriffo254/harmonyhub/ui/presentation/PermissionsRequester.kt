package com.techgriffo254.harmonyhub.ui.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionsRequester(
    private val context: Context,
    private val onPermissionsGranted: () -> Unit,
    private val onPermissionsDenied: () -> Unit
) {

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun checkAndRequestPermissions( activity: ComponentActivity) {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Request the required permissions
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissionsToRequest.all { permission ->
                    permissions[permission] == true
                }
                if (allGranted) {
                    onPermissionsGranted()
                } else {
                    onPermissionsDenied()
                }
            }.launch(permissionsToRequest.toTypedArray())
        } else {
            onPermissionsGranted() // All permissions already granted
        }
    }
}
