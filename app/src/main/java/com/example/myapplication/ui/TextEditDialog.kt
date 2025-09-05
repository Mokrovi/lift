package com.example.myapplication.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.HorizontalAlignmentMode
import com.example.myapplication.WidgetData
import kotlin.math.roundToInt
import java.text.DecimalFormat

// Helper function to get FontFamily from string
@Composable
fun getPlatformFontFamily(fontFamilyName: String?): FontFamily {
    return when (fontFamilyName) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
}

@Composable
fun TextEditDialog(
    showDialog: Boolean,
    widgetData: WidgetData,
    onDismissRequest: () -> Unit,
    onSave: (newTextData: String, newBackgroundColor: Int?, newTextColor: Int?, newTextSize: Int?, newIsVertical: Boolean, newHorizontalAlignment: HorizontalAlignmentMode, newFontFamily: String?, newLineHeightScale: Float?, newLetterSpacingSp: Float?, newFontWeight: Int?) -> Unit
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
        // Store fontWeight as Int? ( FontWeight.Bold.weight or null)
        var tempFontWeightSelection by remember(widgetData.id, widgetData.fontWeight) { mutableStateOf(widgetData.fontWeight) }

        val floatFormatter = remember { DecimalFormat("#.0") }

        val availableColorInts = listOf(
            Color.White.toArgb(), Color.LightGray.toArgb(), Color.Gray.toArgb(), Color.DarkGray.toArgb(), Color.Black.toArgb(),
            Color.Red.toArgb(), Color.Green.toArgb(), Color.Blue.toArgb(), Color.Yellow.toArgb(), Color.Cyan.toArgb(), Color.Magenta.toArgb(), Color.Transparent.toArgb()
        )
        val availableFontFamilies = listOf("Default", "Serif", "SansSerif", "Monospace", "Cursive")

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Edit Text Properties") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TextField(
                        value = tempTextData,
                        onValueChange = { tempTextData = it },
                        label = { Text("Widget Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.fillMaxWidth(),
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
                                        color = if (tempBackgroundColor == colorArgb) Color.Black else Color.Transparent,
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
                        modifier = Modifier.fillMaxWidth(),
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
                                        color = if (tempTextColor == colorArgb) Color.Black else Color.Transparent,
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
                        steps = 24 // (3.0 - 0.5) / 0.1 = 25 steps - 1
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Letter Spacing: ${floatFormatter.format(tempLetterSpacingSp)}sp", fontSize = 16.sp)
                    Slider(
                        value = tempLetterSpacingSp,
                        onValueChange = { tempLetterSpacingSp = it },
                        valueRange = -2.0f..10.0f,
                        steps = 119 // (10 - (-2)) / 0.1 = 120 steps - 1
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(
                        tempTextData,
                        if (tempBackgroundColor == Color.Transparent.toArgb() && widgetData.backgroundColor == null) null else tempBackgroundColor,
                        tempTextColor,
                        tempTextSize.roundToInt(),
                        tempIsVertical,
                        tempHorizontalAlignment,
                        if (tempFontFamily == "Default") null else tempFontFamily,
                        if (tempLineHeightScale == 1.0f) null else tempLineHeightScale,
                        if (tempLetterSpacingSp == 0.0f) null else tempLetterSpacingSp,
                        tempFontWeightSelection // Pass the Int? directly
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

@Composable
fun VerticalStretchedText(
    modifier: Modifier = Modifier,
    text: String,
    textSize: Int,
    color: Color = Color.Black,
    fontFamilyName: String? = null,
    fontWeightInt: Int? = null, // fontWeightInt is Int?
    fontStyle: FontStyle = FontStyle.Normal,
    lineHeightScale: Float? = null
) {
    val currentFontFamily = getPlatformFontFamily(fontFamilyName)
    val currentFontWeight = fontWeightInt?.let { FontWeight(it) } // Converts Int? to FontWeight? correctly
    val effectiveLineHeight = textSize.sp * (lineHeightScale ?: 1.0f)

    Column(modifier = modifier) {
        if (text.isNotEmpty()) {
            text.forEachIndexed { index, char ->
                Text(
                    text = char.toString(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    style = TextStyle(
                        color = color,
                        fontSize = textSize.sp,
                        fontFamily = currentFontFamily,
                        fontWeight = currentFontWeight, // Pass FontWeight? directly
                        fontStyle = fontStyle,
                        textAlign = TextAlign.Center
                    ),
                    lineHeight = effectiveLineHeight,
                    overflow = TextOverflow.Visible,
                    softWrap = false
                )
                if (index < text.length - 1 && (lineHeightScale ?: 1.0f) > 1.0f) {
                    val rawSpacerValue = (((lineHeightScale ?: 1.0f) - 1.0f) * textSize * 0.5f)
                    val spacerHeightInSp = rawSpacerValue.sp
                    val spacerHeightInDp = with(LocalDensity.current) { spacerHeightInSp.toDp() } 
                    Spacer(modifier = Modifier.height(spacerHeightInDp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditableTextWidget(
    modifier: Modifier = Modifier,
    widgetData: WidgetData, 
    onWidgetDataChange: (WidgetData) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val contentAlignment = when (widgetData.horizontalAlignment) { 
        HorizontalAlignmentMode.LEFT -> Alignment.CenterStart
        HorizontalAlignmentMode.CENTER -> Alignment.Center
        HorizontalAlignmentMode.RIGHT -> Alignment.CenterEnd
    }

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = { /* Одиночное нажатие, если нужно */ },
                onDoubleClick = {
                    showEditDialog = true
                }
            )
            .background(widgetData.backgroundColor?.let { Color(it) } ?: Color.Transparent) 
            .padding(8.dp)
            .fillMaxSize(),
        contentAlignment = if (widgetData.isVertical) Alignment.Center else contentAlignment 
    ) {
        if (widgetData.isVertical) { 
            VerticalStretchedText(
                modifier = Modifier.fillMaxSize(),
                text = widgetData.textData ?: "", 
                textSize = widgetData.textSize ?: 16, 
                color = widgetData.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface, 
                fontFamilyName = widgetData.fontFamily, 
                lineHeightScale = widgetData.lineHeightScale,
                fontWeightInt = widgetData.fontWeight // Pass Int? directly
            )
        } else {
            Text(
                text = widgetData.textData ?: "",
                style = TextStyle(
                    color = widgetData.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface,
                    fontSize = (widgetData.textSize ?: 16).sp,
                    fontFamily = getPlatformFontFamily(widgetData.fontFamily),
                    fontWeight = widgetData.fontWeight?.let { FontWeight(it) }, // Converts Int? to FontWeight?, or null if widgetData.fontWeight is null
                    textAlign = when (widgetData.horizontalAlignment) {
                        HorizontalAlignmentMode.LEFT -> TextAlign.Start
                        HorizontalAlignmentMode.CENTER -> TextAlign.Center
                        HorizontalAlignmentMode.RIGHT -> TextAlign.End
                    },
                    lineHeight = (widgetData.textSize ?: 16).sp * (widgetData.lineHeightScale ?: 1.0f),
                    letterSpacing = (widgetData.letterSpacingSp ?: 0.0f).sp
                )
            )
        }
    }

    if (showEditDialog) {
        TextEditDialog(
            showDialog = true,
            widgetData = widgetData, 
            onDismissRequest = { showEditDialog = false },
            onSave = { newTextData, newBackgroundColor, newTextColor, newTextSize, newIsVertical, newHorizontalAlignment, newFontFamily, newLineHeightScale, newLetterSpacingSp, newFontWeight ->
                val updatedWidgetData = widgetData.copy( 
                    textData = newTextData,
                    backgroundColor = newBackgroundColor,
                    textColor = newTextColor,
                    textSize = newTextSize,
                    isVertical = newIsVertical,
                    horizontalAlignment = newHorizontalAlignment,
                    fontFamily = newFontFamily,
                    lineHeightScale = newLineHeightScale,
                    letterSpacingSp = newLetterSpacingSp,
                    fontWeight = newFontWeight // newFontWeight is Int? from onSave
                )
                onWidgetDataChange(updatedWidgetData)
                showEditDialog = false
            }
        )
    }
}
