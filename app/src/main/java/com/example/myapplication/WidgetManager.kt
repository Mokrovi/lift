package com.example.myapplication

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class WidgetManager(initialWidgets: List<WidgetData> = emptyList()) {
    private val _widgets = MutableStateFlow(initialWidgets)
    val widgets: StateFlow<List<WidgetData>> = _widgets.asStateFlow()

    fun addWidget(type: WidgetType, mediaUri: Uri? = null): Boolean {
        val currentWidgets = _widgets.value
        var newX = 16f
        var newY = 16f
        val widgetWidth = 150f // Default width, can be type-specific
        val widgetHeight = 100f // Default height, can be type-specific

        var collision = true
        var attempts = 0 // To prevent infinite loop if no space
        val maxAttempts = 100 // Example limit

        while (collision && attempts < maxAttempts) {
            collision = false
            // Temporary widget for collision check (using Floats for checking logic)
            val potentialWidgetBounds = WidgetData( // This is only for bounds checking, not added to list
                id = "temp_id_${UUID.randomUUID()}", 
                type = type,
                x = newX.toInt(), // Use Int here if checkCollisionInternal expects Int in WidgetData
                y = newY.toInt(),
                width = widgetWidth.toInt(),
                height = widgetHeight.toInt(),
                mediaUri = mediaUri 
            )
            
            // Assuming checkCollisionInternal can conceptually work with Float bounds
            // for the *check values* even if existingWidget data is Int.
            // If checkCollisionInternal strictly requires WidgetData with Ints, then potentialWidgetBounds would need Ints
            // and the checkX, checkY parameters in checkCollisionInternal might need to be Floats.
            // For now, let's assume checkCollisionInternal is robust enough or WidgetData in it is for comparison structure.
            if (checkCollisionInternal(potentialWidgetBounds, newX, newY, widgetWidth, widgetHeight, currentWidgets)) {
                collision = true
                newX += widgetWidth + 16f 
                if (newX + widgetWidth > 1000f) { // Arbitrary canvas width
                    newX = 16f
                    newY += widgetHeight + 16f
                }
            }
            attempts++
            if (newY + widgetHeight > 2000f) { // Arbitrary canvas height
                return false // No space found
            }
        }

        if (attempts >= maxAttempts) {
            return false // No space found after max attempts
        }

        val newWidget = WidgetData(
            id = UUID.randomUUID().toString(),
            type = type,
            x = newX.toInt(),
            y = newY.toInt(),
            width = widgetWidth.toInt(),
            height = widgetHeight.toInt(),
            mediaUri = mediaUri
        )
        _widgets.value = currentWidgets + newWidget
        return true
    }

    fun updateWidget(updatedWidget: WidgetData) {
        _widgets.value = _widgets.value.map {
            if (it.id == updatedWidget.id) updatedWidget else it
        }
    }

    fun removeWidget(widgetId: String) {
        _widgets.value = _widgets.value.filterNot { it.id == widgetId }
    }

    // Renamed for clarity, to be used by WidgetDisplayItem
    // newX, newY, etc are Floats from gesture detection in WidgetDisplayItem
    fun checkCollisionWithExisting(
        widgetToCheck: WidgetData, // The widget being dragged/resized
        newX: Float,
        newY: Float,
        newWidth: Float,
        newHeight: Float
    ): Boolean {
        // Pass true for ignoreSelf because widgetToCheck is already in _widgets.value
        return checkCollisionInternal(widgetToCheck, newX, newY, newWidth, newHeight, _widgets.value, true)
    }

    // Internal collision check logic
    private fun checkCollisionInternal(
        widget: WidgetData, // The widget whose bounds are being checked (can be temp or existing)
        checkX: Float,      // The X position to check (Float)
        checkY: Float,      // The Y position to check (Float)
        checkWidth: Float,  // The width to check (Float)
        checkHeight: Float, // The height to check (Float)
        widgetsToCompareAgainst: List<WidgetData>,
        ignoreSelf: Boolean = false 
    ): Boolean {
        val widgetRight = checkX + checkWidth
        val widgetBottom = checkY + checkHeight

        for (existingWidget in widgetsToCompareAgainst) {
            if (ignoreSelf && existingWidget.id == widget.id) {
                continue
            }
            // Comparison is Float vs Int. This is okay as Ints will be promoted.
            if (checkX < existingWidget.x + existingWidget.width &&
                widgetRight > existingWidget.x &&
                checkY < existingWidget.y + existingWidget.height &&
                widgetBottom > existingWidget.y
            ) {
                return true // Collision detected
            }
        }
        return false // No collision
    }
}