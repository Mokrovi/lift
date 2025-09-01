@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat // Added this line
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication.data.WidgetRepository
import com.example.myapplication.ui.WidgetCanvas
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var widgetManager: WidgetManager
    private lateinit var widgetRepository: WidgetRepository
    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    private var hasLocationPermission by mutableStateOf(false)


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
                val scope = rememberCoroutineScope()
                var currentDialogWidgetType by remember { mutableStateOf<WidgetType?>(null) }
                var locationString by remember { mutableStateOf("Location: Unknown") }

                var canvasBackground by rememberSaveable { mutableStateOf<Uri?>(null) }

                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let { canvasBackground = it }
                    }
                )

                val adMediaPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri: Uri? ->
                        uri?.let {
                            if (!widgetManager.addWidget(WidgetType.AD, mediaUri = it)) {
                                Toast.makeText(this@MainActivity, "Could not place AD widget: No free space.", Toast.LENGTH_SHORT).show()
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

                LaunchedEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        try {
                            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) // Or GPS_PROVIDER
                                if (lastKnownLocation != null) {
                                    locationString = "Lat: ${lastKnownLocation.latitude}, Lon: ${lastKnownLocation.longitude}"
                                    // Optionally update weather widget if it exists and needs explicit update
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
                                onClick = { canvasBackground = null },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Сбросить фон")
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
                                        IconButton(onClick = { currentDialogWidgetType = WidgetType.CLOCK /* Placeholder */ }) {
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
                                .padding(paddingValues) // Apply padding from Scaffold
                                .background(Color.LightGray)
                        ) {
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
                                            AddWidgetRow(WidgetType.CAMERA, "Камера") {
                                                if (!widgetManager.addWidget(WidgetType.CAMERA)) {
                                                    Toast.makeText(this@MainActivity, "Could not place Camera widget: No free space.", Toast.LENGTH_SHORT).show()
                                                }
                                                currentDialogWidgetType = null
                                            }
                                            AddWidgetRow(WidgetType.AD, "Реклама") {
                                                adMediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                                currentDialogWidgetType = null
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                    dismissButton = {
                                        Button(onClick = { currentDialogWidgetType = null }) {
                                            Text("Отмена")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::widgetRepository.isInitialized) {
            widgetRepository.saveWidgets(widgetManager.widgets.value)
        }
    }

    @Composable
    fun AddWidgetRow(type: WidgetType, text: String, onClickAction: () -> Unit) {
        Button(
            onClick = onClickAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(text)
        }
    }
    // Removed checkAndRequestLocationPermissions as per previous fix request
}
