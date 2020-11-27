package com.dobots.bpp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private val TAG = "BoomPinPoint Activity"
    private var api: API? = null

    override fun onDestroy() {
        api?.unbind(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            Snackbar.make(view, "ToDo", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()

            api?.HandleButtonPress()
            // Log.d(TAG, "BUTTON PRESSED!")
        }

    }
}
