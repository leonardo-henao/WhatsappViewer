package com.wappscorp.wpvw

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.wappscorp.wpvw.ads.AdManager
import com.wappscorp.wpvw.data.model.MediaType
import com.wappscorp.wpvw.ui.screens.ImageViewerScreen
import com.wappscorp.wpvw.ui.screens.MainScreen
import com.wappscorp.wpvw.ui.screens.VideoPlayerScreen
import com.wappscorp.wpvw.data.model.AppVersionInfo
import com.wappscorp.wpvw.ui.theme.WhatsappViewerTheme
import com.wappscorp.wpvw.viewmodel.MediaViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Info
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

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
            Toast.makeText(
                this,
                "Los permisos son necesarios para acceder a los archivos multimedia",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Environment.isExternalStorageManager()) {
            hasPermissions = true
            viewModel.loadMedia()
        }
    }

    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadInterstitial()

        setContent {
            val pendingAction by viewModel.pendingDeleteAction.collectAsState()

            LaunchedEffect(pendingAction) {
                if (pendingAction != null) {
                    showInterstitialThenExecute()
                }
            }

            var showUpdateDialog by remember { mutableStateOf(false) }
            var updateInfo by remember { mutableStateOf<AppVersionInfo?>(null) }

            LaunchedEffect(Unit) {
                try {
                    val json = withContext(Dispatchers.IO) {
                        URL("https://raw.githubusercontent.com/leonardo-henao/info-apps/refs/heads/main/wp-viewer.json").readText()
                    }
                    val info = Gson().fromJson(json, AppVersionInfo::class.java)
                    if (info.versionCode > BuildConfig.VERSION_CODE) {
                        updateInfo = info
                        showUpdateDialog = true
                    }
                } catch (_: Exception) { }
            }

            WhatsappViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showPermissionDialog by remember { mutableStateOf(needsPermissionDialog()) }

                    if (showPermissionDialog) {
                        PermissionDialog(
                            onAllow = {
                                showPermissionDialog = false
                                launchManageStorageSettings()
                            },
                            onCancel = {
                                showPermissionDialog = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "Permiso necesario para usar la aplicación",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }

                    if (showUpdateDialog && updateInfo != null) {
                        UpdateDialog(
                            info = updateInfo!!,
                            onUpdate = {
                                val url = if (updateInfo!!.urlDownload.startsWith("http"))
                                    updateInfo!!.urlDownload
                                else
                                    "https://${updateInfo!!.urlDownload}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            }
                        )
                    }

                    AppContent(
                        viewModel = viewModel,
                        hasPermissions = hasPermissions,
                        onRequestPermissions = { checkAndRequestPermissions() })
                }
            }
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            this, AdManager.INTERSTITIAL_AD_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialThenExecute() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    viewModel.executePendingDelete()
                    loadInterstitial()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    viewModel.executePendingDelete()
                }
            }
            interstitialAd?.show(this)
        } else {
            viewModel.executePendingDelete()
            loadInterstitial()
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
            if (permissions.any {
                    ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                }) return true
        }
        return false
    }

    private fun launchManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:$packageName".toUri()
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
            val needsRequest = permissions.any {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }
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
fun AppContent(
    viewModel: MediaViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMedia = uiState.selectedMedia

    if (selectedMedia != null) {
        BackHandler { viewModel.clearSelectedMedia() }
        when (selectedMedia.type) {
            MediaType.IMAGE -> ImageViewerScreen(
                selectedMedia,
                onBack = { viewModel.clearSelectedMedia() },
                onDelete = { viewModel.deleteMedia(selectedMedia) })

            MediaType.VIDEO, MediaType.AUDIO -> VideoPlayerScreen(
                selectedMedia,
                onBack = { viewModel.clearSelectedMedia() },
                onDelete = { viewModel.deleteMedia(selectedMedia) })
        }
    } else {
        MainScreen(
            viewModel = viewModel,
            onNavigateToImageViewer = { viewModel.selectMediaForViewing(it) },
            onNavigateToVideoPlayer = { viewModel.selectMediaForViewing(it) },
            onNavigateToAudioPlayer = { viewModel.selectMediaForViewing(it) },
            hasPermissions = hasPermissions,
            onRequestPermissions = onRequestPermissions
        )
    }
}

@Composable
fun UpdateDialog(info: AppVersionInfo, onUpdate: () -> Unit) {
    val lang = Locale.getDefault().language
    val updateText = when (lang) {
        "es" -> info.latestUpdate_es.ifEmpty { info.latestUpdate_en }
        else -> info.latestUpdate_en.ifEmpty { info.latestUpdate_es }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("¡Nueva versión disponible!") },
        text = {
            Column {
                Text("Actualiza para obtener las últimas mejoras:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(updateText)
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text("ACTUALIZAR")
            }
        }
    )
}
