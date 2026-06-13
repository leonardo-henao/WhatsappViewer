package com.wappscorp.wpvw

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.wappscorp.wpvw.data.model.MediaType
import com.wappscorp.wpvw.ui.screens.ImageViewerScreen
import com.wappscorp.wpvw.ui.screens.MainScreen
import com.wappscorp.wpvw.ui.screens.VideoPlayerScreen
import com.wappscorp.wpvw.ui.theme.WhatsappViewerTheme
import com.wappscorp.wpvw.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {

    private var hasPermissions by mutableStateOf(false)
    private val viewModel: MediaViewModel by lazy {
        ViewModelProvider(this)[MediaViewModel::class.java]
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            hasPermissions = true
            viewModel.loadMedia()
        } else {
            Toast.makeText(this, "Los permisos son necesarios para acceder a los archivos multimedia", Toast.LENGTH_LONG).show()
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Environment.isExternalStorageManager()) {
            hasPermissions = true
            viewModel.loadMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhatsappViewerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showPermissionDialog by remember { mutableStateOf(needsPermissionDialog()) }

                    if (showPermissionDialog) {
                        PermissionDialog(
                            onAllow = {
                                showPermissionDialog = false
                                launchManageStorageSettings()
                            },
                            onCancel = {
                                showPermissionDialog = false
                                Toast.makeText(this@MainActivity, "Permiso necesario para usar la aplicación", Toast.LENGTH_LONG).show()
                            }
                        )
                    }

                    AppContent(viewModel = viewModel, hasPermissions = hasPermissions, onRequestPermissions = { checkAndRequestPermissions() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermissions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            hasPermissions = true
            viewModel.loadMedia()
        }
    }

    private fun needsPermissionDialog(): Boolean {
        if (hasPermissions) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissions = getRequiredPermissions()
            if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) return true
        }
        return false
    }

    private fun launchManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            manageStorageLauncher.launch(intent)
        } else {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                launchManageStorageSettings()
            }
        } else {
            val permissions = getRequiredPermissions()
            val needsRequest = permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            if (needsRequest) {
                requestPermissionLauncher.launch(permissions.toTypedArray())
            } else {
                hasPermissions = true
                viewModel.loadMedia()
            }
        }
    }

    private fun getRequiredPermissions(): List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

@Composable
fun PermissionDialog(onAllow: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Permiso de almacenamiento requerido") },
        text = {
            Text(
                "WhatsApp Viewer necesita acceso a todos los archivos para poder leer los medios de WhatsApp.\n\n" +
                "WhatsApp guarda sus imágenes, videos y audios en una carpeta protegida del sistema " +
                "(Android/media/com.whatsapp). Para acceder a estos archivos, Android requiere un permiso especial.\n\n" +
                "Tus archivos personales no serán modificados ni compartidos sin tu consentimiento."
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("PERMITIR")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun AppContent(viewModel: MediaViewModel, hasPermissions: Boolean, onRequestPermissions: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMedia = uiState.selectedMedia

    if (selectedMedia != null) {
        BackHandler { viewModel.clearSelectedMedia() }
        when (selectedMedia.type) {
            MediaType.IMAGE -> ImageViewerScreen(selectedMedia, onBack = { viewModel.clearSelectedMedia() }, onDelete = { viewModel.deleteSelected(); viewModel.clearSelectedMedia() })
            MediaType.VIDEO, MediaType.AUDIO -> VideoPlayerScreen(selectedMedia, onBack = { viewModel.clearSelectedMedia() }, onDelete = { viewModel.deleteSelected(); viewModel.clearSelectedMedia() })
        }
    } else {
        MainScreen(viewModel = viewModel, onNavigateToImageViewer = { viewModel.selectMediaForViewing(it) }, onNavigateToVideoPlayer = { viewModel.selectMediaForViewing(it) }, onNavigateToAudioPlayer = { viewModel.selectMediaForViewing(it) }, hasPermissions = hasPermissions, onRequestPermissions = onRequestPermissions)
    }
}
