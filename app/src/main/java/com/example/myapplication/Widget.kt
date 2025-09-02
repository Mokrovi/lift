package com.example.myapplication

import android.net.Uri

enum class WidgetType {
    WEATHER,
    CLOCK,
    CAMERA,
    AD,
    TEXT,
    GIF,
    VIDEO
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
    var textData: String? = null, // Renamed from 'data' for clarity, used for TEXT widget
    // Weather-specific data
    var cityName: String? = null, // For WEATHER widget
    var temperature: Double? = null, // For WEATHER widget
    var weatherDescription: String? = null, // For WEATHER widget
    var weatherIconUrl: String? = null // For WEATHER widget
)

// The HorizontalAlignmentMode enum has been removed from here
// to avoid redeclaration. It is defined in HorizontalAlignmentMode.kt.