package com.example.myapplication.ui

import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.WidgetData
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList

@Composable
fun OnvifCameraDisplay(widgetData: WidgetData, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }

    val rtspUrl = widgetData.mediaUri
    val rtspUrlString = rtspUrl?.toString()

    if (rtspUrlString.isNullOrEmpty()) { 
        Box(modifier = modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Text("RTSP URL не указан в WidgetData", color = Color.White)
        }
        return
    }

    // VLC player setup
    val libVlc = remember {
        val options = ArrayList<String>()
        options.add("--rtsp-tcp")
        LibVLC(context, options)
    }

    val mediaPlayer = remember(libVlc) {
        MediaPlayer(libVlc).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> Log.d("VLC_OnvifCameraDisplay", "Opening stream: $rtspUrl")
                    MediaPlayer.Event.Playing -> Log.d("VLC_OnvifCameraDisplay", "Playing stream: $rtspUrl")
                    MediaPlayer.Event.Paused -> Log.d("VLC_OnvifCameraDisplay", "Stream paused: $rtspUrl")
                    MediaPlayer.Event.Stopped -> Log.d("VLC_OnvifCameraDisplay", "Stream stopped: $rtspUrl")
                    MediaPlayer.Event.EncounteredError -> {
                        showError = "Ошибка воспроизведения потока"
                        Log.e("VLC_OnvifCameraDisplay", "Error playing stream: $rtspUrl")
                    }
                    else -> Unit // Handle other events as needed
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("VLC_OnvifCameraDisplay", "Disposing OnvifCameraDisplay for $rtspUrl. Stopping and releasing media player.")
            mediaPlayer.stop()
            mediaPlayer.release()
            libVlc.release()
        }
    }

    LaunchedEffect(rtspUrl, mediaPlayer) {
        if (rtspUrl != null && !rtspUrl.toString().isNullOrEmpty()) {
            try {
                Log.d("VLC_OnvifCameraDisplay", "Setting media for $rtspUrl")
                val media = Media(libVlc, rtspUrl)
                mediaPlayer.media = media
                media.release() // Media object can be released after being set to MediaPlayer
                // Play will be called after views are attached in AndroidView's factory or update block
            } catch (e: Exception) {
                showError = "Ошибка установки медиа: ${e.message}"
                Log.e("VLC_OnvifCameraDisplay", "Error setting media for $rtspUrl", e)
            }
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
                    Log.d("VLC_OnvifCameraDisplay", "Factory: Attaching views for $rtspUrl")
                    mediaPlayer.attachViews(this, null, false, false)
                    if (mediaPlayer.media != null && !mediaPlayer.isPlaying) {
                        Log.d("VLC_OnvifCameraDisplay", "Factory: Playing media for $rtspUrl")
                        mediaPlayer.play()
                    }
                }
            },
            update = { vlcVideoLayout ->
                Log.d("VLC_OnvifCameraDisplay", "Update block for $rtspUrl. Current media: ${mediaPlayer.media?.uri}, isPlaying: ${mediaPlayer.isPlaying}")
                // This block is crucial if the rtspUrl can change for an existing widget,
                // or if play needs to be re-triggered.

                // If media is not set or changed, set it again.
                if (mediaPlayer.media == null || mediaPlayer.media?.uri != rtspUrl) {
                    if (rtspUrl != null && !rtspUrl.toString().isNullOrEmpty()) {
                        try {
                            Log.d("VLC_OnvifCameraDisplay", "Update: Re-setting media for $rtspUrl")
                            val newMedia = Media(libVlc, rtspUrl)
                            mediaPlayer.media = newMedia
                            newMedia.release()
                             // Ensure views are attached before playing
                            if (!mediaPlayer.vlcVout.areViewsAttached()) {
                                Log.d("VLC_OnvifCameraDisplay", "Update: Re-attaching views for $rtspUrl")
                                mediaPlayer.attachViews(vlcVideoLayout, null, false, false)
                            }
                            Log.d("VLC_OnvifCameraDisplay", "Update: Playing new media for $rtspUrl")
                            mediaPlayer.play()
                        } catch (e: Exception) {
                            showError = "Ошибка обновления медиа: ${e.message}"
                            Log.e("VLC_OnvifCameraDisplay", "Error updating media for $rtspUrl", e)
                        }
                    }
                } else if (mediaPlayer.media != null && !mediaPlayer.isPlaying && showError == null) {
                    // If media is set but not playing, play it.
                    Log.d("VLC_OnvifCameraDisplay", "Update: Media is set but not playing. Calling play for $rtspUrl")
                    mediaPlayer.play()
                } else if (!mediaPlayer.vlcVout.areViewsAttached()) {
                     // Ensure views are attached, if for some reason they got detached.
                    Log.d("VLC_OnvifCameraDisplay", "Update: Views not attached. Re-attaching for $rtspUrl")
                    mediaPlayer.attachViews(vlcVideoLayout, null, false, false)
                    if (mediaPlayer.media != null && !mediaPlayer.isPlaying && showError == null) {
                         mediaPlayer.play()
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}
