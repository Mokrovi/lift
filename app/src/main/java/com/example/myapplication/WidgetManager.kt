package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.myapplication.data.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ProcessedWidgetResult(
    val widgetData: WidgetData,
    val statusCode: Int
)

class WidgetManager(initialWidgets: List<WidgetData> = emptyList()) {
    private val _widgets = MutableStateFlow(initialWidgets)
    val widgets: StateFlow<List<WidgetData>> = _widgets.asStateFlow()

    private val weatherRepository = WeatherRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private fun checkMediaResourceExists(context: Context, mediaUri: Uri?): Boolean {
        if (mediaUri == null) return false
        if ("file" == mediaUri.scheme) {
            val filePath = mediaUri.path
            if (filePath != null) {
                return File(filePath).exists()
            }
            return false
        }
        return true
    }

    fun processSavedWidget(context: Context, widgetData: WidgetData): ProcessedWidgetResult {
        return when (widgetData.type) {
            WidgetType.AD, WidgetType.GIF, WidgetType.VIDEO -> {
                if (widgetData.mediaUri != null && checkMediaResourceExists(context, widgetData.mediaUri)) {
                    ProcessedWidgetResult(widgetData, 200)
                } else {
                    Log.w("WidgetManager", "Media resource not found for widget ${widgetData.id} (URI: ${widgetData.mediaUri}). Type: ${widgetData.type}")
                    ProcessedWidgetResult(widgetData.copy(mediaUri = null), 404)
                }
            }
            else -> ProcessedWidgetResult(widgetData, 200)
        }
    }

    fun actuallyAddProcessedWidget(processedWidgetData: WidgetData) {
        if (_widgets.value.any { it.id == processedWidgetData.id }) {
            Log.w("WidgetManager", "Widget with ID ${processedWidgetData.id} already exists. Skipping add.")
            return
        }
        _widgets.value = _widgets.value + processedWidgetData
        if (processedWidgetData.type == WidgetType.WEATHER) {
            fetchWeatherData(processedWidgetData.id)
        }
        Log.d("WidgetManager", "Successfully added widget ${processedWidgetData.id} to the canvas.")
    }

    fun updateCurrentLocation(latitude: Double?, longitude: Double?) {
        currentLatitude = latitude
        currentLongitude = longitude
        _widgets.value.forEach { widget ->
            if (widget.type == WidgetType.WEATHER && widget.autoLocate) {
                fetchWeatherData(widget.id)
            }
        }
    }

    fun addNewWidgetAtAvailableSlot(
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
        val maxAttempts = 100 // Original maxAttempts

        while (collision && attempts < maxAttempts) {
            collision = false
            val potentialWidgetBounds = WidgetData(
                id = "temp_id_placement_${UUID.randomUUID()}", type = type,
                x = newX.toInt(), y = newY.toInt(),
                width = widgetWidth.toInt(), height = widgetHeight.toInt(),
                mediaUri = mediaUri?.let { Uri.parse(it) }, // Added for completeness, though not strictly needed for internal collision check
                textData = if (type == WidgetType.TEXT) textData else null // Added for completeness
            )

            if (checkCollisionInternal(potentialWidgetBounds, newX, newY, widgetWidth, widgetHeight, currentWidgets, false)) {
                collision = true
                newX += widgetWidth + 16f
                if (newX + widgetWidth > 1000f) { // Reverted X boundary check
                    newX = 16f
                    newY += widgetHeight + 16f
                }
            }
            attempts++
            if (newY + widgetHeight > 2000f) { // Reverted Y boundary check for canvas full
                 Log.e("WidgetManager", "Could not place new widget: Canvas full (Y boundary exceeded).")
                return false
            }
        }

        if (attempts >= maxAttempts && collision) { // Check collision flag as well if max attempts reached
            Log.e("WidgetManager", "Could not place new widget: Max placement attempts reached and still colliding.")
            return false
        }
        if (collision) { // Should ideally be caught by the Y boundary or max attempts, but as a safeguard
             Log.e("WidgetManager", "Could not place new widget: Collision detected after loop completion (should not happen).")
            return false
        }


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
        Log.d("WidgetManager", "Added new widget ${newWidget.id} of type $type at ($newX, $newY)")
        return true
    }

    fun fetchWeatherData(widgetId: String) {
        val widget = _widgets.value.find { it.id == widgetId }
        if (widget == null || widget.type != WidgetType.WEATHER) {
            return
        }

        coroutineScope.launch {
            if (widget.autoLocate) {
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
                            } else { it }
                        }
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
                                cityName = widget.cityName ?: "N/A"
                            )
                        } else { it }
                    }
                }
            } else { 
                val cityNameToFetch = widget.cityName
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
                                    cityName = weatherInfo.cityName
                                )
                            } else { it }
                        }
                    } else {
                        Log.e("WidgetManager", "Failed to get weather by city '$cityNameToFetch' for widget ${widget.id}")
                        _widgets.value = _widgets.value.map {
                            if (it.id == widgetId) {
                                it.copy(
                                    temperature = null,
                                    weatherDescription = "City not found: $cityNameToFetch",
                                    weatherIconUrl = null,
                                    cityName = cityNameToFetch
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
            val needsRefresh = updatedWidget.temperature == null ||
                               (updatedWidget.autoLocate && previousWidgetState?.autoLocate == false) ||
                               (!updatedWidget.autoLocate && previousWidgetState?.autoLocate == true && !updatedWidget.cityName.isNullOrBlank()) ||
                               (!updatedWidget.autoLocate && previousWidgetState?.cityName != updatedWidget.cityName && !updatedWidget.cityName.isNullOrBlank())

            if (needsRefresh) {
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

            val existingWidgetRight = existingWidget.x + existingWidget.width
            val existingWidgetBottom = existingWidget.y + existingWidget.height

            if (checkX < existingWidgetRight &&
                widgetRight > existingWidget.x &&
                checkY < existingWidgetBottom &&
                widgetBottom > existingWidget.y) {
                return true
            }
        }
        return false
    }
}
