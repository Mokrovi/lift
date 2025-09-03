package com.example.myapplication

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
// Import your chosen HTTP Server library here
// e.g., import io.ktor.server.engine.*
// e.g., import io.ktor.server.netty.*
// e.g., import io.ktor.server.routing.*
// e.g., import io.ktor.server.application.*
// e.g., import io.ktor.server.response.*

class NetworkSignalService : Service() {

    private val TAG = "NetworkSignalService"
    // private var httpServer: NettyApplicationEngine? = null // Example for Ktor
    private var isStreamActivityRunning = false // Simple state tracking

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating.")
        startHttpServer()
    }

    private fun startHttpServer() {
        // TODO: Implement your HTTP server initialization and start logic here
        // This is a placeholder. You need to replace this with actual server code
        // using a library like Ktor, NanoHTTPD, or Sparkjava.

        // Example outline for Ktor:
        /*
        httpServer = embeddedServer(Netty, port = 8080) { // Choose an available port
            routing {
                get("/toggle-stream") {
                    handleToggleStream()
                    call.respondText("Signal received")
                }
            }
        }.start(wait = false) // Start non-blocking if in main thread
        */

        Log.d(TAG, "HTTP Server should be started here on a specific IP and port.")
        // You'll need to find the device's local IP address to be accessible on the LAN.
        // The service will listen on http://<DEVICE_IP_ON_LAN>:8080/toggle-stream
    }

    private fun handleToggleStream() {
        if (!isStreamActivityRunning) {
            Log.d(TAG, "Starting CameraStreamActivity.")
            val intent = Intent(this, CameraStreamActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // You could pass the STREAM_URL here if it's dynamic
            }
            startActivity(intent)
            isStreamActivityRunning = true
            // Note: More robust state checking might involve checking if activity is really in foreground
        } else {
            Log.d(TAG, "Sending close broadcast to CameraStreamActivity.")
            val intent = Intent(ACTION_CLOSE_CAMERA_STREAM)
            sendBroadcast(intent)
            isStreamActivityRunning = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started.")
        // If the service is killed, it will be restarted.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying.")
        // TODO: Stop your HTTP server here
        /*
        httpServer?.stop(1_000, 5_000)
        */
        Log.d(TAG, "HTTP Server should be stopped here.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }
}
