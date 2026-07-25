package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.update.UpdateChecker
import com.example.update.UpdateDownloader
import com.example.update.UpdateInfo
import com.example.update.UpdatePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo) : UpdateUiState
    data class ReadyToInstall(val apkFile: File) : UpdateUiState
    data object Installing : UpdateUiState
    data object Installed : UpdateUiState
    data class InstallFailed(val message: String) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var checkedThisProcess = false

    /** Checks GitHub for a newer release. Only does anything the first time it's called per process. */
    fun checkForUpdateOnce() {
        if (checkedThisProcess) return
        checkedThisProcess = true

        viewModelScope.launch {
            val info = UpdateChecker.checkForUpdate() ?: return@launch
            if (info.versionCode <= BuildConfig.VERSION_CODE) return@launch
            if (info.versionCode <= UpdatePreferences.getDismissedVersionCode(getApplication())) return@launch
            _uiState.value = UpdateUiState.Available(info)
        }
    }

    fun startDownload(info: UpdateInfo) {
        _uiState.value = UpdateUiState.Downloading(info)
        viewModelScope.launch {
            val context = getApplication<Application>()
            val downloadId = UpdateDownloader.enqueueDownload(context, info.downloadUrl)
            val success = UpdateDownloader.awaitCompletion(context, downloadId)
            _uiState.value = if (success) {
                UpdateUiState.ReadyToInstall(UpdateDownloader.downloadedApkFile(context))
            } else {
                UpdateUiState.Error("Download failed. Check your connection and try again.")
            }
        }
    }

    fun dismiss(versionCode: Int) {
        UpdatePreferences.setDismissedVersionCode(getApplication(), versionCode)
        _uiState.value = UpdateUiState.Idle
    }

    fun onInstallStarted() {
        _uiState.value = UpdateUiState.Installing
    }

    fun onInstallSucceeded() {
        _uiState.value = UpdateUiState.Installed
    }

    fun onInstallFailed(message: String) {
        _uiState.value = UpdateUiState.InstallFailed(message)
    }
}
