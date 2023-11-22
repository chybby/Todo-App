package com.chybby.todo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
class MockPermissionState(override val permission: String): PermissionState {
    override val status = PermissionStatus.Granted
    override fun launchPermissionRequest() = Unit
}

@ExperimentalPermissionsApi
@Composable
fun rememberPermissionStateSafe(
    permission: String,
    onPermissionResult: (Boolean) -> Unit
): PermissionState {
    return when {
        LocalInspectionMode.current -> remember {
            MockPermissionState(permission)
        }
        else -> rememberPermissionState(permission, onPermissionResult)
    }
}

@ExperimentalPermissionsApi
@Composable
fun rememberMultiplePermissionsStateSafe(
    permissions: List<String>,
    onPermissionsResult: (Map<String, Boolean>) -> Unit
): MultiplePermissionsState {
    return when {
        LocalInspectionMode.current -> remember {
            object : MultiplePermissionsState {
                override val permissions = permissions.map { MockPermissionState(it) }
                override val revokedPermissions: List<PermissionState> = listOf()
                override val allPermissionsGranted = true
                override val shouldShowRationale = false
                override fun launchMultiplePermissionRequest() = Unit
            }
        }
        else -> rememberMultiplePermissionsState(permissions, onPermissionsResult)
    }
}