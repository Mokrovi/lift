package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu // <-- ИЗМЕНЕННЫЙ ИМПОРТ
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate // <--- ДОБАВЛЕН ИМПОРТ
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp // <--- ДОБАВЛЕН ИМПОРТ
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import com.example.myapplication.GifImage
import com.example.myapplication.VideoPlayer
import com.example.myapplication.WidgetData
import com.example.myapplication.WidgetType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

fun Float.toSafeDp(minSize: Dp = 1.dp): Dp {
    return if (this.isNaN() || this <= 0f) {
        minSize
    } else {
        this.dp
    }
}

@Composable
fun WidgetDisplayItem(
    widgetData: WidgetData,
    isEditMode: Boolean,
    onUpdate: (WidgetData) -> Unit, // Called after successful drag/resize
    onDeleteRequest: (WidgetData) -> Unit, // Called when delete icon is pressed
    checkCollision: (WidgetData, Float, Float, Float, Float, Boolean) -> Boolean
) {
    // Гарантируем, что начальные размеры корректны
    val initialWidth = remember(widgetData.width) { widgetData.width.toFloat().toSafeDp(minSize = 48.dp) }
    val initialHeight = remember(widgetData.height) { widgetData.height.toFloat().toSafeDp(minSize = 48.dp) }

    var currentPosition by remember { mutableStateOf(IntOffset(widgetData.x, widgetData.y)) }
    var currentWidth by remember { mutableStateOf(initialWidth) }
    var currentHeight by remember { mutableStateOf(initialHeight) }

    var dragStartOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var resizeStartSize by remember { mutableStateOf(Pair(0.dp, 0.dp)) }

    val density = LocalDensity.current
    var isColliding by remember { mutableStateOf(false) }

    // Переменные для толщины рамки в пикселях
    val normalBorderWidth = with(density) { 1f.toDp() }
    val collidingBorderWidth = with(density) { 5f.toDp() }

    // Список цветов для палитры (обернут в remember)
    val colorPalette = remember {
        listOf(
            Color.White, Color(0xFFF0F0F0), Color.LightGray, Color.Gray, Color.DarkGray, Color.Black,
            Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB), // Пастельные
            Color(0xFFFFF9C4), Color(0xFFB2EBF2), Color(0xFFE1BEE7)
        )
    }

    LaunchedEffect(widgetData) {
        currentPosition = IntOffset(widgetData.x, widgetData.y)
        currentWidth = widgetData.width.toFloat().toSafeDp(minSize = 48.dp)
        currentHeight = widgetData.height.toFloat().toSafeDp(minSize = 48.dp)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить виджет?") },
            text = { Text("Вы уверены, что хотите удалить этот виджет?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteRequest(widgetData)
                    showDeleteDialog = false
                }) { Text("Да") }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) { Text("Нет") }
            }
        )
    }

    Box(
        modifier = Modifier
            .offset(fun Density.(): IntOffset { return currentPosition })
            .size(currentWidth, currentHeight)
            .clip(RoundedCornerShape(widgetData.cornerRadius.dp))
            .pointerInput(isEditMode, widgetData) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = { offset -> dragStartOffset = Offset(currentPosition.x.toFloat(), currentPosition.y.toFloat()) },
                        onDragEnd = {
                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), currentWidth.value, currentHeight.value, true)
                            if (isColliding) {
                                currentPosition = IntOffset(dragStartOffset.x.roundToInt(), dragStartOffset.y.roundToInt())
                            } else {
                                onUpdate(widgetData.copy(x = currentPosition.x, y = currentPosition.y))
                            }
                            isColliding = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = currentPosition.x + dragAmount.x
                            val newY = currentPosition.y + dragAmount.y
                            isColliding = checkCollision(widgetData, newX, newY, currentWidth.value, currentHeight.value, true)
                            currentPosition = IntOffset(newX.roundToInt(), newY.roundToInt())
                        }
                    )
                }
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(widgetData.cornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = widgetData.backgroundColor?.let { Color(it) } // Используем цвет из WidgetData
                    ?: if ((widgetData.type == WidgetType.CAMERA || widgetData.type == WidgetType.GIF || widgetData.type == WidgetType.VIDEO || widgetData.type == WidgetType.ONVIF_CAMERA) && widgetData.mediaUri == null) Color.Gray
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (isColliding) {
                BorderStroke(collidingBorderWidth, Color.Red)
            } else {
                if (widgetData.type == WidgetType.WEATHER) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.outline) // MODIFIED HERE
                } else {
                    BorderStroke(normalBorderWidth, MaterialTheme.colorScheme.outline)
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                when (widgetData.type) {
                    WidgetType.WEATHER -> WeatherWidgetCard(widget = widgetData)
                    WidgetType.CLOCK -> {
                        var currentTime by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            while (true) {
                                currentTime = sdf.format(System.currentTimeMillis())
                                delay(1000L)
                            }
                        }
                        Text(currentTime, fontSize = (currentHeight.value / 3).sp)
                    }
                    WidgetType.CAMERA -> {
                        widgetData.mediaUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(model = it),
                                contentDescription = "Camera feed",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Text("Нет сигнала", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.ONVIF_CAMERA -> {

                        // Формируем URL с Basic Authentication: http://username:password@ip/path
                        val testMjpegUrl = "http://admin:admin@192.168.1.200/action/stream?subject=mjpeg"

                        val testCameraData = widgetData.copy(
                            mediaUri = Uri.parse(testMjpegUrl)
                        )
                        OnvifCameraDisplay(
                            widgetData = testCameraData,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                        )

                    }
                    WidgetType.AD -> {
                        widgetData.mediaUri?.let { uri ->
                            val context = LocalContext.current
                            val imageWidthPx = with(density) { currentWidth.roundToPx() }
                            val imageHeightPx = with(density) { currentHeight.roundToPx() }

                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(uri)
                                    .size(imageWidthPx, imageHeightPx) // Request image at exact size in pixels
                                    .precision(Precision.EXACT)
                                    .build()
                            )
                            Image(
                                painter = painter,
                                contentDescription = "Advertisement background",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("Advertisement Area", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.TEXT -> {
                        EditableTextWidget(
                            initialWidgetData = widgetData,
                            onWidgetDataChange = onUpdate, // Передаем существующий onUpdate
                            modifier = Modifier.fillMaxSize() // Чтобы занимал все доступное место в Card
                        )
                    }
                    WidgetType.GIF -> {
                        widgetData.mediaUri?.let {
                            GifImage(
                                data = it,
                                contentDescription = "GIF image",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                            )
                        } ?: Text("No GIF selected", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.VIDEO -> {
                        widgetData.mediaUri?.let {
                            VideoPlayer(
                                videoUri = it,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                            )
                        } ?: Text("No video selected", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (isEditMode) {
                    // Кнопка удаления
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd).size(36.dp).padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                    }


                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 30.dp) // Отступ от ручки ресайза
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorPalette.forEach { colorItem ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(colorItem, CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        val updatedWidget = widgetData.copy(backgroundColor = colorItem.toArgb())
                                        onUpdate(updatedWidget)
                                    }
                            )
                        }
                    }

                    // Ручка для изменения размера
                    Box(
                        contentAlignment = Alignment.Center, 
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp) 
                            .clip(CircleShape) 
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) 
                            .pointerInput(isEditMode, widgetData) {
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { resizeStartSize = Pair(currentWidth, currentHeight) },
                                        onDragEnd = {
                                            val finalWidth = kotlin.math.max(currentWidth.value, 48f)
                                            val finalHeight = kotlin.math.max(currentHeight.value, 48f)
                                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), finalWidth, finalHeight, true)
                                            if (isColliding) {
                                                currentWidth = resizeStartSize.first
                                                currentHeight = resizeStartSize.second
                                            } else {
                                                onUpdate(widgetData.copy(width = finalWidth.roundToInt(), height = finalHeight.roundToInt()))
                                            }
                                            isColliding = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newWidthDp = currentWidth + with(density) { dragAmount.x.toDp() }
                                            val newHeightDp = currentHeight + with(density) { dragAmount.y.toDp() }
                                            currentWidth = androidx.compose.ui.unit.max(newWidthDp, 48.dp)
                                            currentHeight = androidx.compose.ui.unit.max(newHeightDp, 48.dp)
                                            val tempWidth = kotlin.math.max(currentWidth.value, 48f)
                                            val tempHeight = kotlin.math.max(currentHeight.value, 48f)
                                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), tempWidth, tempHeight, true)
                                        }
                                    )
                                }
                            }
                    ) {
                        Icon(
                            Icons.Filled.Menu, 
                            contentDescription = "Изменить размер",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(-45f), // <--- ДОБАВЛЕН ПОВОРОТ 
                            tint = MaterialTheme.colorScheme.onPrimary 
                        )
                    }
                }
            }
        }
    }
}
