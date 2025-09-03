package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication.data.WidgetRepository
import com.example.myapplication.ui.discoverTrD3121Camera // Исправленный импорт
import com.example.myapplication.ui.WidgetCanvas
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var widgetManager: WidgetManager
    private lateinit var widgetRepository: WidgetRepository
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    private var hasLocationPermission by mutableStateOf(false)


    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController = WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        widgetRepository = WidgetRepository(applicationContext)
        val loadedWidgets = widgetRepository.loadWidgets()
        widgetManager = WidgetManager(loadedWidgets)


        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var systemBarsVisible by rememberSaveable { mutableStateOf(false) }
                var isEditMode by rememberSaveable { mutableStateOf(false) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope() // <--- Scope for coroutines
                var currentDialogWidgetType by remember { mutableStateOf<WidgetType?>(null) }
                var locationString by remember { mutableStateOf("Location: Unknown") }

                var canvasImageBackground by rememberSaveable { mutableStateOf<Uri?>(null) }

                val ColorSaver = Saver<Color, Int>(
                    save = { it.toArgb() },
                    restore = { Color(it) }
                )
                var canvasBackgroundColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(Color.LightGray) }

                var showTextInputDialog by remember { mutableStateOf(false) }
                var currentTextValue by remember { mutableStateOf("") }


                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let { canvasImageBackground = it }
                    }
                )

                val adMediaPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let {
                            // Используем mediaUri: String? теперь
                            if (!widgetManager.addWidget(WidgetType.AD, mediaUri = it.toString())) {
                                Toast.makeText(this@MainActivity, "Could not place AD widget: No free space.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                val gifMediaPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let {
                             // Используем mediaUri: String? теперь
                            if (!widgetManager.addWidget(WidgetType.GIF, mediaUri = it.toString())) {
                                Toast.makeText(this@MainActivity, "Could not place GIF widget: No free space.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                val videoMediaPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let {
                            // Используем mediaUri: String? теперь
                            if (!widgetManager.addWidget(WidgetType.VIDEO, mediaUri = it.toString())) {
                                Toast.makeText(this@MainActivity, "Could not place Video widget: No free space.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                val locationPermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (!hasLocationPermission) {
                            locationString = "Location permission denied."
                            Toast.makeText(this@MainActivity, "Location permission denied. Weather functions may be limited.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                LaunchedEffect(Unit) { // Request permissions on initial launch
                    locationPermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                @SuppressLint("MissingPermission")
                LaunchedEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        try {
                            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                if (lastKnownLocation != null) {
                                    locationString = "Lat: ${lastKnownLocation.latitude}, Lon: ${lastKnownLocation.longitude}"
                                    widgetManager.updateCurrentLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
                                } else {
                                    locationString = "Location: Not available (try enabling location services)"
                                    Toast.makeText(this@MainActivity, locationString, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            locationString = "Location error: ${e.message}"
                            Toast.makeText(this@MainActivity, locationString, Toast.LENGTH_LONG).show()
                        }
                    }
                }


                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text("Настройки", modifier = Modifier.padding(16.dp))
                            Divider()
                            Button(
                                onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Выбрать изображение фона")
                            }
                            Button(
                                onClick = { canvasImageBackground = null },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Сбросить фон")
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Цвет фона", modifier = Modifier.padding(16.dp))
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                ColorButton(color = Color.LightGray) { canvasBackgroundColor = Color.LightGray }
                                ColorButton(color = Color.White) { canvasBackgroundColor = Color.White }
                                ColorButton(color = Color.Black) { canvasBackgroundColor = Color.Black }
                                ColorButton(color = Color.Blue) { canvasBackgroundColor = Color.Blue }
                                ColorButton(color = Color.Green) { canvasBackgroundColor = Color.Green }
                            }
                        }
                    },
                    gesturesEnabled = isEditMode || drawerState.isOpen
                ) {
                    Scaffold(
                        topBar = {
                            if (systemBarsVisible) {
                                TopAppBar(
                                    title = { Text("My Application") },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                                        }
                                    },
                                    actions = {
                                        Text("Режим правок")
                                        Switch(
                                            checked = isEditMode,
                                            onCheckedChange = { isEditMode = it }
                                        )
                                        IconButton(onClick = { currentDialogWidgetType = WidgetType.CLOCK /* Placeholder to show dialog */ }) {
                                            Icon(Icons.Filled.Add, contentDescription = "Добавить виджет")
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    systemBarsVisible = !systemBarsVisible
                                    if (systemBarsVisible) {
                                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                                    } else {
                                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(if (canvasImageBackground != null) Color.Transparent else canvasBackgroundColor)
                        ) {
                            // TODO: Add CoilImage if canvasImageBackground is not null
                            WidgetCanvas(
                                widgetManager = widgetManager,
                                isEditMode = isEditMode,
                                onUpdate = { widgetData: WidgetData ->
                                    widgetManager.updateWidget(widgetData)
                                },
                                checkCollision = { widgetData: WidgetData, x: Float, y: Float, width: Float, height: Float, ignoreSelf: Boolean ->
                                    widgetManager.checkCollisionWithExisting(widgetData, x, y, width, height)
                                }
                            )

                            if (currentDialogWidgetType != null) {
                                AlertDialog(
                                    onDismissRequest = { currentDialogWidgetType = null },
                                    title = { Text("Добавить виджет") },
                                    text = {
                                        Column {
                                            AddWidgetRow(WidgetType.WEATHER, "Погода") {
                                                if (!widgetManager.addWidget(WidgetType.WEATHER)) {
                                                    Toast.makeText(this@MainActivity, "Could not place Weather widget: No free space.", Toast.LENGTH_SHORT).show()
                                                }
                                                currentDialogWidgetType = null
                                            }
                                            AddWidgetRow(WidgetType.CLOCK, "Часы") {
                                                if (!widgetManager.addWidget(WidgetType.CLOCK)) {
                                                    Toast.makeText(this@MainActivity, "Could not place Clock widget: No free space.", Toast.LENGTH_SHORT).show()
                                                }
                                                currentDialogWidgetType = null
                                            }
                                            AddWidgetRow(WidgetType.CAMERA, "Камера") { // This might be the old camera, ensure it's distinct
                                                if (!widgetManager.addWidget(WidgetType.CAMERA)) {
                                                    Toast.makeText(this@MainActivity, "Could not place Camera widget: No free space.", Toast.LENGTH_SHORT).show()
                                                }
                                                currentDialogWidgetType = null
                                            }
                                            // Изменяем логику для ONVIF_CAMERA
                                            AddWidgetRow(WidgetType.ONVIF_CAMERA, "ONVIF Камера") {
                                                scope.launch { // <--- Launch coroutine
                                                    val cameraDevice = discoverTrD3121Camera(this@MainActivity) // <--- Call suspend function
                                                    if (cameraDevice != null && !cameraDevice.rtspUrl.isNullOrEmpty()) {
                                                        if (!widgetManager.addWidget(WidgetType.ONVIF_CAMERA, mediaUri = cameraDevice.rtspUrl)) {
                                                            Toast.makeText(this@MainActivity, "Could not place ONVIF Camera widget: No free space.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "ONVIF камера не найдена или RTSP URL отсутствует.", Toast.LENGTH_LONG).show()
                                                    }
                                                    currentDialogWidgetType = null // Закрываем диалог в любом случае
                                                }
                                            }
                                            AddWidgetRow(WidgetType.AD, "Фото") {
                                                adMediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                                currentDialogWidgetType = null
                                            }
                                            AddWidgetRow(WidgetType.TEXT, "Текст") {
                                                currentDialogWidgetType = null
                                                showTextInputDialog = true
                                            }
                                            AddWidgetRow(WidgetType.GIF, "GIF Анимация") {
                                                gifMediaPickerLauncher.launch(arrayOf("image/gif"))
                                                currentDialogWidgetType = null
                                            }
                                            AddWidgetRow(WidgetType.VIDEO, "Видео") {
                                                videoMediaPickerLauncher.launch(arrayOf("video/*"))
                                                currentDialogWidgetType = null
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { currentDialogWidgetType = null }) {
                                            Text("Отмена")
                                        }
                                    }
                                )
                            }

                            if (showTextInputDialog) {
                                AlertDialog(
                                    onDismissRequest = { showTextInputDialog = false },
                                    title = { Text("Введите текст виджета") },
                                    text = {
                                        TextField(
                                            value = currentTextValue,
                                            onValueChange = { currentTextValue = it },
                                            label = { Text("Текст") }
                                        )
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            if (!widgetManager.addWidget(WidgetType.TEXT, textData = currentTextValue)) {
                                                Toast.makeText(this@MainActivity, "Could not place Text widget: No free space.", Toast.LENGTH_SHORT).show()
                                            }
                                            currentTextValue = ""
                                            showTextInputDialog = false
                                        }) { Text("OK") }
                                    },
                                    dismissButton = {
                                        Button(onClick = { showTextInputDialog = false }) { Text("Отмена") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun AddWidgetRow(type: WidgetType, text: String, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(text)
        }
    }

    @Composable
    fun ColorButton(color: Color, onClick: () -> Unit) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .padding(4.dp)
                .size(40.dp)
                .background(color, CircleShape)
        ) {
        }
    }
}
