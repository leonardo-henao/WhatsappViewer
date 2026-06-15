package com.wappscorp.wpvw.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.wappscorp.wpvw.data.model.MediaType
import com.wappscorp.wpvw.data.model.WhatsAppMedia
import com.wappscorp.wpvw.ui.components.MediaItemCard
import com.wappscorp.wpvw.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MediaViewModel,
    onNavigateToImageViewer: (WhatsAppMedia) -> Unit,
    onNavigateToVideoPlayer: (WhatsAppMedia) -> Unit,
    onNavigateToAudioPlayer: (WhatsAppMedia) -> Unit,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              if (uiState.selectionMode) {
                Text("${uiState.selectedCount} seleccionados")
              } else {
                Text("WhatsApp Viewer")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            navigationIcon = {
              if (uiState.selectionMode) {
                IconButton(onClick = { viewModel.clearSelection() }) {
                  Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Salir selección")
                }
              }
            },
            actions = {
              if (uiState.selectionMode) {
                IconButton(onClick = { viewModel.selectAll() }) {
                  Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todo")
                }
                IconButton(onClick = { viewModel.requestDeleteSelected() }) {
                  Icon(Icons.Filled.Delete, contentDescription = "Eliminar seleccionados")
                }
              } else {
                IconButton(onClick = { showSettingsDialog = true }) {
                  Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                }
              }
            },
        )
      }
  ) { padding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxSize().padding(padding),
          contentPadding = PaddingValues(bottom = 16.dp),
      ) {
        item {
          StorageSummary(
              totalSize = uiState.totalSize,
              imagesSize = uiState.imagesSize,
              audiosSize = uiState.audiosSize,
              videosSize = uiState.videosSize,
          )
        }

        if (uiState.images.isEmpty() && uiState.audios.isEmpty() && uiState.videos.isEmpty()) {
          item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.fillMaxWidth(),
              ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No se encontraron archivos multimedia de WhatsApp",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                        "Busca en:\n/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/\nWhatsApp Images/Sent/ - WhatsApp Images/Private/\nWhatsApp Video/Sent/ - WhatsApp Video/Private/\nWhatsApp Audio/Sent/ - WhatsApp Audio/Private/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
              }
            }
          }
        }

        if (uiState.images.isNotEmpty()) {
          item {
            SectionHeader(
                icon = Icons.Filled.Image,
                title = "Imágenes",
                count = uiState.images.size,
                sectionSize = uiState.imagesSize,
                onDeleteAll = { viewModel.requestDeleteAllByType(MediaType.IMAGE) },
            )
          }
          item { BannerAd() }
          item {
            MediaRow(
                mediaList = uiState.images,
                selectionMode = uiState.selectionMode,
                onMediaClick = onNavigateToImageViewer,
                onSelectionToggle = { viewModel.toggleSelection(it) },
                onShare = { shareMedia(context, it) },
            )
          }
        }

        if (uiState.videos.isNotEmpty()) {
          item {
            SectionHeader(
                icon = Icons.Filled.VideoLibrary,
                title = "Videos",
                count = uiState.videos.size,
                sectionSize = uiState.videosSize,
                onDeleteAll = { viewModel.requestDeleteAllByType(MediaType.VIDEO) },
            )
          }
          item {
            MediaRow(
                mediaList = uiState.videos,
                selectionMode = uiState.selectionMode,
                onMediaClick = onNavigateToVideoPlayer,
                onSelectionToggle = { viewModel.toggleSelection(it) },
                onShare = { shareMedia(context, it) },
            )
          }
        }

        if (uiState.audios.isNotEmpty()) {
          item {
            SectionHeader(
                icon = Icons.Filled.Audiotrack,
                title = "Audios",
                count = uiState.audios.size,
                sectionSize = uiState.audiosSize,
                onDeleteAll = { viewModel.requestDeleteAllByType(MediaType.AUDIO) },
            )
          }
          item {
            MediaRow(
                mediaList = uiState.audios,
                selectionMode = uiState.selectionMode,
                onMediaClick = onNavigateToAudioPlayer,
                onSelectionToggle = { viewModel.toggleSelection(it) },
                onShare = { shareMedia(context, it) },
            )
          }
        }

        item { BannerAd() }
      }
    }
  }

  if (showSettingsDialog) {
    SettingsDialog(
      onDismiss = { showSettingsDialog = false },
      onPrivacyPolicy = {
        showSettingsDialog = false
        showPrivacyDialog = true
      },
      onRecommendedApps = {
        showSettingsDialog = false
        context.startActivity(
          Intent(context, com.wappscorp.wpvw.ui.screens.RecommendedAppsActivity::class.java)
        )
      },
      onShareApp = {
        showSettingsDialog = false
        shareApp(context)
      }
    )
  }

  if (showPrivacyDialog) {
    PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
  }
}

private fun shareApp(context: android.content.Context) {
  val shareText = "Descarga WhatsApp Viewer: https://play.google.com/store/apps/details?id=${context.packageName}"
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, shareText)
  }
  context.startActivity(Intent.createChooser(intent, "Compartir aplicación"))
}

@Composable
fun SettingsDialog(
  onDismiss: () -> Unit,
  onPrivacyPolicy: () -> Unit,
  onRecommendedApps: () -> Unit,
  onShareApp: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Filled.Settings, contentDescription = null)
    },
    title = { Text("Ajustes") },
    text = {
      Column {
        SettingsOption(
          icon = Icons.Filled.Info,
          title = "Ver políticas de privacidad",
          onClick = onPrivacyPolicy
        )
        SettingsOption(
          icon = Icons.Filled.Star,
          title = "Aplicaciones recomendadas",
          onClick = onRecommendedApps
        )
        SettingsOption(
          icon = Icons.Filled.Share,
          title = "Compartir la aplicación",
          onClick = onShareApp
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cerrar")
      }
    }
  )
}

@Composable
fun SettingsOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge
    )
  }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    androidx.compose.material3.Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxSize(0.9f),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Políticas de Privacidad",
            style = MaterialTheme.typography.titleLarge
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
          }
        }
        AndroidView(
          factory = { ctx ->
            WebView(ctx).apply {
              settings.javaScriptEnabled = true
              settings.loadWithOverviewMode = true
              settings.useWideViewPort = true
              webViewClient = WebViewClient()
              loadUrl("https://leonardohenao.com/tecnolapps/privacy-policies/whatsapp-viewer/")
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

private fun shareMedia(context: android.content.Context, media: WhatsAppMedia) {
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", media.file)
  val intent =
      Intent(Intent.ACTION_SEND).apply {
        type =
            when (media.type) {
              MediaType.IMAGE -> "image/*"
              MediaType.VIDEO -> "video/*"
              MediaType.AUDIO -> "audio/*"
            }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
  context.startActivity(Intent.createChooser(intent, "Compartir ${media.name}"))
}

@Composable
fun BannerAd() {
  Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
    AndroidView(
        factory = { ctx ->
          val display = ctx.resources.displayMetrics
          val adWidth = (display.widthPixels / display.density).toInt()
          AdView(ctx).apply {
            adUnitId = com.wappscorp.wpvw.ads.AdManager.BANNER_AD_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
            loadAd(AdRequest.Builder().build())
          }
        },
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
fun StorageSummary(totalSize: Long, imagesSize: Long, audiosSize: Long, videosSize: Long) {
  val totalFormatted = formatSize(totalSize)

  Card(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
          ),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
          text = "Archivos enviados de WhatsApp",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
      )
      Text(
          text = totalFormatted,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
      )
      Text(
          text = "Archivos enviados que ya no son útiles",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
      )
      Spacer(modifier = Modifier.height(12.dp))
      StorageBar(
          label = "Imágenes",
          size = imagesSize,
          total = totalSize,
          color = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      StorageBar(
          label = "Videos",
          size = videosSize,
          total = totalSize,
          color = MaterialTheme.colorScheme.tertiary,
      )
      Spacer(modifier = Modifier.height(8.dp))
      StorageBar(
          label = "Audios",
          size = audiosSize,
          total = totalSize,
          color = MaterialTheme.colorScheme.secondary,
      )
    }
  }
}

@Composable
fun StorageBar(label: String, size: Long, total: Long, color: Color) {
  val sizeFormatted = formatSize(size)
  val progress = if (total > 0) size.toFloat() / total.toFloat() else 0f

  Column {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(text = label, style = MaterialTheme.typography.bodySmall)
      Text(text = sizeFormatted, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = color,
        trackColor = color.copy(alpha = 0.2f),
    )
  }
}

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    sectionSize: Long,
    onDeleteAll: () -> Unit,
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Text(
          text = "$count archivos - ${formatSize(sectionSize)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
      )
    }
    Button(
        onClick = onDeleteAll,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.error,
            ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
      Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("Eliminar todo", style = MaterialTheme.typography.labelSmall)
    }
  }
}

@Composable
fun MediaRow(
    mediaList: List<WhatsAppMedia>,
    selectionMode: Boolean,
    onMediaClick: (WhatsAppMedia) -> Unit,
    onSelectionToggle: (WhatsAppMedia) -> Unit,
    onShare: (WhatsAppMedia) -> Unit,
) {
  LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.height(240.dp),
  ) {
    items(items = mediaList, key = { it.id }) { media ->
      MediaItemCard(
          media = media,
          selectionMode = selectionMode,
          onItemClick = { onMediaClick(media) },
          onSelectionToggle = { onSelectionToggle(media) },
          onShare = onShare,
          modifier = Modifier.width(160.dp),
      )
    }
  }
}

fun formatSize(size: Long): String {
  return when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", size / 1024.0)
    size < 1024 * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(java.util.Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
  }
}
