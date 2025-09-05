package com.example.myapplication

import android.net.Uri
import android.util.Log // Добавим для логирования ошибок
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
        _widgets.value.forEach { widget ->
            if (widget.type == WidgetType.WEATHER && widget.autoLocate) { // Обновляем только если в авто-режиме
                fetchWeatherData(widget.id)
            }
        }
    }

    fun addWidget(
        type: WidgetType,
        mediaUri: String? = null,
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
            val potentialWidgetBounds = WidgetData(
                id = "temp_id_${UUID.randomUUID()}", type = type,
                x = newX.toInt(), y = newY.toInt(),
                width = widgetWidth.toInt(), height = widgetHeight.toInt(),
                mediaUri = mediaUri?.let { Uri.parse(it) },
                textData = if (type == WidgetType.TEXT) textData else null
            )
            if (checkCollisionInternal(potentialWidgetBounds, newX, newY, widgetWidth, widgetHeight, currentWidgets)) {
                collision = true
                newX += widgetWidth + 16f
                if (newX + widgetWidth > 1000f) {
                    newX = 16f
                    newY += widgetHeight + 16f
                }
            }
            attempts++
            if (newY + widgetHeight > 2000f) return false
        }
        if (attempts >= maxAttempts) return false

        val newWidgetId = UUID.randomUUID().toString()
        val newWidget = WidgetData(
            id = newWidgetId, type = type,
            x = newX.toInt(), y = newY.toInt(),
            width = widgetWidth.toInt(), height = widgetHeight.toInt(),
            mediaUri = mediaUri?.let { Uri.parse(it) },
            textData = if (type == WidgetType.TEXT) textData else null
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
            Log.d("WidgetManager", "fetchWeatherData: Widget not found or not a weather widget. ID: $widgetId")
            return
        }

        coroutineScope.launch {
            if (widget.autoLocate) {
                Log.d("WidgetManager", "Fetching weather for widget ${widget.id} in AUTO mode.")
                val lat = currentLatitude
                val lon = currentLongitude
                if (lat != null && lon != null) {
                    Log.d("WidgetManager", "Using coordinates: Lat=$lat, Lon=$lon")
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
                                    cityName = weatherInfo.cityName // Обновляем cityName из API
                                )
                            } else { it }
                        }
                        Log.d("WidgetManager", "Weather data updated for widget ${widget.id} (Auto): ${weatherInfo.cityName}")
                    } else {
                         Log.e("WidgetManager", "Failed to get weather by coords for widget ${widget.id}")
                    }
                } else {
                    Log.w("WidgetManager", "Auto mode for widget ${widget.id}, but location (lat/lon) is unavailable.")
                    _widgets.value = _widgets.value.map {
                        if (it.id == widgetId) {
                            it.copy(
                                temperature = null,
                                weatherDescription = "Location unavailable",
                                weatherIconUrl = null,
                                cityName = "N/A" // Или оставить widget.cityName
                            )
                        } else { it }
                    }
                }
            } else { // Ручной режим (autoLocate = false)
                val cityNameToFetch = widget.cityName // Должен быть равен manualCityName из WidgetData, установленным в onSaveSettings
                Log.d("WidgetManager", "Fetching weather for widget ${widget.id} in MANUAL mode for city: '$cityNameToFetch'")
                if (!cityNameToFetch.isNullOrBlank()) {
                    val weatherInfo = weatherRepository.getCurrentWeatherByCityName(cityNameToFetch)
                    if (weatherInfo != null) {
                        _widgets.value = _widgets.value.map {
                            if (it.id == widgetId) {
                                it.copy(
                                    temperature = weatherInfo.main.temperature,
                                    weatherDescription = weatherInfo.weather.firstOrNull()?.description,
                                    weatherIconUrl = weatherInfo.weather.firstOrNull()?.icon?.let { iconCode ->
                                        "https://openweathermap.org/img/wn/$iconCode@2x.png"
                                    },
                                    cityName = weatherInfo.cityName // Можно обновить cityName из API, если он возвращает каноническое имя
                                )
                            } else { it }
                        }
                         Log.d("WidgetManager", "Weather data updated for widget ${widget.id} (Manual): ${weatherInfo.cityName}")
                    } else {
                        Log.e("WidgetManager", "Failed to get weather by city '$cityNameToFetch' for widget ${widget.id}")
                        _widgets.value = _widgets.value.map {
                            if (it.id == widgetId) {
                                it.copy(
                                    temperature = null,
                                    weatherDescription = "City not found: $cityNameToFetch",
                                    weatherIconUrl = null,
                                    cityName = cityNameToFetch // Оставляем введенное пользователем имя
                                )
                            } else { it }
                        }
                    }
                } else {
                     Log.w("WidgetManager", "Manual mode for widget ${widget.id}, but city name is blank.")
                    _widgets.value = _widgets.value.map {
                        if (it.id == widgetId) {
                            it.copy(
                                temperature = null,
                                weatherDescription = "Please specify a city",
                                weatherIconUrl = null,
                                cityName = null
                            )
                        } else { it }
                    }
                }
            }
        }
    }

    fun updateWidget(updatedWidget: WidgetData) {
        val previousWidgetState = _widgets.value.find { it.id == updatedWidget.id }

        _widgets.value = _widgets.value.map {
            if (it.id == updatedWidget.id) updatedWidget else it
        }

        if (updatedWidget.type == WidgetType.WEATHER) {
            val needsRefresh = updatedWidget.temperature == null || // Данные были сброшены, нужна загрузка
                               (updatedWidget.autoLocate && previousWidgetState?.autoLocate == false) || // Переключились на авто
                               (!updatedWidget.autoLocate && previousWidgetState?.autoLocate == true && !updatedWidget.cityName.isNullOrBlank()) || // Переключились на ручной с указанным городом
                               (!updatedWidget.autoLocate && previousWidgetState?.cityName != updatedWidget.cityName && !updatedWidget.cityName.isNullOrBlank()) // Остались в ручном, но город изменился

            if (needsRefresh) {
                Log.d("WidgetManager", "Widget ${updatedWidget.id} updated, needs weather refresh. Auto: ${updatedWidget.autoLocate}, City: ${updatedWidget.cityName}, TempIsNull: ${updatedWidget.temperature == null}")
                fetchWeatherData(updatedWidget.id)
            }
        }
    }

    fun removeWidget(widgetId: String) {
        _widgets.value = _widgets.value.filterNot { it.id == widgetId }
    }

    fun checkCollisionWithExisting(
        widgetToCheck: WidgetData, newX: Float, newY: Float, newWidth: Float, newHeight: Float
    ): Boolean {
        return checkCollisionInternal(widgetToCheck, newX, newY, newWidth, newHeight, _widgets.value, true)
    }

    private fun checkCollisionInternal(
        widget: WidgetData, checkX: Float, checkY: Float, checkWidth: Float, checkHeight: Float,
        widgetsToCompareAgainst: List<WidgetData>, ignoreSelf: Boolean = false
    ): Boolean {
        val widgetRight = checkX + checkWidth
        val widgetBottom = checkY + checkHeight
        for (existingWidget in widgetsToCompareAgainst) {
            if (ignoreSelf && existingWidget.id == widget.id) continue
            if (checkX < existingWidget.x + existingWidget.width &&
                widgetRight > existingWidget.x &&
                checkY < existingWidget.y + existingWidget.height &&
                widgetBottom > existingWidget.y) {
                return true
            }
        }
        return false
    }
}
