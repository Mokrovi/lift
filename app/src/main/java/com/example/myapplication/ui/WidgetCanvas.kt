package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.myapplication.WidgetData
import com.example.myapplication.WidgetManager

@Composable
fun WidgetCanvas(
    widgetManager: WidgetManager,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
    onUpdate: (WidgetData) -> Unit,
    checkCollision: (WidgetData, Float, Float, Float, Float, Boolean) -> Boolean
) {
    val widgets: List<WidgetData> by widgetManager.widgets.collectAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize()) {
        widgets.forEach { widget ->
            WidgetDisplayItem(
                widgetData = widget,
                isEditMode = isEditMode,
                onUpdate = { updatedWidget: WidgetData ->
                    widgetManager.updateWidget(updatedWidget)
                },
                onDeleteRequest = { widgetToDelete: WidgetData ->
                    widgetManager.removeWidget(widgetToDelete.id)
                },
                checkCollision = { widgetData: WidgetData, newX: Float, newY: Float, newWidth: Float, newHeight: Float, ignoreSelf: Boolean ->
                    widgetManager.checkCollisionWithExisting(
                        widgetToCheck = widgetData,
                        newX = newX,
                        newY = newY,
                        newWidth = newWidth,
                        newHeight = newHeight
                    )
                }
            )
        }
    }
}