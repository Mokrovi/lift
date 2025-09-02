package com.example.myapplication

import android.net.Uri

enum class WidgetType {
    WEATHER,
    CLOCK,
    CAMERA,
    AD,
    TEXT,
    GIF,
    VIDEO // <-- НОВЫЙ ТИП ВИДЖЕТА ДЛЯ ВИДЕО
}

data class WidgetData(
    val id: String,
    val type: WidgetType,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var cornerRadius: Int = 12, // Default to 12dp as per step 7
    var mediaUri: Uri? = null, // For AD type, and now GIF type
    var backgroundColor: Int? = null,
    var textColor: Int? = null,
    var textSize: Int? = null,
    var isVertical: Boolean = false,
    var horizontalAlignment: HorizontalAlignmentMode = HorizontalAlignmentMode.LEFT, // This will now refer to the enum in HorizontalAlignmentMode.kt
    var zIndex: Int = 0,
    var data: String? = null // For storing additional widget-specific data
)

// The HorizontalAlignmentMode enum has been removed from here
// to avoid redeclaration. It is defined in HorizontalAlignmentMode.kt.