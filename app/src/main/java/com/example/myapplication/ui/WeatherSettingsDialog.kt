package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions // Needed for KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType // Needed for KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.parseHexColor
import com.example.myapplication.toHexString

@Composable
fun WeatherSettingsDialog(
    initialAutoLocate: Boolean,
    initialManualCity: String?,
    initialTextColorInt: Int?,
    initialBackgroundColorInt: Int?,
    initialWidth: Int,
    initialHeight: Int,
    onDismissRequest: () -> Unit,
    onSaveSettings: (
        autoLocate: Boolean,
        manualCity: String?,
        textColorInt: Int?,
        backgroundColorInt: Int?,
        newWidth: Int,
        newHeight: Int
    ) -> Unit
) {
    var autoLocate by remember { mutableStateOf(initialAutoLocate) }
    var manualCity by remember { mutableStateOf(initialManualCity ?: "") }
    var widthInput by remember { mutableStateOf(initialWidth.toString()) }
    var heightInput by remember { mutableStateOf(initialHeight.toString()) }

    val defaultTextColor = MaterialTheme.colorScheme.onSurface
    val defaultBackgroundColor = Color.Transparent

    // Text Color States
    var selectedTextColor by remember { mutableStateOf(initialTextColorInt?.let { Color(it) } ?: defaultTextColor) }
    var textColorHex by remember { mutableStateOf((initialTextColorInt?.let { Color(it) } ?: defaultTextColor).toHexString()) }
    var previewTextColor by remember { mutableStateOf(initialTextColorInt?.let { Color(it) } ?: defaultTextColor) }

    // Background Color States
    var selectedBackgroundColor by remember { mutableStateOf(initialBackgroundColorInt?.let { Color(it) } ?: defaultBackgroundColor) }
    var backgroundColorHex by remember { mutableStateOf((initialBackgroundColorInt?.let { Color(it) } ?: defaultBackgroundColor).toHexString()) }
    var previewBackgroundColor by remember { mutableStateOf(initialBackgroundColorInt?.let { Color(it) } ?: defaultBackgroundColor) }

    val predefinedColors = remember {
        listOf(
            Color.Black, Color.White, Color.Gray, Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan,
            defaultTextColor, defaultBackgroundColor, Color.Transparent
        ).distinct()
    }

    // Sync HEX input with preview and Color state for Text Color
    LaunchedEffect(textColorHex) {
        val parsed = parseHexColor(textColorHex)
        if (parsed != null) {
            previewTextColor = parsed
            // selectedTextColor = parsed // Uncomment if direct HEX input should also update the palette selection immediately
        } else {
            previewTextColor = selectedTextColor // Fallback to current palette selection if HEX is invalid
        }
    }
    LaunchedEffect(selectedTextColor) {
        textColorHex = selectedTextColor.toHexString()
        previewTextColor = selectedTextColor
    }
     // Initial preview setup for text color
    LaunchedEffect(Unit) {
        previewTextColor = parseHexColor(textColorHex) ?: selectedTextColor
    }

    // Sync HEX input with preview and Color state for Background Color
    LaunchedEffect(backgroundColorHex) {
        val parsed = parseHexColor(backgroundColorHex)
        if (parsed != null) {
            previewBackgroundColor = parsed
            // selectedBackgroundColor = parsed // Uncomment if direct HEX input should also update the palette selection immediately
        } else {
            previewBackgroundColor = selectedBackgroundColor // Fallback to current palette selection if HEX is invalid
        }
    }
    LaunchedEffect(selectedBackgroundColor) {
        backgroundColorHex = selectedBackgroundColor.toHexString()
        previewBackgroundColor = selectedBackgroundColor
    }
    // Initial preview setup for background color
    LaunchedEffect(Unit) {
        previewBackgroundColor = parseHexColor(backgroundColorHex) ?: selectedBackgroundColor
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // Added for scrollability
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Настройки погоды", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // City and Autolocate (as before)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Автоматическое определение",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

                // Widget Dimensions (as before)
                Text("Размеры виджета", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Ширина (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Высота (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Цвет текста")
                ColorPickerRow(predefinedColors, selectedTextColor) { color ->
                    selectedTextColor = color // This will trigger LaunchedEffect to update HEX and preview
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = textColorHex,
                    onValueChange = { textColorHex = it },
                    label = { Text("HEX цвета текста (#RRGGBB)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(previewTextColor, CircleShape)
                                .border(BorderStroke(1.dp, Color.Gray), CircleShape)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Цвет фона")
                ColorPickerRow(predefinedColors, selectedBackgroundColor) { color ->
                    selectedBackgroundColor = color // This will trigger LaunchedEffect to update HEX and preview
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = backgroundColorHex,
                    onValueChange = { backgroundColorHex = it },
                    label = { Text("HEX цвета фона (#RRGGBB)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(previewBackgroundColor, CircleShape)
                                .border(BorderStroke(1.dp, Color.Gray), CircleShape)
                        )
                    }
                )
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
                        val finalTextColorInt = parseHexColor(textColorHex)?.toArgb() 
                            ?: if (selectedTextColor == defaultTextColor && initialTextColorInt == null) null else selectedTextColor.toArgb()
                        
                        val finalBackgroundColorInt = parseHexColor(backgroundColorHex)?.toArgb() 
                            ?: if (selectedBackgroundColor == defaultBackgroundColor && initialBackgroundColorInt == null) null else selectedBackgroundColor.toArgb()

                        val newWidth = widthInput.toIntOrNull() ?: initialWidth
                        val newHeight = heightInput.toIntOrNull() ?: initialHeight
                        
                        onSaveSettings(
                            autoLocate,
                            if (autoLocate) null else manualCity.ifBlank { null },
                            finalTextColorInt,
                            finalBackgroundColorInt,
                            newWidth,
                            newHeight
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

// ColorPickerRow remains the same
@Composable
fun ColorPickerRow(colors: List<Color>, selectedColor: Color, onColorSelected: (Color) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color -> 
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
