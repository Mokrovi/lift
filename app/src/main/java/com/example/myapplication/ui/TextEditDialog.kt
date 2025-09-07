package com.example.myapplication.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable // Keep for EditableTextWidget if other gestures are needed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width // Added for Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions // Added
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField // Added
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily // Added import
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType // Added
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.HorizontalAlignmentMode
import com.example.myapplication.WidgetData
import kotlin.math.roundToInt
import java.text.DecimalFormat
import com.example.myapplication.parseHexColor // Added import
import com.example.myapplication.toHexString // Added import
import androidx.compose.foundation.BorderStroke // Added import

@Composable
fun TextEditDialog(
    showDialog: Boolean,
    widgetData: WidgetData,
    initialWidth: Int,
    initialHeight: Int,
    onDismissRequest: () -> Unit,
    onSave: (
        newTextData: String,
        newBackgroundColor: Int?,
        newTextColor: Int?,
        newTextSize: Int?,
        newIsVertical: Boolean,
        newHorizontalAlignment: HorizontalAlignmentMode,
        newFontFamily: String?,
        newLineHeightScale: Float?,
        newLetterSpacingSp: Float?,
        newFontWeight: Int?,
        newWidth: Int,
        newHeight: Int
    ) -> Unit,
    isTextContentEditable: Boolean = true
) {
    if (showDialog) {
        var tempTextData by remember(widgetData.id, widgetData.textData) { mutableStateOf(widgetData.textData ?: "") }

        // Background Color States
        var tempBackgroundColor by remember(widgetData.id, widgetData.backgroundColor) { mutableStateOf(widgetData.backgroundColor ?: Color.Transparent.toArgb()) }
        var tempBackgroundColorHex by remember(widgetData.id, widgetData.backgroundColor) { mutableStateOf( (widgetData.backgroundColor?.let { Color(it) } ?: Color.Transparent).toHexString() ) }
        var previewBackgroundColor by remember { mutableStateOf(widgetData.backgroundColor?.let { Color(it) } ?: Color.Transparent) }

        // Text Color States
        var tempTextColor by remember(widgetData.id, widgetData.textColor) { mutableStateOf(widgetData.textColor ?: Color.Black.toArgb()) }
        var tempTextColorHex by remember(widgetData.id, widgetData.textColor) { mutableStateOf( (widgetData.textColor?.let { Color(it) } ?: Color.Black).toHexString() ) }
        var previewTextColor by remember { mutableStateOf(widgetData.textColor?.let { Color(it) } ?: Color.Black) }

        // Other states
        var tempTextSize by remember(widgetData.id, widgetData.textSize) { mutableStateOf((widgetData.textSize ?: 16).toFloat()) }
        var tempIsVertical by remember(widgetData.id, widgetData.isVertical) { mutableStateOf(widgetData.isVertical) }
        var tempHorizontalAlignment by remember(widgetData.id, widgetData.horizontalAlignment) { mutableStateOf(widgetData.horizontalAlignment) }
        var tempFontFamily by remember(widgetData.id, widgetData.fontFamily) { mutableStateOf(widgetData.fontFamily ?: "Default") }
        var tempLineHeightScale by remember(widgetData.id, widgetData.lineHeightScale) { mutableStateOf(widgetData.lineHeightScale ?: 1.0f) }
        var tempLetterSpacingSp by remember(widgetData.id, widgetData.letterSpacingSp) { mutableStateOf(widgetData.letterSpacingSp ?: 0.0f) }
        var tempFontWeightSelection by remember(widgetData.id, widgetData.fontWeight) { mutableStateOf(widgetData.fontWeight) }
        var widthInput by remember(widgetData.id, initialWidth) { mutableStateOf(initialWidth.toString()) }
        var heightInput by remember(widgetData.id, initialHeight) { mutableStateOf(initialHeight.toString()) }

        val floatFormatter = remember { DecimalFormat("#.0") }
        val availableColorInts = remember {
            listOf(
                Color.White.toArgb(), Color.LightGray.toArgb(), Color.Gray.toArgb(), Color.DarkGray.toArgb(), Color.Black.toArgb(),
                Color.Red.toArgb(), Color(0xFF00FF00).toArgb(), Color.Blue.toArgb(), Color.Yellow.toArgb(), Color.Cyan.toArgb(), Color.Magenta.toArgb(), Color.Transparent.toArgb()
            ).distinct()
        }
        val availableFontFamilies = remember { listOf("Default", "Serif", "SansSerif", "Monospace") }

        // Sync HEX input with preview and potentially the ARGB int state (tempBackgroundColor)
        LaunchedEffect(tempBackgroundColorHex) {
            val parsedColor = parseHexColor(tempBackgroundColorHex)
            if (parsedColor != null) {
                previewBackgroundColor = parsedColor
                // Optionally update tempBackgroundColor (Int) if HEX is valid and differs
                // if (tempBackgroundColor != parsedColor.toArgb()) {
                //     tempBackgroundColor = parsedColor.toArgb()
                // }
            } else {
                // If HEX becomes invalid, preview falls back to the color from ARGB state
                previewBackgroundColor = Color(tempBackgroundColor)
            }
        }
        // Sync palette choice with HEX input and preview
        LaunchedEffect(tempBackgroundColor) {
            val colorFromInt = Color(tempBackgroundColor)
            tempBackgroundColorHex = colorFromInt.toHexString()
            previewBackgroundColor = colorFromInt
        }
         // Initial preview setup for background
        LaunchedEffect(Unit) {
            previewBackgroundColor = parseHexColor(tempBackgroundColorHex) ?: Color(tempBackgroundColor)
        }

        LaunchedEffect(tempTextColorHex) {
            val parsedColor = parseHexColor(tempTextColorHex)
            if (parsedColor != null) {
                previewTextColor = parsedColor
                // Optionally update tempTextColor (Int)
                // if (tempTextColor != parsedColor.toArgb()) {
                //     tempTextColor = parsedColor.toArgb()
                // }
            } else {
                previewTextColor = Color(tempTextColor)
            }
        }
        LaunchedEffect(tempTextColor) {
            val colorFromInt = Color(tempTextColor)
            tempTextColorHex = colorFromInt.toHexString()
            previewTextColor = colorFromInt
        }
        // Initial preview setup for text
        LaunchedEffect(Unit) {
            previewTextColor = parseHexColor(tempTextColorHex) ?: Color(tempTextColor)
        }


        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(if (isTextContentEditable) "Edit Text Properties" else "Edit Clock Style") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                    if (isTextContentEditable) {
                        TextField(
                            value = tempTextData,
                            onValueChange = { tempTextData = it },
                            label = { Text("Widget Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Font Family:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(/*...*/) { /* Font family selection remains same */
                        items(availableFontFamilies) { fontFamilyName ->
                            OutlinedButton(
                                onClick = { tempFontFamily = fontFamilyName },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (tempFontFamily == fontFamilyName) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text(fontFamilyName, style = TextStyle(fontFamily = getPlatformFontFamily(fontFamilyName)))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(/*...*/) { /* Bold text switch remains same */
                        Text("Bold Text:", fontSize = 16.sp)
                        Switch(
                            checked = tempFontWeightSelection == FontWeight.Bold.weight,
                            onCheckedChange = { isChecked ->
                                tempFontWeightSelection = if (isChecked) FontWeight.Bold.weight else null
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Background Color:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(availableColorInts) { colorArgb ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .border(
                                        width = 2.dp,
                                        color = if (tempBackgroundColor == colorArgb) MaterialTheme.colorScheme.outline else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { tempBackgroundColor = colorArgb /* LaunchedEffect will sync HEX */ }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tempBackgroundColorHex,
                        onValueChange = { tempBackgroundColorHex = it },
                        label = { Text("Background HEX (#RRGGBB)") },
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
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Text Color:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(availableColorInts) { colorArgb ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb))
                                    .border(
                                        width = 2.dp,
                                        color = if (tempTextColor == colorArgb) MaterialTheme.colorScheme.outline else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { tempTextColor = colorArgb /* LaunchedEffect will sync HEX */ }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = tempTextColorHex,
                        onValueChange = { tempTextColorHex = it },
                        label = { Text("Text Color HEX (#RRGGBB)") },
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

                    // ... (rest of the UI elements like Vertical Text, Alignment, Size, etc. remain the same)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vertical Text:", fontSize = 16.sp)
                        Switch(
                            checked = tempIsVertical,
                            onCheckedChange = { tempIsVertical = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Horizontal Alignment:", fontSize = 16.sp)
                     Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HorizontalAlignmentMode.entries.forEach { mode ->
                            OutlinedButton(
                                onClick = { tempHorizontalAlignment = mode },
                                enabled = !tempIsVertical,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (tempHorizontalAlignment == mode && !tempIsVertical) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text(mode.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Text Size: ${tempTextSize.roundToInt()}sp", fontSize = 16.sp)
                    Slider(
                        value = tempTextSize,
                        onValueChange = { tempTextSize = it },
                        valueRange = 8f..100f,
                        steps = (100 - 8) - 1
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Line Height Scale: ${floatFormatter.format(tempLineHeightScale)}x", fontSize = 16.sp)
                    Slider(
                        value = tempLineHeightScale,
                        onValueChange = { tempLineHeightScale = it },
                        valueRange = 0.5f..3.0f,
                        steps = ((3.0f - 0.5f) / 0.1f).toInt() -1
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Letter Spacing: ${floatFormatter.format(tempLetterSpacingSp)}sp", fontSize = 16.sp)
                    Slider(
                        value = tempLetterSpacingSp,
                        onValueChange = { tempLetterSpacingSp = it },
                        valueRange = -2.0f..10.0f,
                        steps = ((10.0f - (-2.0f)) / 0.1f).toInt() -1
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Widget Dimensions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = widthInput,
                            onValueChange = { widthInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Width (dp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it.filter { char -> char.isDigit() } },
                            label = { Text("Height (dp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val textToSave = if (isTextContentEditable) tempTextData else widgetData.textData ?: ""
                    val finalWidth = widthInput.toIntOrNull() ?: initialWidth
                    val finalHeight = heightInput.toIntOrNull() ?: initialHeight

                    val finalBackgroundColor = parseHexColor(tempBackgroundColorHex)?.toArgb()
                        ?: if (tempBackgroundColor == Color.Transparent.toArgb() && widgetData.backgroundColor == null) null else tempBackgroundColor
                    val finalTextColor = parseHexColor(tempTextColorHex)?.toArgb() ?: tempTextColor

                    onSave(
                        textToSave,
                        finalBackgroundColor,
                        finalTextColor,
                        tempTextSize.roundToInt(),
                        tempIsVertical,
                        tempHorizontalAlignment,
                        if (tempFontFamily == "Default") null else tempFontFamily,
                        if (tempLineHeightScale == 1.0f && widgetData.lineHeightScale == null) null else tempLineHeightScale,
                        if (tempLetterSpacingSp == 0.0f && widgetData.letterSpacingSp == null) null else tempLetterSpacingSp,
                        tempFontWeightSelection,
                        finalWidth,
                        finalHeight
                    )
                    onDismissRequest()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        )
    }
}

// EditableTextWidget and getPlatformFontFamily remain the same
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditableTextWidget(
    modifier: Modifier = Modifier,
    widgetData: WidgetData,
    onWidgetDataChange: (WidgetData) -> Unit
) {
    Box(
        modifier = modifier
            .background(widgetData.backgroundColor?.let { Color(it) } ?: Color.Transparent)
            .padding(8.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ReusableTextDisplayView(
            text = widgetData.textData ?: "",
            styleData = widgetData,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun getPlatformFontFamily(fontFamilyName: String): FontFamily? {
    return when (fontFamilyName.lowercase()) {
        "serif" -> FontFamily.Serif
        "sansserif", "sans-serif" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        "default" -> FontFamily.Default
        else -> null
    }
}
