package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationHelper
import com.example.update.ApkInstaller
import com.example.ui.ChatViewModel
import com.example.ui.FamilyDataSyncViewModel
import com.example.ui.PollsViewModel
import com.example.ui.SavingsViewModel
import com.example.ui.ShoppingListsViewModel
import com.example.ui.UpdateBanner
import com.example.ui.UpdateUiState
import com.example.ui.UpdateViewModel
import com.example.ui.screens.FamilySavingsApp
import com.example.ui.theme.MyApplicationTheme
import com.example.R

class MainActivity : ComponentActivity() {
  private val viewModel: SavingsViewModel by viewModels()
  private val chatViewModel: ChatViewModel by viewModels()
  private val familyDataSyncViewModel: FamilyDataSyncViewModel by viewModels()
  private val pollsViewModel: PollsViewModel by viewModels()
  private val shoppingListsViewModel: ShoppingListsViewModel by viewModels()
  private val updateViewModel: UpdateViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    NotificationHelper.createChannels(this)

    setContent {
      MyApplicationTheme {
        val context = LocalContext.current
        val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { }
        val requestInstallPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()
        ) {
          // Returning from the "allow installs from this source" settings screen:
          // retry the install now that the permission may have been granted.
          val state = updateViewModel.uiState.value
          if (state is UpdateUiState.ReadyToInstall && ApkInstaller.canRequestInstallPackages(context)) {
            ApkInstaller.installApk(context, state.apkFile)
          }
        }

        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
          ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }

        LaunchedEffect(Unit) {
          updateViewModel.checkForUpdateOnce()
        }

        val updateUiState by updateViewModel.uiState.collectAsState()

        Column(modifier = Modifier.fillMaxSize()) {
          UpdateBanner(
            state = updateUiState,
            onUpdateClick = { info -> updateViewModel.startDownload(info) },
            onInstallClick = { apkFile ->
              if (ApkInstaller.canRequestInstallPackages(context)) {
                ApkInstaller.installApk(context, apkFile)
              } else {
                requestInstallPermissionLauncher.launch(ApkInstaller.installPermissionSettingsIntent(context))
              }
            },
            onDismiss = { versionCode -> updateViewModel.dismiss(versionCode) }
          )

          Box(modifier = Modifier.weight(1f)) {
            FamilySavingsApp(
              viewModel = viewModel,
              chatViewModel = chatViewModel,
              familyDataSyncViewModel = familyDataSyncViewModel,
              pollsViewModel = pollsViewModel,
              shoppingListsViewModel = shoppingListsViewModel
            )
          }
        }
      }
    }
  }
}
