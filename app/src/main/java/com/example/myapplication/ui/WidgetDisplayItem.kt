package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.myapplication.WidgetData
import com.example.myapplication.WidgetType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// Вспомогательная функция для безопасного преобразования Float в Dp,
// гарантируя, что значение не NaN и не отрицательное.
// Минимальный размер можно установить, чтобы избежать нулевых или слишком маленьких виджетов.
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


    LaunchedEffect(widgetData) {
        currentPosition = IntOffset(widgetData.x, widgetData.y)
        // Обновляем с проверкой
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
                Button(
                    onClick = {
                        onDeleteRequest(widgetData)
                        showDeleteDialog = false
                    }
                ) { Text("Да") }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) { Text("Нет") }
            }
        )
    }


    Box(
        modifier = Modifier
            .offset { currentPosition }
            .size(currentWidth, currentHeight)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(isEditMode, widgetData) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = { offset: Offset ->
                            dragStartOffset = Offset(currentPosition.x.toFloat(), currentPosition.y.toFloat())
                        },
                        onDragEnd = {
                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), currentWidth.value, currentHeight.value, true)
                            if (isColliding) {
                                currentPosition = IntOffset(dragStartOffset.x.roundToInt(), dragStartOffset.y.roundToInt())
                            } else {
                                val updatedWidget = widgetData.copy(
                                    x = currentPosition.x,
                                    y = currentPosition.y
                                )
                                onUpdate(updatedWidget)
                            }
                            isColliding = false // Reset after drag
                        },
                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                            change.consume()
                            val newX = currentPosition.x + dragAmount.x
                            val newY = currentPosition.y + dragAmount.y
                            isColliding = checkCollision(widgetData, newX, newY, currentWidth.value, currentHeight.value, true)
                            currentPosition = IntOffset(
                                newX.roundToInt(),
                                newY.roundToInt()
                            )
                        }
                    )
                }
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (widgetData.type == WidgetType.CAMERA) Color.Gray else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (isColliding) BorderStroke(2.dp, Color.Red) else null
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                when (widgetData.type) {
                    WidgetType.WEATHER -> {
                        Text(
                            text = widgetData.data ?: "Location data pending...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    WidgetType.CLOCK -> {
                        var currentTime by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) { // Keyed on Unit, runs once and keeps running
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            while (true) {
                                currentTime = sdf.format(System.currentTimeMillis())
                                delay(1000L)
                            }
                        }
                        Text(currentTime, style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.CAMERA -> {
                        Text("Нет сигнала", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.AD -> {
                        Text("Advertisement Area", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (isEditMode) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp) // Ручка для изменения размера
                            .background(Color.Transparent) // Сделаем ее прозрачной, чтобы не перекрывать содержимое
                            .pointerInput(isEditMode, widgetData) { // Передаем widgetData, чтобы onUpdate имел доступ к последней версии
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { offset: Offset ->
                                            resizeStartSize = Pair(currentWidth, currentHeight)
                                        },
                                        onDragEnd = {
                                            // Гарантируем, что итоговые размеры корректны перед обновлением
                                            val finalWidth = max(currentWidth.value, 48f) // Используем .value для Dp
                                            val finalHeight = max(currentHeight.value, 48f)

                                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), finalWidth, finalHeight, true)

                                            if (isColliding) {
                                                currentWidth = resizeStartSize.first
                                                currentHeight = resizeStartSize.second
                                            } else {
                                                val updatedWidget = widgetData.copy(
                                                    width = finalWidth.roundToInt(),
                                                    height = finalHeight.roundToInt()
                                                )
                                                onUpdate(updatedWidget)
                                            }
                                            isColliding = false // Reset after drag
                                        },
                                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                            change.consume()
                                            // Применяем изменения к текущим размерам
                                            val newWidthDp = currentWidth + with(density) { dragAmount.x.toDp() }
                                            val newHeightDp = currentHeight + with(density) { dragAmount.y.toDp() }

                                            // Устанавливаем минимальные размеры прямо здесь
                                            currentWidth = max(newWidthDp, 48.dp)
                                            currentHeight = max(newHeightDp, 48.dp)

                                            val tempWidth = max(currentWidth.value, 48f)
                                            val tempHeight = max(currentHeight.value, 48f)
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