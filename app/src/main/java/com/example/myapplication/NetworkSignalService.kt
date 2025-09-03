package com.example.myapplication

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.Status // Added import for Status
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

// Action for the broadcast to close the CameraStreamActivity
const val ACTION_CLOSE_CAMERA_STREAM = "com.example.myapplication.ACTION_CLOSE_CAMERA_STREAM"
// Action for the broadcast to close the RemoteCameraViewActivity
const val ACTION_CLOSE_REMOTE_CAMERA_VIEW = "com.example.myapplication.ACTION_CLOSE_REMOTE_CAMERA_VIEW" // New Action

class NetworkSignalService : Service() {

    private val TAG = "NetworkSignalService"
    private var webServer: MyWebServer? = null
    private val PORT = 8080

    // State tracking for activities
    companion object {
        var isStreamActivityRunning = false // For local CameraStreamActivity
        var isRemoteCameraActivityRunning = false // New: For RemoteCameraViewActivity
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service creating.")
        webServer = MyWebServer(PORT)
        try {
            webServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            val ipAddress = getLocalIpAddress()
            Log.d(TAG, "NanoHTTPD server started on: http://$ipAddress:$PORT")
        } catch (e: IOException) {
            Log.e(TAG, "Error starting NanoHTTPD server", e)
        }
    }

    inner class MyWebServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            Log.d(TAG, "Received request: ${'$'}{session.method} ${'$'}uri from ${'$'}{session.remoteIpAddress}")

            // CORS Preflight request
            if (Method.OPTIONS.equals(session.method)) {
                // Use Status.OK directly after importing Status
                val response = newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, null, 0)
                response.addHeader("Access-Control-Allow-Origin", "*")
                response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE")
                response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                response.addHeader("Access-Control-Max-Age", "86400")
                return response
            }

            // Handler for /trigger (toggle remote stream)
            if ("/trigger" == uri) {
                if (Method.POST.equals(session.method)) {
                    Log.d(TAG, "Handling POST request for /trigger from ${'$'}{session.remoteIpAddress}")
                    return handleTriggerRemoteStream(session) // Delegated to a new function
                } else {
                    // Use Status.METHOD_NOT_ALLOWED directly
                    val response = newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method Not Allowed")
                    response.addHeader("Access-Control-Allow-Origin", "*")
                    return response
                }
            }

            // Existing handler for /toggle-stream (local stream)
            if ("/toggle-stream" == uri && Method.GET == session.method) {
                handleToggleStream() // This is for the local CameraStreamActivity
                 // Use Status.OK directly
                val response = newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "Signal received, toggling local stream.")
                response.addHeader("Access-Control-Allow-Origin", "*")
                return response
            }

            // For other requests, return 404
            // Use Status.NOT_FOUND directly
            val response = newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }
    }

    private fun handleTriggerRemoteStream(session: IHTTPSession): Response {
        return if (!isRemoteCameraActivityRunning) {
            Log.d(TAG, "Starting RemoteCameraViewActivity.")
            val remoteIpAddress = session.remoteIpAddress
            val intent = Intent(this@NetworkSignalService, RemoteCameraViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("REMOTE_IP_ADDRESS", remoteIpAddress)
            }
            try {
                startActivity(intent)
                Log.d(TAG, "Attempting to start RemoteCameraViewActivity for IP: $remoteIpAddress")
                // Use Status.OK directly
                NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Remote stream started.").apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "RemoteCameraViewActivity not found. Did you declare it in AndroidManifest.xml?", e)
                // Corrected to Status.INTERNAL_ERROR
                NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Server error: Target activity not found.").apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            }
        } else {
            Log.d(TAG, "Sending close broadcast to RemoteCameraViewActivity.")
            val intent = Intent(ACTION_CLOSE_REMOTE_CAMERA_VIEW)
            sendBroadcast(intent)
            // Use Status.OK directly
            NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Remote stream stopped.").apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }
    }

    private fun handleToggleStream() {
        if (!isStreamActivityRunning) {
            Log.d(TAG, "Starting CameraStreamActivity.")
            val intent = Intent(this, CameraStreamActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            isStreamActivityRunning = true
        } else {
            Log.d(TAG, "Sending close broadcast to CameraStreamActivity.")
            val intent = Intent(ACTION_CLOSE_CAMERA_STREAM)
            sendBroadcast(intent)
            // isStreamActivityRunning will be set to false by CameraStreamActivity
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses: List<java.net.InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (addr is Inet4Address) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
        }
        return "0.0.0.0"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroying.")
        webServer?.stop()
        Log.d(TAG, "NanoHTTPD server stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
