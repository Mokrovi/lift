package com.example.myapplication

import android.net.Uri

enum class WidgetType {
    WEATHER,
    CLOCK,
    CAMERA,
    AD,
    TEXT,
    GIF,
    VIDEO,
    ONVIF_CAMERA // Новый тип для ONVIF камеры
}

data class WidgetData(
    val id: String,
    val type: WidgetType,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var cornerRadius: Int = 12,
    var mediaUri: Uri? = null,
    var backgroundColor: Int? = null,
    var textColor: Int? = null,
    var textSize: Int? = null,
    var isVertical: Boolean = false,
    var horizontalAlignment: HorizontalAlignmentMode = HorizontalAlignmentMode.LEFT,
    var zIndex: Int = 0,
    var textData: String? = null,
    var fontFamily: String? = null,
    var lineHeightScale: Float? = null,
    var letterSpacingSp: Float? = null,
    var fontWeight: Int? = null, // <--- НОВОЕ ПОЛЕ ДЛЯ ЖИРНОСТИ
    // Weather-specific data
    var cityName: String? = null,
    var temperature: Double? = null,
    var weatherDescription: String? = null,
    var weatherIconUrl: String? = null
)
