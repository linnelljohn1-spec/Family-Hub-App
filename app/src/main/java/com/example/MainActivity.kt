package com.aistudio.familysavings.cleaninstall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.aistudio.familysavings.cleaninstall.ui.SavingsViewModel
import com.aistudio.familysavings.cleaninstall.ui.screens.FamilySavingsApp
import com.aistudio.familysavings.cleaninstall.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SavingsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        FamilySavingsApp(viewModel = viewModel)
      }
    }
  }
}
