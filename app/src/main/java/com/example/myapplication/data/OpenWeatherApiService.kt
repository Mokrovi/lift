package com.example.myapplication.data

import com.example.myapplication.BuildConfig // Import BuildConfig
import retrofit2.Response // Import Retrofit's Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeatherByCityName( // Renamed from getCurrentWeather
        @Query("q") cityName: String,
        @Query("appid") apiKey: String = BuildConfig.OPENWEATHER_API_KEY, // Use from BuildConfig
        @Query("units") units: String = "metric" // Or "imperial" for Fahrenheit
    ): Response<WeatherResponse> // Use Retrofit's Response for more control

    @GET("weather")
    suspend fun getCurrentWeatherByCoordinates( // Added this method
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = BuildConfig.OPENWEATHER_API_KEY,
        @Query("units") units: String = "metric"
    ): Response<WeatherResponse>
}
