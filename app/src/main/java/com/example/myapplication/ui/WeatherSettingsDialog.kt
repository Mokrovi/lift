package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun WeatherSettingsDialog(
    initialAutoLocate: Boolean,
    initialManualCity: String?,
    initialTextColorInt: Int?,
    initialBackgroundColorInt: Int?,
    onDismissRequest: () -> Unit,
    onSaveSettings: (autoLocate: Boolean, manualCity: String?, textColorInt: Int?, backgroundColorInt: Int?) -> Unit
) {
    var autoLocate by remember { mutableStateOf(initialAutoLocate) }
    var manualCity by remember { mutableStateOf(initialManualCity ?: "") }
    
    val defaultTextColor = MaterialTheme.colorScheme.onSurface
    val defaultBackgroundColor = Color.Transparent

    var selectedTextColor by remember { 
        mutableStateOf(initialTextColorInt?.let { Color(it) } ?: defaultTextColor)
    }
    var selectedBackgroundColor by remember { 
        mutableStateOf(initialBackgroundColorInt?.let { Color(it) } ?: defaultBackgroundColor)
    }

    val predefinedColors = remember {
        listOf(
            Color.Black, Color.White, Color.Gray, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan,
            defaultTextColor, defaultBackgroundColor, Color.Transparent
        ).distinct()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Настройки погоды", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                    // Removed horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Автоматическое определение",
                        modifier = Modifier.weight(1f) // Text takes available space and can wrap
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Added Spacer for visual separation
                    Switch(
                        checked = autoLocate,
                        onCheckedChange = { autoLocate = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualCity,
                    onValueChange = { manualCity = it },
                    label = { Text("Город") },
                    enabled = !autoLocate,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Цвет текста")
                ColorPickerRow(predefinedColors, selectedTextColor) { color ->
                    selectedTextColor = color
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Цвет фона")
                ColorPickerRow(predefinedColors, selectedBackgroundColor) { color ->
                    selectedBackgroundColor = color
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val textColorToSave = if (selectedTextColor == defaultTextColor && initialTextColorInt == null) null else selectedTextColor.toArgb()
                        val bgColorToSave = if (selectedBackgroundColor == defaultBackgroundColor && initialBackgroundColorInt == null) null else selectedBackgroundColor.toArgb()
                        
                        onSaveSettings(
                            autoLocate,
                            if (autoLocate) null else manualCity.ifBlank { null },
                            textColorToSave,
                            bgColorToSave
                        )
                        onDismissRequest()
                    }) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerRow(colors: List<Color>, selectedColor: Color, onColorSelected: (Color) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) {
            color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(color) }
                    .border(
                        width = if (color == selectedColor) 2.dp else 0.dp,
                        color = if (color == selectedColor) MaterialTheme.colorScheme.outline else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }
}
