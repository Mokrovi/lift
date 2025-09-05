package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.WidgetData
import java.util.Locale // For formatting temperature

@Composable
fun WeatherWidgetCard(
    widget: WidgetData,
    onWeatherSettingsClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface, // Default text color
    backgroundColor: Color = Color.Transparent // Default background color
) {
    val currentCityName = widget.cityName
    val currentTemperature = widget.temperature
    val currentDescription = widget.weatherDescription
    val currentIconUrl = widget.weatherIconUrl

    Column(
        modifier = Modifier
            .width(widget.width.dp)
            .height(widget.height.dp)
            .background(backgroundColor) // Apply background color
            .pointerInput(Unit) { // Add double tap gesture detection
                detectTapGestures(
                    onDoubleTap = { onWeatherSettingsClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (currentCityName != null) {
            Text(
                text = currentCityName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor, // Apply text color
                modifier = Modifier.padding(0.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (currentTemperature != null && currentDescription != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(0.dp)
                ) {
                    if (currentIconUrl != null) {
                        AsyncImage(
                            model = currentIconUrl,
                            contentDescription = currentDescription,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f°C", currentTemperature),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor, // Apply text color
                        modifier = Modifier.padding(0.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDescription.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor, // Apply text color
                    modifier = Modifier.padding(0.dp)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp).padding(0.dp),
                    color = textColor // Apply text color to indicator as well, or use a specific one
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Loading weather...",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor, // Apply text color
                    modifier = Modifier.padding(0.dp)
                )
            }
        } else {
            Text(
                "City not set",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor, // Apply text color
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}
