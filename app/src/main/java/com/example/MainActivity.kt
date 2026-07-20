package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.notifications.NotificationHelper
import com.example.ui.ChatViewModel
import com.example.ui.SavingsViewModel
import com.example.ui.screens.FamilySavingsApp
import com.example.ui.theme.MyApplicationTheme
import com.example.R

class MainActivity : ComponentActivity() {
  private val viewModel: SavingsViewModel by viewModels()
  private val chatViewModel: ChatViewModel by viewModels()

  private val requestNotificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    NotificationHelper.createChannels(this)
    requestNotificationPermissionIfNeeded()

    setContent {
      MyApplicationTheme {
        FamilySavingsApp(viewModel = viewModel, chatViewModel = chatViewModel)
      }
    }
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
