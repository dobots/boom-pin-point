package com.dobots.bpp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private val TAG = "BoomPinPoint Activity"
    private var api: API? = null
    private var next_message = "Starting detection..."

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == 200) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    override fun onDestroy() {
        api?.unbind(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request permission for microphone
        ActivityCompat.requestPermissions(this, permissions, 200)

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
