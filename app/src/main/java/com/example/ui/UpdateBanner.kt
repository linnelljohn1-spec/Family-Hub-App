package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.update.UpdateInfo
import java.io.File

@Composable
fun UpdateBanner(
    state: UpdateUiState,
    onUpdateClick: (UpdateInfo) -> Unit,
    onInstallClick: (File) -> Unit,
    onDismiss: (Int) -> Unit,
) {
    AnimatedVisibility(visible = state !is UpdateUiState.Idle) {
        Surface(tonalElevation = 3.dp, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Update available", style = MaterialTheme.typography.titleSmall)
                    val subtitle = when (state) {
                        is UpdateUiState.Available -> "Version ${state.info.versionName}"
                        is UpdateUiState.Downloading -> "Downloading…"
                        is UpdateUiState.ReadyToInstall -> "Ready to install"
                        UpdateUiState.Installing -> "Installing…"
                        UpdateUiState.Installed -> "Installed — reopen the app to finish updating"
                        is UpdateUiState.InstallFailed -> state.message
                        is UpdateUiState.Error -> state.message
                        UpdateUiState.Idle -> ""
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }

                when (state) {
                    is UpdateUiState.Available -> {
                        TextButton(onClick = { onUpdateClick(state.info) }) { Text("Update") }
                    }
                    is UpdateUiState.Downloading, UpdateUiState.Installing -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        TextButton(onClick = { onInstallClick(state.apkFile) }) { Text("Install") }
                    }
                    UpdateUiState.Installed, is UpdateUiState.InstallFailed,
                    is UpdateUiState.Error, UpdateUiState.Idle -> {}
                }

                val dismissVersionCode = when (state) {
                    is UpdateUiState.Available -> state.info.versionCode
                    is UpdateUiState.Downloading -> state.info.versionCode
                    else -> null
                }
                if (dismissVersionCode != null) {
                    IconButton(onClick = { onDismiss(dismissVersionCode) }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }
    }
}
