package com.example.myapplication.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import com.example.myapplication.GifImage
import com.example.myapplication.VideoPlayer
import com.example.myapplication.WidgetData
import com.example.myapplication.WidgetType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

fun Float.toSafeDp(minSize: Dp = 1.dp): Dp {
    return if (this.isNaN() || this <= 0f) {
        minSize
    } else {
        this.dp
    }
}

@OptIn(ExperimentalFoundationApi::class) // Added for combinedClickable
@Composable
fun WidgetDisplayItem(
    widgetData: WidgetData,
    isEditMode: Boolean,
    onUpdate: (WidgetData) -> Unit,
    onDeleteRequest: (WidgetData) -> Unit,
    checkCollision: (WidgetData, Float, Float, Float, Float, Boolean) -> Boolean,
    onWidgetDoubleClick: () -> Unit // <-- NEW PARAMETER
) {
    val initialWidth = remember(widgetData.width) { widgetData.width.toFloat().toSafeDp(minSize = 48.dp) }
    val initialHeight = remember(widgetData.height) { widgetData.height.toFloat().toSafeDp(minSize = 48.dp) }

    var currentPosition by remember { mutableStateOf(IntOffset(widgetData.x, widgetData.y)) }
    var currentWidth by remember { mutableStateOf(initialWidth) }
    var currentHeight by remember { mutableStateOf(initialHeight) }

    var dragStartOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var resizeStartSize by remember { mutableStateOf(Pair(0.dp, 0.dp)) }

    val density = LocalDensity.current
    var isColliding by remember { mutableStateOf(false) }

    val normalBorderWidth = with(density) { 1f.toDp() }
    val collidingBorderWidth = with(density) { 5f.toDp() }

    val colorPalette = remember {
        listOf(
            Color.White, Color(0xFFF0F0F0), Color.LightGray, Color.Gray, Color.DarkGray, Color.Black,
            Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB),
            Color(0xFFFFF9C4), Color(0xFFB2EBF2), Color(0xFFE1BEE7)
        )
    }

    LaunchedEffect(widgetData) {
        currentPosition = IntOffset(widgetData.x, widgetData.y)
        currentWidth = widgetData.width.toFloat().toSafeDp(minSize = 48.dp)
        currentHeight = widgetData.height.toFloat().toSafeDp(minSize = 48.dp)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClockStyleDialog by remember(widgetData.id) { mutableStateOf(false) } // Used for Clock and Text
    var showWeatherSettingsDialog by remember(widgetData.id) { mutableStateOf(false) }
    var showEditPropertiesDialog by remember(widgetData.id) { mutableStateOf(false) } // General properties

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить виджет?") },
            text = { Text("Вы уверены, что хотите удалить этот виджет?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteRequest(widgetData)
                    showDeleteDialog = false
                }) { Text("Да") }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) { Text("Нет") }
            }
        )
    }

    Box(
        modifier = Modifier
            .offset { currentPosition } 
            .size(currentWidth, currentHeight)
            .clip(RoundedCornerShape(widgetData.cornerRadius.dp))
            .pointerInput(isEditMode, widgetData) { // For dragging the widget
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = { dragStartOffset = Offset(currentPosition.x.toFloat(), currentPosition.y.toFloat()) },
                        onDragEnd = {
                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), currentWidth.value, currentHeight.value, false) 
                            if (isColliding) {
                                currentPosition = IntOffset(dragStartOffset.x.roundToInt(), dragStartOffset.y.roundToInt())
                            } else {
                                onUpdate(widgetData.copy(x = currentPosition.x, y = currentPosition.y))
                            }
                            isColliding = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = currentPosition.x + dragAmount.x
                            val newY = currentPosition.y + dragAmount.y
                            currentPosition = IntOffset(newX.roundToInt(), newY.roundToInt())
                            isColliding = checkCollision(widgetData, newX, newY, currentWidth.value, currentHeight.value, false) 
                        }
                    )
                }
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable( 
                    onClick = { /* No action on single click on the card itself */ },
                    onLongClick = {
                        if (isEditMode) {
                            when (widgetData.type) {
                                WidgetType.CLOCK -> showClockStyleDialog = true
                                WidgetType.WEATHER -> showWeatherSettingsDialog = true
                                WidgetType.TEXT -> showClockStyleDialog = true 
                                else -> showEditPropertiesDialog = true 
                            }
                        }
                    },
                    onDoubleClick = { // <-- ADDED HANDLER
                        onWidgetDoubleClick()
                    }
                ),
            shape = RoundedCornerShape(widgetData.cornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = widgetData.backgroundColor?.let { Color(it) }
                    ?: if ((widgetData.type == WidgetType.CAMERA || widgetData.type == WidgetType.GIF || widgetData.type == WidgetType.VIDEO || widgetData.type == WidgetType.ONVIF_CAMERA) && widgetData.mediaUri == null) Color.Gray
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (isColliding) {
                BorderStroke(collidingBorderWidth, Color.Red)
            } else null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (widgetData.type) {
                    WidgetType.WEATHER -> WeatherWidgetCard(
                        widget = widgetData,
                        onWeatherSettingsClick = { /* Kept for potential future use, primary is long press */ }, 
                        textColor = widgetData.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface,
                        backgroundColor = widgetData.backgroundColor?.let { Color(it) } ?: Color.Transparent,
                        isEditMode = isEditMode,
                        onLongPress = { 
                            if (isEditMode) {
                                showWeatherSettingsDialog = true
                            }
                        }
                    )
                    WidgetType.CLOCK -> {
                        var currentTime by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            while (true) {
                                currentTime = sdf.format(System.currentTimeMillis())
                                delay(1000L)
                            }
                        }
                        ReusableTextDisplayView(
                            text = currentTime,
                            styleData = widgetData,
                            modifier = Modifier.fillMaxSize(),
                            defaultFontSizeIfNotSet = (currentHeight.value / 3).sp
                        )
                    }
                    WidgetType.CAMERA -> {
                        widgetData.mediaUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(model = it),
                                contentDescription = "Camera feed",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Text("Нет сигнала", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.ONVIF_CAMERA -> {
                        val testMjpegUrl = "http://admin:admin@192.168.1.200/action/stream?subject=mjpeg"
                        val testCameraData = widgetData.copy(
                            mediaUri = Uri.parse(testMjpegUrl)
                        )
                        OnvifCameraDisplay(
                            widgetData = testCameraData,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                        )
                    }
                    WidgetType.AD -> {
                        widgetData.mediaUri?.let { uri ->
                            val context = LocalContext.current
                            val imageWidthPx = with(density) { currentWidth.roundToPx() }
                            val imageHeightPx = with(density) { currentHeight.roundToPx() }

                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(uri)
                                    .size(imageWidthPx, imageHeightPx)
                                    .precision(Precision.EXACT)
                                    .build()
                            )
                            Image(
                                painter = painter,
                                contentDescription = "Advertisement background",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("Advertisement Area", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.TEXT -> {
                        EditableTextWidget(
                            widgetData = widgetData,
                            onWidgetDataChange = onUpdate, 
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    WidgetType.GIF -> {
                        widgetData.mediaUri?.let {
                            GifImage(
                                data = it,
                                contentDescription = "GIF image",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                            )
                        } ?: Text("No GIF selected", style = MaterialTheme.typography.bodyLarge)
                    }
                    WidgetType.VIDEO -> {
                        widgetData.mediaUri?.let {
                            VideoPlayer(
                                videoUri = it,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                            )
                        } ?: Text("No video selected", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (isEditMode) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd).size(36.dp).padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                    }

                    Box( // Resize handle
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            .pointerInput(isEditMode, widgetData) { // For resizing
                                if (isEditMode) {
                                    detectDragGestures(
                                        onDragStart = { resizeStartSize = Pair(currentWidth, currentHeight) },
                                        onDragEnd = {
                                            val finalWidth = kotlin.math.max(currentWidth.value, 48f)
                                            val finalHeight = kotlin.math.max(currentHeight.value, 48f)
                                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), finalWidth, finalHeight, true) 
                                            if (isColliding) {
                                                currentWidth = resizeStartSize.first
                                                currentHeight = resizeStartSize.second
                                            } else {
                                                onUpdate(widgetData.copy(width = finalWidth.roundToInt(), height = finalHeight.roundToInt()))
                                            }
                                            isColliding = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val newWidthDp = currentWidth + with(density) { dragAmount.x.toDp() }
                                            val newHeightDp = currentHeight + with(density) { dragAmount.y.toDp() }
                                            currentWidth = androidx.compose.ui.unit.max(newWidthDp, 48.dp)
                                            currentHeight = androidx.compose.ui.unit.max(newHeightDp, 48.dp)
                                            val tempWidth = kotlin.math.max(currentWidth.value, 48f)
                                            val tempHeight = kotlin.math.max(currentHeight.value, 48f)
                                            isColliding = checkCollision(widgetData, currentPosition.x.toFloat(), currentPosition.y.toFloat(), tempWidth, tempHeight, true) 
                                        }
                                    )
                                }
                            }
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Изменить размер",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(-45f),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    if (showEditPropertiesDialog) { // General Properties Dialog
        WidgetPropertiesDialog(
            showDialog = showEditPropertiesDialog,
            widgetData = widgetData,
            colorPalette = colorPalette,
            isTextColorRelevant = false, 
            onDismissRequest = { showEditPropertiesDialog = false },
            onSave = { newWidth, newHeight, newBackgroundColor, newTextColor ->
                val updatedWidget = widgetData.copy(
                    width = newWidth,
                    height = newHeight,
                    backgroundColor = newBackgroundColor,
                    textColor = newTextColor 
                )
                onUpdate(updatedWidget)
                currentWidth = newWidth.toFloat().toSafeDp(minSize = 48.dp)
                currentHeight = newHeight.toFloat().toSafeDp(minSize = 48.dp)
                showEditPropertiesDialog = false
            }
        )
    }

    if (showWeatherSettingsDialog && widgetData.type == WidgetType.WEATHER) {
        WeatherSettingsDialog(
            initialAutoLocate = widgetData.autoLocate,
            initialManualCity = widgetData.manualCityName,
            initialTextColorInt = widgetData.textColor,
            initialBackgroundColorInt = widgetData.backgroundColor,
            initialWidth = widgetData.width, 
            initialHeight = widgetData.height, 
            onDismissRequest = { showWeatherSettingsDialog = false },
            onSaveSettings = { newAutoLocate, newManualCity, newTextColorInt, newBackgroundColorInt, newWidth, newHeight ->
                val cityOrModeChanged = (widgetData.autoLocate != newAutoLocate) ||
                                        (!newAutoLocate && widgetData.manualCityName != newManualCity?.ifBlank { null })

                val updatedWidget = widgetData.copy(
                    autoLocate = newAutoLocate,
                    manualCityName = if (newAutoLocate) null else newManualCity?.ifBlank { null },
                    cityName = if (newAutoLocate) {
                        if (widgetData.autoLocate && !cityOrModeChanged) widgetData.cityName else null
                    } else {
                        newManualCity?.ifBlank { null }
                    },
                    textColor = newTextColorInt,
                    backgroundColor = newBackgroundColorInt,
                    width = newWidth, 
                    height = newHeight, 
                    temperature = if (cityOrModeChanged) null else widgetData.temperature,
                    weatherDescription = if (cityOrModeChanged) null else widgetData.weatherDescription,
                    weatherIconUrl = if (cityOrModeChanged) null else widgetData.weatherIconUrl
                )
                onUpdate(updatedWidget)
                currentWidth = newWidth.toFloat().toSafeDp(minSize = 48.dp)
                currentHeight = newHeight.toFloat().toSafeDp(minSize = 48.dp)
                showWeatherSettingsDialog = false
            }
        )
    }

    if (showClockStyleDialog && (widgetData.type == WidgetType.CLOCK || widgetData.type == WidgetType.TEXT)) { 
        TextEditDialog(
            showDialog = showClockStyleDialog, 
            widgetData = widgetData,
            initialWidth = widgetData.width, 
            initialHeight = widgetData.height, 
            onDismissRequest = { showClockStyleDialog = false },
            onSave = { newText, newBackgroundColor, newTextColor, newTextSize, newIsVertical, newHorizontalAlignment, newFontFamily, newLineHeightScale, newLetterSpacingSp, newFontWeight, newWidth, newHeight -> 
                val updatedWidget = widgetData.copy(
                    textData = newText, 
                    backgroundColor = newBackgroundColor,
                    textColor = newTextColor,
                    textSize = newTextSize,
                    isVertical = newIsVertical,
                    horizontalAlignment = newHorizontalAlignment,
                    fontFamily = newFontFamily,
                    lineHeightScale = newLineHeightScale,
                    letterSpacingSp = newLetterSpacingSp,
                    fontWeight = newFontWeight,
                    width = newWidth, 
                    height = newHeight 
                )
                onUpdate(updatedWidget)
                currentWidth = newWidth.toFloat().toSafeDp(minSize = 48.dp)
                currentHeight = newHeight.toFloat().toSafeDp(minSize = 48.dp)
                showClockStyleDialog = false
            },
            isTextContentEditable = widgetData.type == WidgetType.TEXT 
        )
    }
}

@Composable
fun WidgetPropertiesDialog( 
    showDialog: Boolean,
    widgetData: WidgetData,
    colorPalette: List<Color>,
    isTextColorRelevant: Boolean, 
    onDismissRequest: () -> Unit,
    onSave: (newWidth: Int, newHeight: Int, newBackgroundColor: Int?, newTextColor: Int?) -> Unit
) {
    if (showDialog) {
        var currentWidthInput by remember { mutableStateOf(widgetData.width.toString()) }
        var currentHeightInput by remember { mutableStateOf(widgetData.height.toString()) }
        var selectedBackgroundColor by remember { mutableStateOf(widgetData.backgroundColor?.let { Color(it) }) }
        var selectedTextColor by remember(widgetData.id, widgetData.textColor, isTextColorRelevant) { 
            mutableStateOf(if(isTextColorRelevant) widgetData.textColor?.let { Color(it) } else null) 
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Edit Widget Properties") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) { 
                    TextField(
                        value = currentWidthInput,
                        onValueChange = { currentWidthInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Width (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = currentHeightInput,
                        onValueChange = { currentHeightInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Height (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Background Color:")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorPalette.forEach { colorItem ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(colorItem, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { selectedBackgroundColor = colorItem }
                                    .border(
                                        if (selectedBackgroundColor == colorItem) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent),
                                        CircleShape
                                    )
                            )
                        }
                    }
                    Button(onClick = { selectedBackgroundColor = null }) {
                        Text("Clear Background Color")
                    }
                    
                    if (isTextColorRelevant) { 
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Text Color:")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorPalette.forEach { colorItem ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(colorItem, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { selectedTextColor = colorItem }
                                        .border(
                                            if (selectedTextColor == colorItem) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent),
                                            CircleShape
                                        )
                                )
                            }
                        }
                        Button(onClick = { selectedTextColor = null }) {
                            Text("Clear Text Color")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newWidth = currentWidthInput.toIntOrNull() ?: widgetData.width
                    val newHeight = currentHeightInput.toIntOrNull() ?: widgetData.height
                    val textColorToSave = if (isTextColorRelevant) { 
                        selectedTextColor?.toArgb()
                    } else {
                        widgetData.textColor 
                    }
                    onSave(newWidth, newHeight, selectedBackgroundColor?.toArgb(), textColorToSave)
                    onDismissRequest()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        )
    }
}
