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
import androidx.compose.foundation.text.KeyboardOptions
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
// import androidx.compose.ui.draw.shadow // Не используется, можно удалить если нет других применений
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
// import androidx.compose.ui.unit.Density // Не используется, можно удалить если нет других применений
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
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
import kotlin.math.max // Убедимся что импорт для max есть, если используется androidx.compose.ui.unit.max, то этот специфичный не нужен
import kotlin.math.roundToInt

// Вспомогательные функции для HEX <-> Color
fun Color.toHexString(includeAlpha: Boolean = false): String {
    val argb = this.toArgb()
    return if (includeAlpha) {
        String.format("#%08X", argb)
    } else {
        String.format("#%06X", (0xFFFFFF and argb))
    }
}

fun parseHexColor(hexString: String): Color? {
    return try {
        val colorString = if (hexString.startsWith("#")) hexString.substring(1) else hexString
        if (colorString.length != 6 && colorString.length != 8) return null // Basic validation for length

        val colorLong = colorString.toLong(16)
        when (colorString.length) {
            6 -> Color(colorLong or 0xFF000000) // Add full alpha if only RGB is provided
            8 -> Color(colorLong)
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}


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
    onWidgetDoubleClick: () -> Unit,
    onChangeMediaRequest: (WidgetData) -> Unit
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

    // val normalBorderWidth = with(density) { 1f.toDp() } // Не используется, можно удалить
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
    var showClockStyleDialog by remember(widgetData.id) { mutableStateOf(false) }
    var showWeatherSettingsDialog by remember(widgetData.id) { mutableStateOf(false) }
    var showEditPropertiesDialog by remember(widgetData.id) { mutableStateOf(false) }
    var showMediaActionDialog by remember(widgetData.id) { mutableStateOf(false) }

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

    if (showMediaActionDialog) {
        AlertDialog(
            onDismissRequest = { showMediaActionDialog = false },
            title = { Text("Выберите действие") },
            text = { Text("Что вы хотите сделать с медиа-виджетом?") },
            confirmButton = {
                Column(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            onChangeMediaRequest(widgetData)
                            showMediaActionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) { Text("Изменить содержимое") }
                    Button(
                        onClick = {
                            showEditPropertiesDialog = true
                            showMediaActionDialog = false
                        },
                         modifier = Modifier.fillMaxWidth()
                    ) { Text("Изменить свойства") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaActionDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .offset { currentPosition }
            .size(currentWidth, currentHeight)
            .clip(RoundedCornerShape(widgetData.cornerRadius.dp))
            .pointerInput(isEditMode, widgetData) {
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
                                WidgetType.AD, WidgetType.GIF, WidgetType.VIDEO -> {
                                    showMediaActionDialog = true
                                }
                                WidgetType.CLOCK -> showClockStyleDialog = true
                                WidgetType.WEATHER -> showWeatherSettingsDialog = true
                                WidgetType.TEXT -> showClockStyleDialog = true
                                else -> showEditPropertiesDialog = true
                            }
                        }
                    },
                    onDoubleClick = {
                        onWidgetDoubleClick()
                    }
                ),
            shape = RoundedCornerShape(widgetData.cornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = widgetData.backgroundColor?.let { Color(it) }
                    ?: if ((widgetData.type == WidgetType.GIF || widgetData.type == WidgetType.VIDEO) && widgetData.mediaUri == null) Color.Gray
                    else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (isColliding) {
                BorderStroke(collidingBorderWidth, Color.Red)
            } else null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
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
                    WidgetType.ONVIF_CAMERA -> {
                        val fixedUrl = "rtsp://192.168.1.188:554/live/sub"
                        key(fixedUrl) { // Key with the fixed URL to ensure recomposition if needed
                            val cameraDataForDisplay = widgetData.copy(
                                mediaUri = Uri.parse(fixedUrl)
                            )
                            OnvifCameraDisplay(
                                widgetData = cameraDataForDisplay,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                            )
                        }
                    }
                    WidgetType.AD -> {
                        key(widgetData.mediaUri) {
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
                    }
                    WidgetType.TEXT -> {
                        EditableTextWidget(
                            widgetData = widgetData,
                            onWidgetDataChange = onUpdate,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    WidgetType.GIF -> {
                        key(widgetData.mediaUri) {
                            widgetData.mediaUri?.let {
                                GifImage(
                                    data = it,
                                    contentDescription = "GIF image",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                                )
                            } ?: Text("No GIF selected", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    WidgetType.VIDEO -> {
                        key(widgetData.mediaUri) {
                            widgetData.mediaUri?.let {
                                VideoPlayer(
                                    videoUri = it,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(widgetData.cornerRadius.dp))
                                )
                            } ?: Text("No video selected", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                if (isEditMode) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd).size(36.dp).padding(4.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            .pointerInput(isEditMode, widgetData) {
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

    if (showEditPropertiesDialog) {
        WidgetPropertiesDialog(
            showDialog = showEditPropertiesDialog,
            widgetData = widgetData,
            colorPalette = colorPalette,
            isTextColorRelevant = when (widgetData.type) { // Определяем релевантность цвета текста
                WidgetType.TEXT, WidgetType.CLOCK, WidgetType.WEATHER -> true
                else -> false
            },
            onDismissRequest = { showEditPropertiesDialog = false },
            onSave = { newX, newY, newWidth, newHeight, newBackgroundColor, newTextColor ->
                val updatedWidget = widgetData.copy(
                    x = newX,
                    y = newY,
                    width = newWidth,
                    height = newHeight,
                    backgroundColor = newBackgroundColor,
                    textColor = newTextColor
                )
                onUpdate(updatedWidget)
                currentPosition = IntOffset(newX, newY)
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
    onSave: (newX: Int, newY: Int, newWidth: Int, newHeight: Int, newBackgroundColor: Int?, newTextColor: Int?) -> Unit
) {
    if (showDialog) {
        var currentXInput by remember { mutableStateOf(widgetData.x.toString()) }
        var currentYInput by remember { mutableStateOf(widgetData.y.toString()) }
        var currentWidthInput by remember { mutableStateOf(widgetData.width.toString()) }
        var currentHeightInput by remember { mutableStateOf(widgetData.height.toString()) }

        var selectedBackgroundColor by remember { mutableStateOf(widgetData.backgroundColor?.let { Color(it) }) }
        var backgroundColorHexInput by remember { mutableStateOf(selectedBackgroundColor?.toHexString() ?: "") }

        var selectedTextColor by remember(widgetData.id, widgetData.textColor, isTextColorRelevant) {
            mutableStateOf(if (isTextColorRelevant) widgetData.textColor?.let { Color(it) } else null)
        }
        var textColorHexInput by remember { mutableStateOf(selectedTextColor?.toHexString() ?: "") }

        // Синхронизация HEX <-> Palette для BackgroundColor
        LaunchedEffect(selectedBackgroundColor) {
            backgroundColorHexInput = selectedBackgroundColor?.toHexString() ?: ""
        }
        LaunchedEffect(backgroundColorHexInput) {
            if (backgroundColorHexInput.startsWith("#") && (backgroundColorHexInput.length == 7 || backgroundColorHexInput.length == 9)) {
                parseHexColor(backgroundColorHexInput)?.let {
                    if (it != selectedBackgroundColor) selectedBackgroundColor = it
                }
            } else if (backgroundColorHexInput.isEmpty()) {
                 selectedBackgroundColor = null
            }
        }

        // Синхронизация HEX <-> Palette для TextColor
        LaunchedEffect(selectedTextColor) {
            textColorHexInput = selectedTextColor?.toHexString() ?: ""
        }
        LaunchedEffect(textColorHexInput) {
            if (textColorHexInput.startsWith("#") && (textColorHexInput.length == 7 || textColorHexInput.length == 9)) {
                parseHexColor(textColorHexInput)?.let {
                    if (it != selectedTextColor) selectedTextColor = it
                }
            } else if (textColorHexInput.isEmpty()) {
                selectedTextColor = null
            }
        }


        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Edit Widget Properties") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TextField(
                        value = currentXInput,
                        onValueChange = { newValue ->
                            currentXInput = newValue.filterIndexed { index, char ->
                                char.isDigit() || (index == 0 && char == '-')
                            }
                        },
                        label = { Text("Position X") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = currentYInput,
                        onValueChange = { newValue ->
                            currentYInput = newValue.filterIndexed { index, char ->
                                char.isDigit() || (index == 0 && char == '-')
                            }
                        },
                        label = { Text("Position Y") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = currentWidthInput,
                        onValueChange = { currentWidthInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Width (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = currentHeightInput,
                        onValueChange = { currentHeightInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Height (dp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
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
                    TextField(
                        value = backgroundColorHexInput,
                        onValueChange = { backgroundColorHexInput = it },
                        label = { Text("Background Color HEX (#RRGGBB or #AARRGGBB)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .background(
                                        parseHexColor(backgroundColorHexInput) ?: Color.Transparent,
                                        CircleShape
                                    )
                                    .border(BorderStroke(1.dp, Color.Gray), CircleShape)
                            )
                        }
                    )
                    Button(onClick = { selectedBackgroundColor = null; backgroundColorHexInput = "" }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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
                        TextField(
                            value = textColorHexInput,
                            onValueChange = { textColorHexInput = it },
                            label = { Text("Text Color HEX (#RRGGBB or #AARRGGBB)") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(20.dp)
                                        .background(
                                            parseHexColor(textColorHexInput)
                                                ?: Color.Transparent, // Fallback if parsing fails
                                            CircleShape
                                        )
                                        .border(BorderStroke(1.dp, Color.Gray), CircleShape)
                                )
                            }
                        )
                        Button(onClick = { selectedTextColor = null; textColorHexInput = "" }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text("Clear Text Color")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newX = currentXInput.toIntOrNull() ?: widgetData.x
                    val newY = currentYInput.toIntOrNull() ?: widgetData.y
                    val newWidth = currentWidthInput.toIntOrNull() ?: widgetData.width
                    val newHeight = currentHeightInput.toIntOrNull() ?: widgetData.height

                    val finalBackgroundColor = parseHexColor(backgroundColorHexInput) ?: selectedBackgroundColor
                    val finalTextColor = if (isTextColorRelevant) parseHexColor(textColorHexInput) ?: selectedTextColor else null

                    onSave(newX, newY, newWidth, newHeight, finalBackgroundColor?.toArgb(), finalTextColor?.toArgb())
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
