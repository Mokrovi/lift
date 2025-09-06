package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape // Added import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
// import androidx.compose.material.icons.filled.PhotoLibrary // Commented out for testing
import androidx.compose.material.icons.filled.Settings // Added for testing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.myapplication.data.WidgetRepository
import com.example.myapplication.ui.discoverTrD3121Camera // Corrected import
import com.example.myapplication.ui.WidgetCanvas
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


const val KEY_BACKGROUND_TYPE = "background_type"
const val KEY_BACKGROUND_COLOR_ARGB = "background_color_argb"
const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"
class MainActivity : ComponentActivity() {

    private lateinit var widgetManager: WidgetManager
    private lateinit var widgetRepository: WidgetRepository
    private lateinit var sharedPreferences: SharedPreferences

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO, // If you plan to support audio files in AD widget
            Manifest.permission.CAMERA, // If you plan to add direct camera capture widgets
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE, // For ONVIF discovery
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE // For ONVIF discovery
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Potentially needed for older OS if saving files locally
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        )
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        permissionsResult.entries.forEach { entry ->
            Log.d("Permissions", "${entry.key} = ${entry.value}")
            if (!entry.value) {
                // Handle permission denial, e.g., show a message to the user
                Toast.makeText(this, "${entry.key} permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyFileFromUri(context: Context, uri: Uri, newName: String? = null): Uri? {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("copyFileFromUri", "Failed to get input stream for URI: $uri")
                return null
            }

            val originalFileName = context.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) it.getString(nameIndex) else null
                } else null
            } ?: "temp_file"

            val targetFileName = newName ?: originalFileName
            val file = File(context.filesDir, targetFileName)
            outputStream = FileOutputStream(file)

            val buffer = ByteArray(4 * 1024) // 4K buffer
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()
            Log.d("copyFileFromUri", "File copied to: ${file.absolutePath}")
            return Uri.fromFile(file) // Return a file URI for the copied file
        } catch (e: Exception) {
            Log.e("copyFileFromUri", "Error copying file from URI: $uri", e)
            Toast.makeText(context, "Error copying file: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        widgetRepository = WidgetRepository(this)
        val initialWidgets = widgetRepository.loadWidgets() // Assuming loadWidgets() exists and returns List<WidgetData>
        widgetManager = WidgetManager(initialWidgets) 

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        requestPermissionsLauncher.launch(permissions)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                var isEditMode by remember { mutableStateOf(false) }
                var currentDialogWidgetType by remember { mutableStateOf<WidgetType?>(null) }
                var systemBarsVisible by remember { mutableStateOf(false) }
                var showTextInputDialog by remember { mutableStateOf(false) }
                var currentTextValue by remember { mutableStateOf("") }

                var backgroundType by remember { mutableStateOf(sharedPreferences.getString(KEY_BACKGROUND_TYPE, "color") ?: "color") }
                var canvasBackgroundColor by remember { mutableStateOf(Color(sharedPreferences.getInt(KEY_BACKGROUND_COLOR_ARGB, Color.DarkGray.toArgb()))) }
                var canvasImageBackgroundUriString by remember { mutableStateOf(sharedPreferences.getString(KEY_BACKGROUND_IMAGE_URI, null)) }

                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    if (it.resultCode == Activity.RESULT_OK) {
                        it.data?.data?.let { uri ->
                            context.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            canvasImageBackgroundUriString = uri.toString()
                            backgroundType = "image"
                            with(sharedPreferences.edit()) {
                                putString(KEY_BACKGROUND_TYPE, "image")
                                putString(KEY_BACKGROUND_IMAGE_URI, uri.toString())
                                apply()
                            }
                        }
                    }
                }

                val adMediaPickerLauncher = createMediaPickerLauncher { uri, originalUri ->
                     val copiedUri = copyFileFromUri(context, originalUri, "ad_media_${System.currentTimeMillis()}")
                    if (copiedUri != null) {
                        if (!widgetManager.addWidget(WidgetType.AD, mediaUri = copiedUri.toString())) {
                            Toast.makeText(this@MainActivity, "Could not place AD widget: No free space.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to process selected media for AD widget.", Toast.LENGTH_SHORT).show()
                    }
                }

                val gifMediaPickerLauncher = createMediaPickerLauncher { uri, originalUri ->
                    val copiedUri = copyFileFromUri(context, originalUri, "gif_${System.currentTimeMillis()}.gif")
                    if (copiedUri != null) {
                        if (!widgetManager.addWidget(WidgetType.GIF, mediaUri = copiedUri.toString())) {
                            Toast.makeText(this@MainActivity, "Could not place GIF widget: No free space.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to process selected GIF.", Toast.LENGTH_SHORT).show()
                    }
                }

                val videoMediaPickerLauncher = createMediaPickerLauncher { uri, originalUri ->
                    val copiedUri = copyFileFromUri(context, originalUri, "video_${System.currentTimeMillis()}")
                     if (copiedUri != null) {
                        if (!widgetManager.addWidget(WidgetType.VIDEO, mediaUri = copiedUri.toString())) {
                            Toast.makeText(this@MainActivity, "Could not place Video widget: No free space.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to process selected video.", Toast.LENGTH_SHORT).show()
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text("Настройки приложения", fontSize = 20.sp, modifier = Modifier.padding(16.dp))
                            Divider()
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "image/*" // Allow all image types
                                    }
                                    filePickerLauncher.launch(intent)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = "Выбрать фоновое изображение", modifier = Modifier.padding(end = 8.dp)) // Changed for testing
                                Text("Выбрать фоновое изображение")
                            }
                            if (canvasImageBackgroundUriString != null) {
                                Button(
                                    onClick = {
                                        canvasImageBackgroundUriString = null
                                        canvasBackgroundColor = Color.DarkGray // Reset to default color or last used color
                                        backgroundType = "color"
                                        with(sharedPreferences.edit()) {
                                            putString(KEY_BACKGROUND_TYPE, "color")
                                            remove(KEY_BACKGROUND_IMAGE_URI)
                                            // Optionally save the current canvasBackgroundColor if you want it to persist
                                            putInt(KEY_BACKGROUND_COLOR_ARGB, canvasBackgroundColor.toArgb())
                                            apply()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text("Убрать фоновое изображение")
                                }
                            }
                             Divider(modifier = Modifier.padding(vertical = 8.dp))
                             Text("Или выберите цвет фона:", modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround // Distribute colors evenly
                            ) {
                                val colors = listOf(Color.LightGray, Color.White, Color.Black, Color.Blue, Color.Green, Color.DarkGray, Color.Red, Color.Yellow)
                                colors.forEach { color ->
                                    ColorButton(color = color) {
                                        canvasBackgroundColor = color
                                        canvasImageBackgroundUriString = null // Remove image if color is chosen
                                        backgroundType = "color"
                                        with(sharedPreferences.edit()) {
                                            putString(KEY_BACKGROUND_TYPE, "color")
                                            putInt(KEY_BACKGROUND_COLOR_ARGB, color.toArgb())
                                            remove(KEY_BACKGROUND_IMAGE_URI) // Ensure image URI is cleared
                                            apply()
                                        }
                                    }
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Button(
                                onClick = {
                                    widgetRepository.saveWidgets(widgetManager.widgets.value)
                                    scope.launch {
                                        drawerState.close()
                                    }
                                    Toast.makeText(context, "Расположение виджетов сохранено", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Сохранить расположение виджетов")
                            }
                            Spacer(Modifier.weight(1f)) // Pushes the close button to the bottom
                            OutlinedButton(
                                onClick = { scope.launch { drawerState.close() } },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Закрыть")
                            }
                        }
                    },
                    gesturesEnabled = isEditMode || drawerState.isOpen
                ) {
                    Scaffold(
                        containerColor = Color.Transparent, 
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
                                            onCheckedChange = { 
                                                isEditMode = it 
                                                if (!isEditMode) { // Save widgets when exiting edit mode
                                                    widgetRepository.saveWidgets(widgetManager.widgets.value)
                                                    Toast.makeText(context, "Расположение виджетов сохранено", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        IconButton(onClick = { currentDialogWidgetType = WidgetType.TEXT }) { // Default to text, or remove default
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
                                .background(
                                    if (backgroundType == "image" && canvasImageBackgroundUriString != null) {
                                        Color.Transparent 
                                    } else {
                                        canvasBackgroundColor
                                    }
                                )
                        ) {
                            if (backgroundType == "image") {
                                canvasImageBackgroundUriString?.let { uriString ->
                                    AsyncImage(
                                        model = Uri.parse(uriString),
                                        contentDescription = "Фоновое изображение холста",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            WidgetCanvas(
                                widgetManager = widgetManager,
                                isEditMode = isEditMode,
                                onUpdate = { widgetData: WidgetData ->
                                    widgetManager.updateWidget(widgetData)
                                },
                                checkCollision = { widgetData: WidgetData, x: Float, y: Float, width: Float, height: Float, ignoreSelf: Boolean ->
                                    widgetManager.checkCollisionWithExisting(widgetData, x, y, width, height)
                                },
                                onCanvasDoubleClick = { 
                                    systemBarsVisible = !systemBarsVisible
                                    if (systemBarsVisible) {
                                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                                    } else {
                                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                                    }
                                }
                            )

                            if (currentDialogWidgetType != null) {
                                AddWidgetDialog(
                                    onDismissRequest = { currentDialogWidgetType = null },
                                    onAddWidget = { widgetType, data ->
                                        val success = when (widgetType) {
                                            WidgetType.TEXT -> widgetManager.addWidget(widgetType, textData = data as String)
                                            WidgetType.AD, WidgetType.GIF, WidgetType.VIDEO -> {
                                                // Launch the appropriate picker
                                                when (widgetType) {
                                                    WidgetType.AD -> adMediaPickerLauncher.launch("*/*") // Changed
                                                    WidgetType.GIF -> gifMediaPickerLauncher.launch("image/gif") // Changed
                                                    WidgetType.VIDEO -> videoMediaPickerLauncher.launch("video/*") // Changed
                                                    else -> {}
                                                }
                                                true // Assume picker will handle adding or show toast
                                            }
                                            WidgetType.ONVIF_CAMERA -> {
                                                scope.launch {
                                                    val cameraDevice = discoverTrD3121Camera(this@MainActivity)
                                                    if (cameraDevice != null && !cameraDevice.streamUrl.isNullOrEmpty()) {
                                                        if (!widgetManager.addWidget(WidgetType.ONVIF_CAMERA, mediaUri = cameraDevice.streamUrl)) {
                                                            Toast.makeText(this@MainActivity, "Could not place ONVIF Camera: No free space.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(this@MainActivity, "ONVIF camera not found or RTSP URL missing.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                true // Assume async operation handles success/failure toast
                                            }
                                            else -> widgetManager.addWidget(widgetType)
                                        }
                                        if (!success && widgetType != WidgetType.AD && widgetType != WidgetType.GIF && widgetType != WidgetType.VIDEO && widgetType != WidgetType.ONVIF_CAMERA) { // Check success for non-picker/non-async types
                                            Toast.makeText(this@MainActivity, "Could not place ${widgetType.name} widget: No free space or error.", Toast.LENGTH_SHORT).show()
                                        }
                                        currentDialogWidgetType = null // Close dialog after attempting to add
                                        if (widgetType == WidgetType.TEXT && data == "") showTextInputDialog = true // Reopen text input if it was empty from general dialog
                                    },
                                    onShowTextDialog = { showTextInputDialog = true },
                                    widgetManager = widgetManager,
                                    scope = scope,
                                    mainActivity = this@MainActivity,
                                    adMediaPickerLauncher = adMediaPickerLauncher,
                                    gifMediaPickerLauncher = gifMediaPickerLauncher,
                                    videoMediaPickerLauncher = videoMediaPickerLauncher
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
                                        }) {
                                            Text("Добавить")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showTextInputDialog = false }) {
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

    @Composable
    private fun createMediaPickerLauncher(
        onMediaSelected: (selectedUri: Uri, originalUri: Uri) -> Unit
    ): ManagedActivityResultLauncher<String, Uri?> { // Changed return type
        val context = LocalContext.current
        return rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val originalUriForCopy = it // Preserve the original URI for copying
                // For persistent access if needed across device restarts, or if you are not copying immediately:
                // context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onMediaSelected(it, originalUriForCopy)
            }
        }
    }
}

@Composable
fun ColorButton(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, shape = CircleShape)
            .clickable(onClick = onClick)
            .border(1.dp, Color.Black, CircleShape) 
    )
}

@Composable
fun AddWidgetDialog(
    onDismissRequest: () -> Unit,
    onAddWidget: (WidgetType, Any?) -> Unit, 
    onShowTextDialog: () -> Unit,
    widgetManager: WidgetManager, 
    scope: CoroutineScope,
    mainActivity: MainActivity, 
    adMediaPickerLauncher: ManagedActivityResultLauncher<String, Uri?>, // Changed type
    gifMediaPickerLauncher: ManagedActivityResultLauncher<String, Uri?>, // Changed type
    videoMediaPickerLauncher: ManagedActivityResultLauncher<String, Uri?> // Changed type
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Добавить виджет") },
        text = {
            Column {
                WidgetType.entries.forEach { widgetType ->
                    AddWidgetRow(widgetType, widgetType.displayName) {
                        when (widgetType) {
                            WidgetType.TEXT -> onShowTextDialog()
                            WidgetType.AD -> adMediaPickerLauncher.launch("*/*") // Changed argument
                            WidgetType.GIF -> gifMediaPickerLauncher.launch("image/gif") // Changed argument
                            WidgetType.VIDEO -> videoMediaPickerLauncher.launch("video/*") // Changed argument
                            WidgetType.ONVIF_CAMERA -> {
                                // ONVIF discovery is now part of the main onAddWidget flow
                                onAddWidget(WidgetType.ONVIF_CAMERA, null)
                            }
                            else -> onAddWidget(widgetType, null) // For Weather, Clock
                        }
                        // Close dialog for types that don't open another dialog/picker immediately
                        if (widgetType != WidgetType.TEXT && widgetType != WidgetType.AD && 
                            widgetType != WidgetType.GIF && widgetType != WidgetType.VIDEO && widgetType != WidgetType.ONVIF_CAMERA) {
                            onDismissRequest()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        }
    )
}


@Composable
fun AddWidgetRow(widgetType: WidgetType, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}