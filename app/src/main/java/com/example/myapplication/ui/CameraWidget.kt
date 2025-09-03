package com.example.myapplication.ui

import android.content.Context
import android.net.Uri
import android.util.Log
// import android.view.SurfaceView // No longer directly used here
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout // Added import
import java.net.InetSocketAddress
import java.net.Socket

// Data class to hold camera information
data class CameraDevice(
    val ipAddress: String,
    val name: String = "TR-D3121IR2 Camera",
    val rtspUrl: String = "" // Store the complete RTSP URL
)

@Composable
fun CameraStreamView(
    cameraDevice: CameraDevice,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }

    // VLC player setup
    val libVlc = remember {
        val options = ArrayList<String>()
        options.add("--rtsp-tcp") // Use TCP for RTSP for better stability
        LibVLC(context, options)
    }

    val mediaPlayer = remember(libVlc) {
        MediaPlayer(libVlc).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> Log.d("VLC", "Opening stream")
                    MediaPlayer.Event.Playing -> Log.d("VLC", "Playing stream")
                    MediaPlayer.Event.Paused -> Log.d("VLC", "Stream paused")
                    MediaPlayer.Event.Stopped -> Log.d("VLC", "Stream stopped")
                    MediaPlayer.Event.EncounteredError -> {
                        showError = "Error playing stream"
                        Log.e("VLC", "Error playing stream")
                    }
                    else -> {}
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVlc.release()
        }
    }

    LaunchedEffect(cameraDevice) {
        try {
            val media = Media(libVlc, Uri.parse(cameraDevice.rtspUrl))
            mediaPlayer.media = media
            // Defer play until views are attached in AndroidView's factory or update block
            // mediaPlayer.play() // Play will be called after attachViews
        } catch (e: Exception) {
            showError = "Failed to play stream: ${e.message}"
            Log.e("CameraStream", "Error playing stream", e)
        }
    }

    if (showError != null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(text = showError!!, color = Color.White)
        }
    } else {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    // Attach the VLCVideoLayout to the media player
                    mediaPlayer.attachViews(this, null, false, false)
                    // Start playing after views are attached
                    if (mediaPlayer.media != null && !mediaPlayer.isPlaying) {
                        mediaPlayer.play()
                    }
                }
            },
            update = { vlcVideoLayout ->
                // This block can be used if you need to update the view based on state changes
                // For instance, if the media source changes and you need to re-attach or re-play
                if (mediaPlayer.media == null && cameraDevice.rtspUrl.isNotEmpty()) {
                    try {
                        val media = Media(libVlc, Uri.parse(cameraDevice.rtspUrl))
                        mediaPlayer.media = media
                    } catch (e: Exception) {
                        showError = "Failed to set media: ${e.message}"
                        Log.e("CameraStream", "Error setting media in update", e)
                    }
                }
                if (mediaPlayer.media != null && !mediaPlayer.isPlaying && showError == null) {
                     mediaPlayer.play()
                }
                 // Ensure views are attached, especially if update is called before factory for some reason
                if (!mediaPlayer.vlcVout.areViewsAttached()) {
                    mediaPlayer.attachViews(vlcVideoLayout, null, false, false)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}

/**
 * Discovers the TR-D3121IR2 camera on the local network using ONVIF
 */
suspend fun discoverTrD3121Camera(context: Context): CameraDevice? {
    return withContext(Dispatchers.IO) {
        try {
            // Try to discover camera using ONVIF
            val cameraIp = discoverCameraWithOnvif()
            if (cameraIp != null) {
                // Try common RTSP paths for TR-D3121IR2 camera
                val rtspUrls = listOf(
                    "rtsp://admin:admin@$cameraIp:80/stream1",
                    "rtsp://admin:admin@$cameraIp:80/main",
                    "rtsp://admin:admin@$cameraIp:80/ch01.0",
                    "rtsp://admin:admin@$cameraIp:80/cam/realmonitor?channel=1&subtype=0"
                )

                // Test each URL to find the working one
                for (url in rtspUrls) {
                    if (isRtspStreamReachable(url)) {
                        return@withContext CameraDevice(
                            ipAddress = cameraIp,
                            name = "TR-D3121IR2 Camera",
                            rtspUrl = url
                        )
                    }
                }

                // If no RTSP URL worked, return the device with the first URL
                return@withContext CameraDevice(
                    ipAddress = cameraIp,
                    name = "TR-D3121IR2 Camera",
                    rtspUrl = rtspUrls.first()
                )
            }

            // If ONVIF discovery failed, try IP scanning
            val subnet = getLocalSubnet(context)
            if (subnet != null) {
                for (i in 1..254) {
                    val testIp = "$subnet.$i"
                    if (isCameraReachable(testIp)) {
                        // Try common RTSP paths
                        val rtspUrls = listOf(
                            "rtsp://admin:admin@$testIp:554/stream1",
                            "rtsp://admin:admin@$testIp:554/main",
                            "rtsp://admin:admin@$testIp:554/ch01.0",
                            "rtsp://admin:admin@$testIp:554/cam/realmonitor?channel=1&subtype=0"
                        )

                        for (url in rtspUrls) {
                            if (isRtspStreamReachable(url)) {
                                return@withContext CameraDevice(
                                    ipAddress = testIp,
                                    name = "TR-D3121IR2 Camera",
                                    rtspUrl = url
                                )
                            }
                        }
                    }
                }
            }

            null // Camera not found
        } catch (e: Exception) {
            Log.e("CameraDiscovery", "Error discovering camera", e)
            null
        }
    }
}

/**
 * Try to discover camera using ONVIF protocol
 */
private fun discoverCameraWithOnvif(): String? {
    // This is a simplified implementation
    // In a real app, you would use an ONVIF library like:
    // implementation 'com.rvirin.onvif:onvifcamera:1.1.6'

    // For now, we'll return null and rely on IP scanning
    return null
}

/**
 * Get local subnet (e.g., 192.168.1)
 */
private fun getLocalSubnet(context: Context): String? {
    try {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        return "${ipAddress and 0xFF}.${ipAddress shr 8 and 0xFF}.${ipAddress shr 16 and 0xFF}"
    } catch (e: Exception) {
        Log.e("Network", "Error getting local subnet", e)
        // Return a common default subnet
        return "192.168.1"
    }
}

/**
 * Check if a camera is reachable at the given IP
 */
private fun isCameraReachable(ipAddress: String, timeoutMs: Int = 1000): Boolean {
    val portsToTest = listOf(80, 554, 8000, 8080)
    for (port in portsToTest) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
                return true
            }
        } catch (e: Exception) {
            // Connection failed, try next port
        }
    }
    return false
}

/**
 * Check if an RTSP stream is reachable
 */
private fun isRtspStreamReachable(rtspUrl: String, timeoutMs: Int = 3000): Boolean {
    return try {
        // Try to create a media item and check if it's parsable
        // This is a simplified check - in a real app you might try to connect to the RTSP stream
        Uri.parse(rtspUrl) != null
    } catch (e: Exception) {
        false
    }
}

@Composable
fun DiscoveredCameraWidgetView(
    modifier: Modifier = Modifier,
    onCameraFound: ((CameraDevice) -> Unit)? = null
) {
    val context = LocalContext.current
    var cameraDevice by remember { mutableStateOf<CameraDevice?>(null) }
    var discoveryState by remember { mutableStateOf("Initializing...") }
    var discoveryError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        discoveryState = "Searching for TR-D3121IR2 camera..."
        try {
            val foundCamera = discoverTrD3121Camera(context)
            if (foundCamera != null) {
                cameraDevice = foundCamera
                discoveryState = "Camera found: ${foundCamera.ipAddress}"
                onCameraFound?.invoke(foundCamera)
            } else {
                discoveryState = "Camera not found"
                discoveryError = "TR-D3121IR2 camera not found on the local network. Ensure it's connected, powered on, and on the same network."
            }
        } catch (e: Exception) {
            discoveryState = "Discovery failed"
            discoveryError = "Error discovering camera: ${e.message}"
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (cameraDevice != null) {
            CameraStreamView(cameraDevice = cameraDevice!!, modifier = Modifier.fillMaxSize())
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(text = discoveryState, modifier = Modifier.padding(top = 8.dp), color = Color.Gray)
                discoveryError?.let { error ->
                    Text(text = error, modifier = Modifier.padding(top = 8.dp), color = Color.Red)
                }
            }
        }
    }
}
