package com.example

import android.app.Application
import com.example.data.AuraErrorHandler
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val isInitialized = try {
                FirebaseApp.getInstance() != null
            } catch (e: IllegalStateException) {
                false
            }
            if (!isInitialized) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    // Fall back to programmatic initialization using a dummy config
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDummyKeyForAuraNotesInitOnly")
                        .setApplicationId("1:1234567890:android:abcdef123456")
                        .setProjectId("aura-notes-placeholder")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            AuraErrorHandler.report("AuraApplication.FirebaseInit", e)
        }
    }
}
