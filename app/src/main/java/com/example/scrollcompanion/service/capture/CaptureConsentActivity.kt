package com.example.scrollcompanion.service.capture

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class CaptureConsentActivity : ComponentActivity() {

    private val consentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            Log.e("ScrollCapture", "consent granted, starting service")
            ScreenCaptureService.start(this, result.resultCode, data)
        } else {
            Toast.makeText(this, "Screen capture was cancelled.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            consentLauncher.launch(manager.createScreenCaptureIntent())
        }
    }
}
