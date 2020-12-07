package com.dobots.bpp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private val TAG = "BoomPinPoint Activity"
    private var api: API? = null
    private var next_message = "Starting detection..."

    // Requesting permission to RECORD_AUDIO and ACCESS_LOCATION
    private var permissionToRecordAccepted = false
    private var mic_and_location_permissions = arrayOf<String>(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            // Microphone and location
            100 -> {
                // If permission granted continue, else exit
                permissionToRecordAccepted = grantResults[0] === PackageManager.PERMISSION_GRANTED
                if (!permissionToRecordAccepted) finish()
            }
        }

    }

    override fun onDestroy() {
        api?.unbind(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request permission for microphone and location
        ActivityCompat.requestPermissions(this, mic_and_location_permissions, 100)

        API.bind(this, object : Callback<API>() {
            override fun onResult(result: API?) {
                api = result
                return
            }

            override fun onError(error: java.lang.Error?) {
                Log.e(TAG, "Couldn't bind to API service!")
            }

        })

        // HERE we add event handlers for buttons in the Screen layout
        findViewById<Button>(R.id.button).setOnClickListener { view ->
            Snackbar.make(view, next_message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            next_message = api?.HandleButtonPress().toString()
        }

    }
}
