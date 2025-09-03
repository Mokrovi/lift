package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.WidgetData

@Composable
fun OnvifCameraDisplay(widgetData: WidgetData, modifier: Modifier = Modifier) {
    // TODO: Implement actual ONVIF camera stream display here
    // For now, it's a placeholder
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("ONVIF Camera (${widgetData.id})")
    }
}
