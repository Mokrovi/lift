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
    onWeatherSettingsClick: () -> Unit, // This will now be triggered by onLongPress logic from WidgetDisplayItem
    textColor: Color = MaterialTheme.colorScheme.onSurface, 
    backgroundColor: Color = Color.Transparent, 
    isEditMode: Boolean,      // Added isEditMode
    onLongPress: () -> Unit   // Added onLongPress callback
) {
    val currentCityName = widget.cityName
    val currentTemperature = widget.temperature
    val currentDescription = widget.weatherDescription
    val currentIconUrl = widget.weatherIconUrl

    Column(
        modifier = Modifier
            // .width(widget.width.dp) // Width and height are controlled by parent Box in WidgetDisplayItem
            // .height(widget.height.dp)
            // .fillMaxSize() // строка .fillMaxSize() УДАЛЕНА
            .background(backgroundColor) 
            .pointerInput(isEditMode, onLongPress, onWeatherSettingsClick) { // Pass all relevant keys to pointerInput
                detectTapGestures(
                    // onDoubleTap = { 
                    //     if (isEditMode) {
                    //         onWeatherSettingsClick() // Keep double tap if you want, or remove
                    //     }
                    // },
                    onLongPress = { offset -> // offset is a parameter of onLongPress lambda
                        if (isEditMode) {
                            onLongPress() // This now calls showWeatherSettingsDialog = true from WidgetDisplayItem
                        }
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (currentCityName != null) {
            Text(
                text = currentCityName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor, 
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
                        color = textColor, 
                        modifier = Modifier.padding(0.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDescription.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor, 
                    modifier = Modifier.padding(0.dp)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp).padding(0.dp),
                    color = textColor 
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Loading weather...",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor, 
                    modifier = Modifier.padding(0.dp)
                )
            }
        } else {
            Text(
                "City not set",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor, 
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}
