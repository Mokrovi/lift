package com.example.myapplication

import android.net.Uri

enum class WidgetType {
    WEATHER,
    CLOCK,
    CAMERA,
    AD,
    TEXT
}

data class WidgetData(
    val id: String,
    val type: WidgetType,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var cornerRadius: Int = 12, // Default to 12dp as per step 7
    var mediaUri: Uri? = null, // For AD type,
    var backgroundColor: Int? = null, // <--- НОВОЕ ПОЛЕ ДЛЯ ЦВЕТА ФОНА
    var zIndex: Int = 0,
    var data: String? = null // For storing additional widget-specific data
)