package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MikroTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("MikroTrackApp", "FirebaseApp initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("MikroTrackApp", "FirebaseApp initialization error: ${e.message}", e)
        }
    }
}
