package com.example.myapplication.data

import android.util.Log // Для простого логирования ошибок

class WeatherRepository {

    private val weatherApiService = RetrofitInstance.api

    suspend fun getCurrentWeatherByCityName(cityName: String): WeatherResponse? { // Renamed for clarity
        return try {
            // Assuming your ApiService has a method like getCurrentWeatherByCity
            val response = weatherApiService.getCurrentWeatherByCityName(cityName = cityName)
            if (response.isSuccessful) {
                response.body()
            } else {
                // Логируем ошибку, если ответ не успешный
                Log.e("WeatherRepository", "Error fetching weather by city: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            // Логируем исключение (например, проблемы с сетью)
            Log.e("WeatherRepository", "Exception fetching weather by city", e)
            null
        }
    }

    suspend fun getCurrentWeatherByCoordinates(latitude: Double, longitude: Double): WeatherResponse? {
        return try {
            val response = weatherApiService.getCurrentWeatherByCoordinates(latitude = latitude, longitude = longitude)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("WeatherRepository", "Error fetching weather by coords: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Exception fetching weather by coords", e)
            null
        }
    }
}
