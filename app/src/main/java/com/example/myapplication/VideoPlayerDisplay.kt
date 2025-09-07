package com.example.myapplication

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector // Добавлен импорт
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        // Создаем TrackSelector с ограничениями
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(1280, 720) // Ограничение по высоте 720p
                    .setMaxVideoFrameRate(45)      // Ограничение по FPS
                    .build()
            )
        }

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector) // Устанавливаем TrackSelector
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE // Воспроизведение по кругу
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Контроллер скрыт
            }
        },
        modifier = modifier
    )
}
