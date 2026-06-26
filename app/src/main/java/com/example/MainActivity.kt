package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAppContainer
import com.example.ui.AppViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.data.AuraErrorHandler
import com.example.data.AuraImageLoader
import coil.Coil

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Install global error handler
    AuraErrorHandler.install(this)
    // Configure optimized image loader
    Coil.setImageLoader(AuraImageLoader.getInstance(this))

    try {
      if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
        try {
          com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
          // Fall back to programmatic initialization using a dummy config
          val options = com.google.firebase.FirebaseOptions.Builder()
            .setApiKey("AIzaSyDummyKeyForAuraNotesInitOnly")
            .setApplicationId("1:1234567890:android:abcdef123456")
            .setProjectId("aura-notes-placeholder")
            .build()
          com.google.firebase.FirebaseApp.initializeApp(this, options)
        }
      }
    } catch (e: Exception) {
      AuraErrorHandler.report("MainActivity.FirebaseInit", e)
    }
    enableEdgeToEdge()
    setContent {
      val viewModel: AppViewModel = viewModel()
      val themeMode by viewModel.themeMode.collectAsState()
      val themePalette by viewModel.themePalette.collectAsState()
      val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

      MyApplicationTheme(themeMode = themeMode, themePalette = themePalette) {
        if (!hasSeenOnboarding) {
          com.example.ui.OnboardingScreen(
            viewModel = viewModel,
            onFinished = {
              // Complete onboarding and flow to primary app deck
            }
          )
        } else {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainAppContainer(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}
