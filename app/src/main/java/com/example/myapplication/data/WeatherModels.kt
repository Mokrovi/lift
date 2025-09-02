package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("coord") val coordinates: Coordinates,
    @SerializedName("weather") val weather: List<WeatherCondition>,
    @SerializedName("main") val main: MainDetails,
    @SerializedName("wind") val wind: WindDetails,
    @SerializedName("sys") val system: SystemDetails,
    @SerializedName("name") val cityName: String,
    @SerializedName("cod") val statusCode: Int // HTTP status code from OpenWeather
)

data class Coordinates(
    @SerializedName("lon") val longitude: Double,
    @SerializedName("lat") val latitude: Double
)

data class WeatherCondition(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val mainCondition: String, // e.g., "Clouds", "Rain"
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String // Icon ID
)

data class MainDetails(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Int,
    @SerializedName("humidity") val humidity: Int
)

data class WindDetails(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val degree: Int
)

data class SystemDetails(
    @SerializedName("country") val countryCode: String,
    @SerializedName("sunrise") val sunriseTimestamp: Long,
    @SerializedName("sunset") val sunsetTimestamp: Long
)
