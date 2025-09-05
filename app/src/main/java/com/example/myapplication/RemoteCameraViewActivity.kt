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
import androidx.media3.common.PlaybackException // Added import
import androidx.media3.common.Player // Added import
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

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
        setContentView(R.layout.activity_remote_camera_view)
        Log.d(TAG, "onCreate called.")

        playerView = findViewById(R.id.player_view)
        statusTextView = findViewById(R.id.status_textview)

        NetworkSignalService.isRemoteCameraActivityRunning = true

        val remoteIpAddress = intent.getStringExtra("REMOTE_IP_ADDRESS")

        if (remoteIpAddress != null) {
            Log.d(TAG, "Received IP Address: $remoteIpAddress")
            // MODIFIED PORT HERE
            val streamUrlForStatus = "rtsp://$remoteIpAddress:8554/live/stream"
            statusTextView.text = "Attempting to stream from: $streamUrlForStatus"
        } else {
            Log.e(TAG, "No IP Address received in Intent. Finishing activity.")
            statusTextView.text = "Error: No IP Address received. Closing."
            statusTextView.postDelayed({ finish() }, 3000)
        }

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

        // MODIFIED PORT HERE
        val streamUrl = "rtsp://$remoteIpAddress:8554/live/stream"
        Log.d(TAG, "Initializing player for URL: $streamUrl")

        try {
            player = ExoPlayer.Builder(this).build()
            playerView.player = player

            // Добавьте слушатель событий игрока
            player?.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error: ${error.message}", error)
                    statusTextView.text = "Player error: ${error.message}"
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            Log.d(TAG, "Player ready - playback should start")
                            // statusTextView.text = "Streaming started successfully" // Keep previous status or update carefully
                        }
                        Player.STATE_BUFFERING -> {
                            Log.d(TAG, "Player buffering")
                            // statusTextView.text = "Buffering..." // Optional: update status
                        }
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Player ended")
                            // statusTextView.text = "Stream ended." // Optional: update status
                        }
                        Player.STATE_IDLE -> {
                            Log.d(TAG, "Player idle")
                            // statusTextView.text = "Player idle." // Optional: update status
                        }
                    }
                }
            })

            val mediaItem = MediaItem.fromUri(streamUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
            statusTextView.text = "Initializing stream from: $streamUrl" // More accurate initial status
            Log.d(TAG, "ExoPlayer initialized, preparation started, playWhenReady set.")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ExoPlayer", e)
            statusTextView.text = "Error initializing player: ${e.message}"
        }
    }

    private fun releasePlayer() {
        player?.removeListener(playerListener) // Make sure to remove the listener
        player?.let {
            it.stop()
            it.release()
            player = null
            Log.d(TAG, "ExoPlayer released.")
        }
    }
    // Define the listener as a property to be able to remove it
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            statusTextView.text = "Player error: ${error.message}"
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString = when (playbackState) {
                Player.STATE_IDLE -> "Player.STATE_IDLE"
                Player.STATE_BUFFERING -> "Player.STATE_BUFFERING"
                Player.STATE_READY -> "Player.STATE_READY - Playback should start"
                Player.STATE_ENDED -> "Player.STATE_ENDED"
                else -> "UNKNOWN_STATE"
            }
            Log.d(TAG, "onPlaybackStateChanged: $stateString")
            if (playbackState == Player.STATE_READY) {
                statusTextView.text = "Streaming started successfully"
            } else if (playbackState == Player.STATE_BUFFERING) {
                 statusTextView.text = "Buffering..."
            }
        }
    }


    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            if (player == null && intent.getStringExtra("REMOTE_IP_ADDRESS") != null) {
                initializePlayer()
            }
            playerView.onResume() 
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            if (player == null && intent.getStringExtra("REMOTE_IP_ADDRESS") != null) {
                initializePlayer()
            }
        }
        playerView.onResume()
        player?.playWhenReady = true 
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false 
        playerView.onPause()
        if (Build.VERSION.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        NetworkSignalService.isRemoteCameraActivityRunning = false
        unregisterReceiver(closeReceiver)
        Log.d(TAG, "Close broadcast receiver unregistered.")
        releasePlayer()
    }
}
