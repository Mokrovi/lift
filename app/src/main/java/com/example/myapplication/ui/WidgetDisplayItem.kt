package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image // <-- Добавлен импорт Coil
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <-- ДОБАВЛЕН ИМПОРТ ДЛЯ ПАЛИТРЫ
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape // <-- ДОБАВЛЕН ИМПОРТ ДЛЯ ПАЛИТРЫ
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
// import androidx.compose.material.icons.filled.Palette // <-- НОВЫЙ ИМПОРТ - ЭТУ СТРОКУ УДАЛЯЕМ (оставляем комментарий как есть)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow // <-- ДОБАВЛЕН ИМПОРТ ДЛЯ ПАЛИТРЫ
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb // <-- ДОБАВЛЕН ИМПОРТ ДЛЯ ПРЕОБРАЗОВАНИЯ ЦВЕТА
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale // <-- Добавлен импорт Coil
import androidx.compose.ui.platform.LocalContext // <-- Добавлен импорт для ImageRequest
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import coil.compose.rememberAsyncImagePainter // <-- Добавлен импорт Coil
import coil.request.ImageRequest // <-- Добавлен импорт для ImageRequest
import coil.size.Precision // <-- Добавлен импорт для ImageRequest
import com.example.myapplication.GifImage // <-- ДОБАВЛЕН ИМПОРТ ДЛЯ GIF
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
                    ?: if ((widgetData.type == WidgetType.CAMERA || widgetData.type == WidgetType.GIF) && widgetData.mediaUri == null) Color.Gray // Обновлено условие для GIF
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (isColliding) BorderStroke(2.dp, Color.Red) else null
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                when (widgetData.type) {
                    WidgetType.WEATHER -> Text(widgetData.data ?: "Location data pending...", style = MaterialTheme.typography.bodyLarge)
                    WidgetType.CLOCK -> {
                        var currentTime by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            while (true) {
                                currentTime = sdf.format(System.currentTimeMillis())
                                delay(1000L)
                            }
                        }
                        Text(currentTime, style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.CAMERA -> {
                        widgetData.mediaUri?.let {
                            Image(painter = rememberAsyncImagePainter(model = it), contentDescription = "Camera feed", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } ?: Text("Нет сигнала", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.AD -> {
                        widgetData.mediaUri?.let { uri ->
                            val context = LocalContext.current
                            // val density = LocalDensity.current // density is already available in this scope
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
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("Advertisement Area", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.TEXT -> {
                        Text(widgetData.data ?: "Text widget", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.GIF -> { // <-- НОВЫЙ КЕЙС ДЛЯ ОТОБРАЖЕНИЯ GIF
                        widgetData.mediaUri?.let {
                            GifImage(
                                data = it,
                                contentDescription = "GIF image",
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Text("No GIF selected", style = MaterialTheme.typography.bodyLarge)
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
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .background(Color.Transparent)
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
                    )
                }
            }
        }
    }
}
