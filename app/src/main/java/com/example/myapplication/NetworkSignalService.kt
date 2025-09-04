package com.example.myapplication

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.Status
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

// Action for the broadcast to close the CameraStreamActivity
const val ACTION_CLOSE_CAMERA_STREAM = "com.example.myapplication.ACTION_CLOSE_CAMERA_STREAM"
// Action for the broadcast to close the RemoteCameraViewActivity
const val ACTION_CLOSE_REMOTE_CAMERA_VIEW = "com.example.myapplication.ACTION_CLOSE_REMOTE_CAMERA_VIEW"

class NetworkSignalService : Service() {

    private val TAG = "NetworkSignalService"
    private var webServer: MyWebServer? = null
    private val PORT = 8080

    companion object {
        var isStreamActivityRunning = false
        var isRemoteCameraActivityRunning = false
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
            Log.d(TAG, "Received request: ${session.method} $uri from ${session.remoteIpAddress}")

            if (Method.OPTIONS.equals(session.method)) {
                val response = newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, null, 0)
                response.addHeader("Access-Control-Allow-Origin", "*")
                response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE")
                response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                response.addHeader("Access-Control-Max-Age", "86400")
                return response
            }

            if ("/trigger" == uri) {
                return if (Method.POST.equals(session.method)) {
                    Log.d(TAG, "Handling POST request for /trigger from ${session.remoteIpAddress}")
                    handleTriggerRemoteStream(session)
                } else {
                    newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method Not Allowed").apply {
                        addHeader("Access-Control-Allow-Origin", "*")
                    }
                }
            }

            if ("/toggle-stream" == uri && Method.GET == session.method) {
                handleToggleStream() // This is for the local CameraStreamActivity
                return newFixedLengthResponse(Status.OK, MIME_PLAINTEXT, "Signal received, toggling local stream.").apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            }

            // For other requests, return 404
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found").apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }
    }

    private fun handleTriggerRemoteStream(session: IHTTPSession): Response {
        return if (!isRemoteCameraActivityRunning) {
            Log.d(TAG, "Attempting to start RemoteCameraViewActivity.")
            val remoteIpAddress = session.remoteIpAddress
            val intent = Intent(this@NetworkSignalService, RemoteCameraViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("REMOTE_IP_ADDRESS", remoteIpAddress)
            }
            try {
                startActivity(intent)
                Log.i(TAG, "RemoteCameraViewActivity started successfully for IP: $remoteIpAddress")
                NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, "Remote stream started.").apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "RemoteCameraViewActivity not found. Check AndroidManifest.xml.", e)
                NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Server error: Target activity not found.").apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            } catch (e: Exception) { // Catch any other exceptions during startActivity
                Log.e(TAG, "Failed to start RemoteCameraViewActivity due to an unexpected error.", e)
                NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Server error: Could not start remote view. ${e.localizedMessage}").apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            }
        } else {
            Log.d(TAG, "Sending close broadcast to RemoteCameraViewActivity.")
            val intent = Intent(ACTION_CLOSE_REMOTE_CAMERA_VIEW)
            sendBroadcast(intent)
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
            // isStreamActivityRunning will be set to true by CameraStreamActivity
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
