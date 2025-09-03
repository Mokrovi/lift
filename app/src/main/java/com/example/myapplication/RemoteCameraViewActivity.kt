package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RemoteCameraViewActivity : AppCompatActivity() {

    private val TAG = "RemoteCameraView"

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CLOSE_REMOTE_CAMERA_VIEW) {
                Log.d(TAG, "Received close broadcast. Finishing Activity.")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Corrected this line
        Log.d(TAG, "onCreate called.")
        NetworkSignalService.isRemoteCameraActivityRunning = true // Signal that activity is running

        // Create a TextView programmatically for simplicity
        // In a real application, you would typically use an XML layout
        val textView = TextView(this)
        textView.textSize = 18f
        setContentView(textView)

        val remoteIpAddress = intent.getStringExtra("REMOTE_IP_ADDRESS")

        if (remoteIpAddress != null) {
            Log.d(TAG, "Received IP Address: $remoteIpAddress")
            textView.text = "Attempting to stream from: $remoteIpAddress\n\n(Video player not yet implemented)\n\nSend POST to /trigger again to close this window."
            // TODO: Initialize your video player here
            // e.g., using ExoPlayer, VideoView, or another library,
            // and start playing the stream from remoteIpAddress.
            // You might also need to know the port and path to the stream.
        } else {
            Log.e(TAG, "No IP Address received in Intent")
            textView.text = "Error: No IP Address received."
            // TODO: Handle the error, perhaps by closing the Activity
        }

        // Register the broadcast receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, IntentFilter(ACTION_CLOSE_REMOTE_CAMERA_VIEW), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, IntentFilter(ACTION_CLOSE_REMOTE_CAMERA_VIEW))
        }
        Log.d(TAG, "Close broadcast receiver registered.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        NetworkSignalService.isRemoteCameraActivityRunning = false // Signal that activity is no longer running
        unregisterReceiver(closeReceiver) // Unregister the broadcast receiver
        Log.d(TAG, "Close broadcast receiver unregistered.")
    }
}
