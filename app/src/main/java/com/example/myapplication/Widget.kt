package com.example.myapplication

import android.net.Uri

enum class WidgetType(val displayName: String) {
    WEATHER("Виджет Погоды"),
    CLOCK("Виджет Часов"),
    CAMERA("Виджет Камеры"),
    AD("Рекламный виджет"),
    TEXT("Текстовый виджет"),
    GIF("GIF виджет"),
    VIDEO("Видео виджет"),
    ONVIF_CAMERA("ONVIF Камера") // Новый тип для ONVIF камеры
}

data class WidgetData(
    val id: String,
    val type: WidgetType,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var cornerRadius: Int = 12,
    var mediaUri: Uri? = null, // Kept as Uri? based on current file content, potential follow-up if WidgetManager expects String?
    var backgroundColor: Int? = null, // Will be used for weather widget background
    var textColor: Int? = null,       // Will be used for weather widget text color
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
    var cityName: String? = null, // This will be used if autoLocate is false OR as a display name
    var temperature: Double? = null,
    var weatherDescription: String? = null,
    var weatherIconUrl: String? = null,
    // New weather settings fields
    var autoLocate: Boolean = true,
    var manualCityName: String? = null // City to use if autoLocate is false
)
