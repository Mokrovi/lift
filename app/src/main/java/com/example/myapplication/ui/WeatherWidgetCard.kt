package com.example.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.WidgetData
import java.util.Locale // For formatting temperature

@Composable
fun WeatherWidgetCard(widget: WidgetData) {
    // Copy mutable properties to local immutable variables
    val currentCityName = widget.cityName
    val currentTemperature = widget.temperature
    val currentDescription = widget.weatherDescription
    val currentIconUrl = widget.weatherIconUrl

    Card(
        modifier = Modifier
            .width(widget.width.dp) // Use width from WidgetData
            .height(widget.height.dp) // Use height from WidgetData
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentCityName != null) {
                Text(
                    text = currentCityName, // Use local val
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (currentTemperature != null && currentDescription != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentIconUrl != null) {
                            AsyncImage(
                                model = currentIconUrl, // Use local val
                                contentDescription = currentDescription, // Use local val
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            // Format temperature to one decimal place and add °C
                            text = String.format(Locale.getDefault(), "%.1f°C", currentTemperature), // Use local val
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentDescription.replaceFirstChar { // Use local val
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }, // Capitalize description
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Show a loading indicator or placeholder if weather data is not yet available
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading weather...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("City not set", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
