package com.example.myapplication

import android.net.Uri
import com.example.myapplication.data.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WidgetManager(initialWidgets: List<WidgetData> = emptyList()) {
    private val _widgets = MutableStateFlow(initialWidgets)
    val widgets: StateFlow<List<WidgetData>> = _widgets.asStateFlow()

    private val weatherRepository = WeatherRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    fun updateCurrentLocation(latitude: Double?, longitude: Double?) {
        currentLatitude = latitude
        currentLongitude = longitude
        // После обновления местоположения, запросить данные для всех существующих виджетов погоды
        _widgets.value.forEach { widget ->
            if (widget.type == WidgetType.WEATHER) {
                fetchWeatherData(widget.id)
            }
        }
    }

    fun addWidget(
        type: WidgetType,
        mediaUri: Uri? = null,
        textData: String? = null
        // cityName parameter removed
    ): Boolean {
        val currentWidgets = _widgets.value
        var newX = 16f
        var newY = 16f
        val widgetWidth = if (type == WidgetType.WEATHER) 200f else 150f
        val widgetHeight = if (type == WidgetType.WEATHER) 150f else 100f

        var collision = true
        var attempts = 0
        val maxAttempts = 100

        while (collision && attempts < maxAttempts) {
            collision = false
            val potentialWidgetBounds = WidgetData(
                id = "temp_id_${UUID.randomUUID()}",
                type = type,
                x = newX.toInt(),
                y = newY.toInt(),
                width = widgetWidth.toInt(),
                height = widgetHeight.toInt(),
                mediaUri = mediaUri,
                textData = if (type == WidgetType.TEXT) textData else null,
                cityName = null // cityName is always null initially for new weather widgets
            )
            if (checkCollisionInternal(potentialWidgetBounds, newX, newY, widgetWidth, widgetHeight, currentWidgets)) {
                collision = true
                newX += widgetWidth + 16f
                if (newX + widgetWidth > 1000f) { // Assuming a canvas width limit
                    newX = 16f
                    newY += widgetHeight + 16f
                }
            }
            attempts++
            if (newY + widgetHeight > 2000f) { // Assuming a canvas height limit
                return false
            }
        }

        if (attempts >= maxAttempts) {
            return false
        }

        val newWidgetId = UUID.randomUUID().toString()
        val newWidget = WidgetData(
            id = newWidgetId,
            type = type,
            x = newX.toInt(),
            y = newY.toInt(),
            width = widgetWidth.toInt(),
            height = widgetHeight.toInt(),
            mediaUri = mediaUri,
            textData = if (type == WidgetType.TEXT) textData else null,
            cityName = null // cityName is always null initially
        )
        _widgets.value = currentWidgets + newWidget

        if (type == WidgetType.WEATHER) {
            fetchWeatherData(newWidgetId) // Fetch weather data using coordinates if available
        }
        return true
    }

    fun fetchWeatherData(widgetId: String) {
        val widget = _widgets.value.find { it.id == widgetId }
        if (widget == null || widget.type != WidgetType.WEATHER) {
            return
        }

        coroutineScope.launch {
            val lat = currentLatitude
            val lon = currentLongitude

            if (lat != null && lon != null) {
                // Предполагается, что в WeatherRepository будет метод getCurrentWeatherByCoordinates
                // А в WeatherResponse (или аналогичной модели) будет поле name для города
                val weatherInfo = weatherRepository.getCurrentWeatherByCoordinates(lat, lon)
                if (weatherInfo != null) {
                    _widgets.value = _widgets.value.map {
                        if (it.id == widgetId) {
                            it.copy(
                                temperature = weatherInfo.main.temperature,
                                weatherDescription = weatherInfo.weather.firstOrNull()?.description,
                                weatherIconUrl = weatherInfo.weather.firstOrNull()?.icon?.let { iconCode ->
                                    "https://openweathermap.org/img/wn/$iconCode@2x.png"
                                },
                                cityName = weatherInfo.cityName // <-- ИСПРАВЛЕНО: weatherInfo.name -> weatherInfo.cityName
                            )
                        } else {
                            it
                        }
                    }
                }
            } else {
                val currentWidgetCityName = widget.cityName // Create a stable local variable
                if (currentWidgetCityName != null) { // Fallback if coordinates are not available but city name was somehow set (legacy or future)
                    val weatherInfo = weatherRepository.getCurrentWeatherByCityName(currentWidgetCityName) // <-- Use the local variable
                    if (weatherInfo != null) {
                        _widgets.value = _widgets.value.map {
                            if (it.id == widgetId) {
                                it.copy(
                                    temperature = weatherInfo.main.temperature,
                                    weatherDescription = weatherInfo.weather.firstOrNull()?.description,
                                    weatherIconUrl = weatherInfo.weather.firstOrNull()?.icon?.let { iconCode ->
                                        "https://openweathermap.org/img/wn/$iconCode@2x.png"
                                    }
                                )
                            } else {
                                it
                            }
                        }
                    }
                }
            }
        }
    }


    fun updateWidget(updatedWidget: WidgetData) {
        _widgets.value = _widgets.value.map {
            if (it.id == updatedWidget.id) updatedWidget else it
        }
    }

    fun removeWidget(widgetId: String) {
        _widgets.value = _widgets.value.filterNot { it.id == widgetId }
    }

    fun checkCollisionWithExisting(
        widgetToCheck: WidgetData,
        newX: Float,
        newY: Float,
        newWidth: Float,
        newHeight: Float
    ): Boolean {
        return checkCollisionInternal(widgetToCheck, newX, newY, newWidth, newHeight, _widgets.value, true)
    }

    private fun checkCollisionInternal(
        widget: WidgetData,
        checkX: Float,
        checkY: Float,
        checkWidth: Float,
        checkHeight: Float,
        widgetsToCompareAgainst: List<WidgetData>,
        ignoreSelf: Boolean = false
    ): Boolean {
        val widgetRight = checkX + checkWidth
        val widgetBottom = checkY + checkHeight

        for (existingWidget in widgetsToCompareAgainst) {
            if (ignoreSelf && existingWidget.id == widget.id) {
                continue
            }
            if (checkX < existingWidget.x + existingWidget.width &&
                widgetRight > existingWidget.x &&
                checkY < existingWidget.y + existingWidget.height &&
                widgetBottom > existingWidget.y
            ) {
                return true
            }
        }
        return false
    }
}
