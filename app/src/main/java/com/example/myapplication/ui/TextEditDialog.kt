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
        var tempBackgroundColor by remember(widgetData.id, widgetData.backgroundColor) { mutableStateOf(widgetData.backgroundColor ?: Color.Transparent.toArgb()) }
        var tempTextColor by remember(widgetData.id, widgetData.textColor) { mutableStateOf(widgetData.textColor ?: Color.Black.toArgb()) }
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
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                                    .clickable { tempBackgroundColor = colorArgb }
                            )
                        }
                    }
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
                                    .clickable { tempTextColor = colorArgb }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

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
                    
                    onSave(
                        textToSave,
                        if (tempBackgroundColor == Color.Transparent.toArgb() && widgetData.backgroundColor == null) null else tempBackgroundColor,
                        tempTextColor,
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
