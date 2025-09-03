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

    // Изменяем тип mediaUri на String? чтобы соответствовать RTSP URL
    fun addWidget(
        type: WidgetType,
        mediaUri: String? = null, // Параметр функции остается String?
        textData: String? = null
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
            // При создании WidgetData для проверки коллизий, конвертируем String? в Uri?
            val potentialWidgetBounds = WidgetData(
                id = "temp_id_${UUID.randomUUID()}",
                type = type,
                x = newX.toInt(),
                y = newY.toInt(),
                width = widgetWidth.toInt(),
                height = widgetHeight.toInt(),
                mediaUri = mediaUri?.let { Uri.parse(it) }, // Конвертация
                textData = if (type == WidgetType.TEXT) textData else null,
                cityName = null
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
        // При создании финального WidgetData, также конвертируем String? в Uri?
        val newWidget = WidgetData(
            id = newWidgetId,
            type = type,
            x = newX.toInt(),
            y = newY.toInt(),
            width = widgetWidth.toInt(),
            height = widgetHeight.toInt(),
            mediaUri = mediaUri?.let { Uri.parse(it) }, // Конвертация
            textData = if (type == WidgetType.TEXT) textData else null,
            cityName = null
        )
        _widgets.value = currentWidgets + newWidget

        if (type == WidgetType.WEATHER) {
            fetchWeatherData(newWidgetId)
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
                                cityName = weatherInfo.cityName
                            )
                        } else {
                            it
                        }
                    }
                }
            } else {
                // val currentWidgetCityName = widget.cityName // This would require cityName in WidgetData to be Uri?
                // For now, let's assume if lat/lon are null, we don't fetch by city name from widget
                // Or, WidgetData needs a separate cityName field of type String?
                 val currentWidgetCityName = widget.cityName // Assuming cityName is String? in WidgetData
                if (currentWidgetCityName != null) {
                    val weatherInfo = weatherRepository.getCurrentWeatherByCityName(currentWidgetCityName)
                    if (weatherInfo != null) {
                        _widgets.value = _widgets.value.map {
                            if (it.id == widgetId) {
                                it.copy(
                                    temperature = weatherInfo.main.temperature,
                                    weatherDescription = weatherInfo.weather.firstOrNull()?.description,
                                    weatherIconUrl = weatherInfo.weather.firstOrNull()?.icon?.let { iconCode ->
                                        "https://openweathermap.org/img/wn/$iconCode@2x.png"
                                    }
                                    // cityName will remain as it was or be updated if API provides it
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
