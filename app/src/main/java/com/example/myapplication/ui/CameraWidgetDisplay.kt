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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.ArrayList

@Composable
fun OnvifCameraDisplay(widgetData: WidgetData, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }

    val mediaUrl = widgetData.mediaUri 
    val mediaUrlString = mediaUrl?.toString()

    if (mediaUrlString.isNullOrEmpty()) { 
        Box(modifier = modifier
            .fillMaxSize()
            .background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Text("Media URL не указан в WidgetData", color = Color.White)
        }
        return
    }

    // VLC player setup
    val libVlc = remember(mediaUrlString) { // Ключ mediaUrlString, чтобы опции пересчитывались при смене URL
        val options = ArrayList<String>()
        options.add("--avcodec-hw=any")        // Пытаемся использовать аппаратное декодирование
        options.add("--network-caching=1000")  // Увеличиваем кэш сети (в миллисекундах)
        options.add("--live-caching=500")     // Кэширование для живых потоков (в миллисекундах)
        
        // Специфичные опции в зависимости от типа URL
        if (mediaUrlString.startsWith("rtsp://")) {
            options.add("--rtsp-tcp")          // Использовать RTSP через TCP (более надежно, чем UDP)
            options.add("--skip-frames")       // Пропускать кадры при проблемах с буферизацией (для VBR)
            options.add("--rtsp-frame-buffer-size=1000000") // Буфер для RTSP (размер в байтах)
        } else if (mediaUrlString.startsWith("http")) {
            // Опции для HTTP MJPEG потоков
            // options.add("--mjpeg-fps=25") // Можно указать ожидаемую частоту кадров, если сервер ее не передает
            // options.add("--sout-mjpeg-q=100") // Эта опция больше для исходящего потока, если VLC кодирует в MJPEG
            // Для входящего MJPEG обычно достаточно стандартных опций и хорошего network-caching
        }
        LibVLC(context, options)
    }

    val mediaPlayer = remember(libVlc) {
        MediaPlayer(libVlc).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> Log.d("VLC_Player", "Opening stream: $mediaUrlString")
                    MediaPlayer.Event.Playing -> Log.d("VLC_Player", "Playing stream: $mediaUrlString")
                    MediaPlayer.Event.Paused -> Log.d("VLC_Player", "Stream paused: $mediaUrlString")
                    MediaPlayer.Event.Stopped -> Log.d("VLC_Player", "Stream stopped: $mediaUrlString")
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e("VLC_Player", "Error playing stream: $mediaUrlString")
                        // Обновляем showError в основном потоке, если хотим показать ошибку в UI
                        // kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) { // Пример, если нужно из другого потока
                        //     showError = "Ошибка воспроизведения потока"
                        // }
                    }
                    else -> Unit // Handle other events as needed
                }
            }
        }
    }

    DisposableEffect(mediaUrlString) { // Добавляем mediaUrlString, чтобы эффект перезапускался при смене URL
        onDispose {
            Log.d("VLC_Player", "Disposing Player for $mediaUrlString. Stopping and releasing media player.")
            mediaPlayer.stop()
            mediaPlayer.setEventListener(null) // Удаляем слушателя
            mediaPlayer.release() }
    }
    // Освобождение libVlc, когда сам Composable уходит с экрана
    DisposableEffect(Unit) {
        onDispose {
            Log.d("VLC_Player", "Disposing LibVLC instance.")
            libVlc.release()
        }
    }

    LaunchedEffect(mediaUrlString, mediaPlayer) { 
        if (mediaUrlString.isNullOrEmpty()) { // Проверка была выше, но дублируем для ясности
            withContext(Dispatchers.Main) {
                showError = "Media URI отсутствует"
            }
            mediaPlayer?.stop()
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d("VLC_Player", "Попытка установить медиа: $mediaUrlString в IO-контексте")
                val media = Media(libVlc, Uri.parse(mediaUrlString))
                
                // Если это HTTP MJPEG, можно добавить специфичные медиа-опции здесь (с двоеточием)
                if (mediaUrlString.startsWith("http")) {
                    media.addOption(":no-audio") // MJPEG обычно без звука
                    // media.addOption(":mjpeg-caching=500") // Индивидуальный кеш для этого медиа
                }

                mediaPlayer?.media = media 
                media.release() 

                mediaPlayer?.play() 
                Log.d("VLC_Player", "Воспроизведение запущено для: $mediaUrlString")

                withContext(Dispatchers.Main) {
                    showError = null // Сбрасываем ошибку, если все прошло успешно
                }

            } catch (e: Exception) {
                Log.e("VLC_Player", "Ошибка при установке медиа или воспроизведении: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showError = "Ошибка медиа: ${e.message}" // Отображаем ошибку в UI
                }
            }
        }
    }

    if (showError != null) {
        Box(modifier = modifier
            .fillMaxSize()
            .background(Color.Black), contentAlignment = Alignment.Center) {
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
                    Log.d("VLC_Player", "Factory: Attaching views for $mediaUrlString")
                    if(mediaPlayer.vlcVout.areViewsAttached()){
                        mediaPlayer.detachViews()
                    }
                    mediaPlayer.attachViews(this, null, false, false)
                    // Не вызываем play здесь, если LaunchedEffect уже это делает
                }
            },
            update = { vlcVideoLayout ->
                Log.d("VLC_Player", "Update block for $mediaUrlString. Current media: ${mediaPlayer.media?.uri}, isPlaying: ${mediaPlayer.isPlaying}, showError: $showError")
                
                // Логика обновления, если URL изменился или плеер не играет, когда должен:
                val currentPlayingUri = mediaPlayer.media?.uri
                if (mediaUrl != null && currentPlayingUri != mediaUrl && showError == null) {
                    // URL изменился, нужно перезапустить с новым URL (это должен обработать LaunchedEffect)
                    Log.d("VLC_Player", "Update: Media URL changed or player not set up. LaunchedEffect should handle this.")
                    // Можно дополнительно вызвать mediaPlayer.stop() здесь, если это необходимо перед сменой media
                } else if (mediaPlayer.media != null && !mediaPlayer.isPlaying && showError == null) {
                    Log.d("VLC_Player", "Update: Media is set but not playing and no error. Calling play for $mediaUrlString")
                    mediaPlayer.play()
                } 
                // Переприсоединение view, если они отсоединены
                if (!mediaPlayer.vlcVout.areViewsAttached()) {
                    Log.d("VLC_Player", "Update: Views not attached. Re-attaching for $mediaUrlString")
                    mediaPlayer.attachViews(vlcVideoLayout, null, false, false)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }
}
