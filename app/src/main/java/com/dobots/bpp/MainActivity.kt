package com.dobots.bpp

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val TAG = "BoomPinPoint Activity"
    private var api: API? = null

    override fun onDestroy() {
        api?.unbind(this)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        API.bind(this, object : Callback<API>() {
            override fun onResult(result: API?) {
                api = result
                return
            }

            override fun onError(error: java.lang.Error?) {
                Log.e(TAG, "Couldn't bind to API service!")
            }

        })

        //TODO: HERE you add event handlers for buttons in the Screen layout

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}