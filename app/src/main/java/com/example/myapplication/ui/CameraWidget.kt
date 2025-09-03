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
    val streamUrl: String = "" // Store the complete stream URL (RTSP or HTTP)
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
        options.add("--no-audio") // Disable audio
        LibVLC(context, options)
    }

    val mediaPlayer = remember(libVlc) {
        MediaPlayer(libVlc).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> Log.d("VLC", "Opening stream: ${cameraDevice.streamUrl}")
                    MediaPlayer.Event.Playing -> Log.d("VLC", "Playing stream: ${cameraDevice.streamUrl}")
                    MediaPlayer.Event.Paused -> Log.d("VLC", "Stream paused: ${cameraDevice.streamUrl}")
                    MediaPlayer.Event.Stopped -> Log.d("VLC", "Stream stopped: ${cameraDevice.streamUrl}")
                    MediaPlayer.Event.EncounteredError -> {
                        showError = "Error playing stream"
                        Log.e("VLC", "Error playing stream: ${cameraDevice.streamUrl}")
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
            if (cameraDevice.streamUrl.isNotEmpty()) {
                val media = Media(libVlc, Uri.parse(cameraDevice.streamUrl))
                mediaPlayer.media = media
            } else {
                showError = "Camera stream URL is empty"
                Log.e("CameraStream", "Camera stream URL is empty for device: ${cameraDevice.ipAddress}")
            }
        } catch (e: Exception) {
            showError = "Failed to play stream: ${e.message}"
            Log.e("CameraStream", "Error playing stream ${cameraDevice.streamUrl}", e)
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
                    mediaPlayer.attachViews(this, null, false, false)
                    if (mediaPlayer.media != null && !mediaPlayer.isPlaying) {
                        mediaPlayer.play()
                    }
                }
            },
            update = { vlcVideoLayout ->
                if (mediaPlayer.media == null && cameraDevice.streamUrl.isNotEmpty()) {
                    try {
                        val media = Media(libVlc, Uri.parse(cameraDevice.streamUrl))
                        mediaPlayer.media = media
                    } catch (e: Exception) {
                        showError = "Failed to set media: ${e.message}"
                        Log.e("CameraStream", "Error setting media in update for ${cameraDevice.streamUrl}", e)
                    }
                }
                if (mediaPlayer.media != null && !mediaPlayer.isPlaying && showError == null) {
                     mediaPlayer.play()
                }
                if (!mediaPlayer.vlcVout.areViewsAttached()) {
                    mediaPlayer.attachViews(vlcVideoLayout, null, false, false)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}


suspend fun discoverTrD3121Camera(context: Context): CameraDevice? {
    return withContext(Dispatchers.IO) {
        val staticIp = "192.168.1.200"
        Log.d("CameraDiscovery", "Attempting to connect to static IP: $staticIp")

        // Define a list of stream URLs to try for the static IP
        // Prioritize common paths without credentials first, then with if necessary
        val rtspStreamUrls = listOf(
            "rtsp://admin:admin@$staticIp:554/live/main", // With common credentials
            "rtsp://admin:admin@$staticIp:554/live/sub",
            "rtsp://$staticIp:554/live/main",
            "rtsp://$staticIp:554/live/sub",
            "rtsp://$staticIp:554/stream1",
            "rtsp://admin:admin@$staticIp:554/stream1",
            "rtsp://$staticIp:554/ch01.0",
            "rtsp://admin:admin@$staticIp:554/ch01.0",
            "rtsp://$staticIp:554/cam/realmonitor?channel=1&subtype=0",
            "rtsp://admin:admin@$staticIp:554/cam/realmonitor?channel=1&subtype=0"
        )

        for (url in rtspStreamUrls) {
            Log.d("CameraDiscovery", "Trying stream URL: $url")
            if (isStreamReachable(url)) {
                Log.d("CameraDiscovery", "Successfully connected to stream: $url")
                return@withContext CameraDevice(
                    ipAddress = staticIp,
                    name = "TR-D3121IR2 Camera (Static IP)",
                    streamUrl = url
                )
            }
        }

        // Try M-JPEG stream as a fallback if RTSP fails
        val mjpegUrl = "http://admin:admin@$staticIp/action/stream?subject=mjpeg"
        Log.d("CameraDiscovery", "Trying M-JPEG stream URL: $mjpegUrl")
        if (isStreamReachable(mjpegUrl)) {
            Log.d("CameraDiscovery", "Successfully connected to M-JPEG stream: $mjpegUrl")
            return@withContext CameraDevice(
                ipAddress = staticIp,
                name = "TR-D3121IR2 Camera (Static IP M-JPEG)",
                streamUrl = mjpegUrl
            )
        }
        
        Log.d("CameraDiscovery", "Failed to connect to any stream for static IP: $staticIp")
        null // Camera not found or stream not reachable
    }
}

// discoverCameraWithOnvif, getLocalSubnet, and isCameraReachable functions
// are no longer needed for static IP configuration but kept for potential future use
// or if other parts of the app might still use them.
// If they are truly unused, they can be removed.

private fun discoverCameraWithOnvif(): String? {
    // This function is not used when connecting to a static IP.
    Log.d("CameraDiscovery", "ONVIF discovery skipped (Static IP configured).")
    return null
}

private fun getLocalSubnet(context: Context): String? {
    // This function is not used when connecting to a static IP.
    Log.d("CameraDiscovery", "Subnet discovery skipped (Static IP configured).")
    return null
}


private fun isCameraReachable(ipAddress: String, timeoutMs: Int = 1000): Boolean {
    // This function can still be useful for a direct reachability test if needed,
    // but discoverTrD3121Camera now relies on isStreamReachable.
    val portsToTest = listOf(80, 554, 8000, 8080)
    for (port in portsToTest) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
                Log.d("CameraReachable", "Successfully connected to $ipAddress:$port")
                return true
            }
        } catch (e: Exception) {
            // Log.d("CameraReachable", "Failed to connect to $ipAddress:$port: ${e.message}")
        }
    }
    Log.d("CameraReachable", "No common ports reachable on $ipAddress")
    return false
}

/**
 * Check if a stream (RTSP or HTTP) is reachable by attempting to connect to its host and port.
 */
private fun isStreamReachable(streamUrl: String, timeoutMs: Int = 3000): Boolean {
    return try {
        val uri = Uri.parse(streamUrl)
        val host = uri.host
        var port = uri.port

        if (host == null) {
            Log.e("StreamCheck", "Invalid URL format (host is null): $streamUrl")
            return false
        }

        if (port == -1) {
            port = when (uri.scheme?.lowercase()) {
                "rtsp" -> 554
                "http" -> 80
                "https" -> 443
                else -> {
                    Log.w("StreamCheck", "Unknown scheme or no port for $streamUrl, defaulting to 80 for HTTP assumption")
                    80
                }
            }
        }
        
        Log.d("StreamCheck", "Attempting to connect to $host:$port for URL $streamUrl")
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            Log.d("StreamCheck", "Successfully connected to $host:$port for URL $streamUrl")
            true
        }
    } catch (e: Exception) {
        Log.w("StreamCheck", "Failed to connect to stream $streamUrl (${e.javaClass.simpleName}: ${e.message})")
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
        discoveryState = "Attempting to connect to camera at 192.168.1.200..."
        try {
            val foundCamera = discoverTrD3121Camera(context)
            if (foundCamera != null) {
                cameraDevice = foundCamera
                discoveryState = "Camera connected: ${foundCamera.name} at ${foundCamera.ipAddress}"
                onCameraFound?.invoke(foundCamera)
            } else {
                discoveryState = "Connection failed"
                discoveryError = "Could not connect to TR-D3121IR2 camera at 192.168.1.200. Please check IP, network, and camera power. Also verify stream paths."
            }
        } catch (e: Exception) {
            discoveryState = "Connection error"
            discoveryError = "Error connecting to camera: ${e.message}"
            Log.e("DiscoveredCameraWidgetView", "Error in connection LaunchedEffect", e)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (cameraDevice != null) {
            CameraStreamView(cameraDevice = cameraDevice!!, modifier = Modifier.fillMaxSize())
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (discoveryState.contains("Attempting")) {
                    CircularProgressIndicator()
                }
                Text(text = discoveryState, modifier = Modifier.padding(top = 8.dp), color = Color.Gray)
                discoveryError?.let { error ->
                    Text(text = error, modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp), color = Color.Red)
                }
            }
        }
    }
}
