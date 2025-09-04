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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// We'll assume ACTION_CLOSE_REMOTE_CAMERA_VIEW is globally accessible,
// often defined in a companion object of a service or a separate constants file.
// For example, if it's in NetworkSignalService:
// import com.example.myapplication.NetworkSignalService.ACTION_CLOSE_REMOTE_CAMERA_VIEW

class RemoteCameraViewActivity : AppCompatActivity() {

    private val TAG = "RemoteCameraView"
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var statusTextView: TextView

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CLOSE_REMOTE_CAMERA_VIEW) {
                Log.d(TAG, "Received close broadcast. Finishing Activity.")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_camera_view) // Use the XML layout
        Log.d(TAG, "onCreate called.")

        playerView = findViewById(R.id.player_view)
        statusTextView = findViewById(R.id.status_textview)

        NetworkSignalService.isRemoteCameraActivityRunning = true

        val remoteIpAddress = intent.getStringExtra("REMOTE_IP_ADDRESS")

        if (remoteIpAddress != null) {
            Log.d(TAG, "Received IP Address: $remoteIpAddress")
            // IMPORTANT: Adjust the port and path based on your webcam stream setup.
            // This example uses RTSP on port 8554 and path /live/stream.
            val streamUrl = "rtsp://$remoteIpAddress:8554/live/stream"
            statusTextView.text = "Attempting to stream from: $streamUrl"
            // Player initialization is handled in onResume/onStart for API level compatibility
        } else {
            Log.e(TAG, "No IP Address received in Intent")
            statusTextView.text = "Error: No IP Address received."
            // Consider finishing the activity or showing a more prominent error
        }

        // Register the broadcast receiver
        val intentFilter = IntentFilter(ACTION_CLOSE_REMOTE_CAMERA_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, intentFilter)
        }
        Log.d(TAG, "Close broadcast receiver registered.")
    }

    private fun initializePlayer() {
        val remoteIpAddress = intent.getStringExtra("REMOTE_IP_ADDRESS")
        if (remoteIpAddress == null) {
            Log.e(TAG, "Cannot initialize player, IP address is null.")
            statusTextView.text = "Error: IP Address missing for player."
            return
        }
        // IMPORTANT: Ensure this matches your webcam's streaming URL structure
        val streamUrl = "rtsp://$remoteIpAddress:8554/live/stream"
        Log.d(TAG, "Initializing player for URL: $streamUrl")

        try {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player

            val mediaItem = MediaItem.fromUri(streamUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true // Start playback automatically
            statusTextView.text = "Streaming from: $streamUrl" // Update status
            Log.d(TAG, "ExoPlayer initialized and stream preparation started.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ExoPlayer", e)
            statusTextView.text = "Error initializing player: ${e.message}"
        }
    }

    private fun releasePlayer() {
        player?.let {
            it.stop()
            it.release()
            player = null
            Log.d(TAG, "ExoPlayer released.")
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) { // Android N (API 24) and above
            if (player == null) { // Initialize player if not already initialized
                initializePlayer()
            }
             playerView.onResume() // PlayerView also needs lifecycle calls for ads and overlay interaction
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) { // Android M (API 23) and below, or if player is null
            if (player == null) {
                 initializePlayer()
            }
        }
        playerView.onResume()
        player?.playWhenReady = true // Ensure playback resumes
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false // Pause playback when activity is not in foreground
        playerView.onPause()
        if (Build.VERSION.SDK_INT <= 23) { // Android M (API 23) and below
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) { // Android N (API 24) and above
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        NetworkSignalService.isRemoteCameraActivityRunning = false
        unregisterReceiver(closeReceiver)
        Log.d(TAG, "Close broadcast receiver unregistered.")
        // Player should be released by onStop or onPause, but call here as a final safeguard
        releasePlayer()
    }
}
