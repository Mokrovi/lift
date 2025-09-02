package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // items должен быть импортирован
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme // Added import
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch // Added import
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.WidgetData
import com.example.myapplication.HorizontalAlignmentMode // Added import
import kotlin.math.roundToInt

@Composable
fun TextEditDialog(
    showDialog: Boolean,
    widgetData: WidgetData,
    onDismissRequest: () -> Unit,
    onSave: (newBackgroundColor: Int?, newTextColor: Int?, newTextSize: Int?, newIsVertical: Boolean, newHorizontalAlignment: HorizontalAlignmentMode) -> Unit
) {
    if (showDialog) {
        var tempBackgroundColor by remember(widgetData.id, widgetData.backgroundColor) { mutableStateOf(widgetData.backgroundColor ?: Color.Transparent.toArgb()) }
        var tempTextColor by remember(widgetData.id, widgetData.textColor) { mutableStateOf(widgetData.textColor ?: Color.Black.toArgb()) }
        var tempTextSize by remember(widgetData.id, widgetData.textSize) { mutableStateOf((widgetData.textSize ?: 16).toFloat()) }
        var tempIsVertical by remember(widgetData.id, widgetData.isVertical) { mutableStateOf(widgetData.isVertical) }
        var tempHorizontalAlignment by remember(widgetData.id, widgetData.horizontalAlignment) { mutableStateOf(widgetData.horizontalAlignment) }

        // Store ARGB Ints, not Color objects, to avoid saveable issues
        val availableColorInts = listOf(
            Color.White.toArgb(), Color.LightGray.toArgb(), Color.Gray.toArgb(), Color.DarkGray.toArgb(), Color.Black.toArgb(),
            Color.Red.toArgb(), Color.Green.toArgb(), Color.Blue.toArgb(), Color.Yellow.toArgb(), Color.Cyan.toArgb(), Color.Magenta.toArgb(), Color.Transparent.toArgb()
        )

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Edit Text Properties") },
            text = {
                Column {
                    Text("Background Color:", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(availableColorInts) { colorArgb -> // Iterate over Ints
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb)) // Create Color on the fly
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
                        items(availableColorInts) { colorArgb -> // Iterate over Ints
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorArgb)) // Create Color on the fly
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
                                Text(mode.name.toLowerCase().capitalize())
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Text Size: ${tempTextSize.roundToInt()}sp", fontSize = 16.sp)
                    Slider(
                        value = tempTextSize,
                        onValueChange = { tempTextSize = it },
                        valueRange = 8f..100f,
                        steps = (100-8) -1 // Make sure this calculation is correct for the desired steps
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(
                        if (tempBackgroundColor == Color.Transparent.toArgb() && widgetData.backgroundColor == null) null else tempBackgroundColor,
                        tempTextColor,
                        tempTextSize.roundToInt(),
                        tempIsVertical,
                        tempHorizontalAlignment
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
    fontFamily: FontFamily = FontFamily.Default,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal
) {
    Column(modifier = modifier) {
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            text.forEach { char ->
                Text(
                    text = char.toString(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    style = TextStyle(
                        color = color,
                        fontSize = textSize.sp,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        textAlign = TextAlign.Center
                    ),
                    overflow = TextOverflow.Visible,
                    softWrap = false
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
