# WhatsApp Viewer — Project Resume

## Descripción
App Android (Jetpack Compose) que lee y gestiona archivos multimedia enviados de WhatsApp (Sent/Private) desde:
`/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp {Images,Video,Audio}/{Sent,Private}/`

## Build
- `./gradlew assembleDebug` → genera `app/build/outputs/apk/debug/app-debug.apk`
- Gradle wrapper, version catalog (`libs.versions.toml`)
- Min SDK: 26, Target SDK: 34, Compile SDK: 34

## Enviar APK al teléfono
```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/Documentos/release/WhatsAppViewer-v1.0-debug.apk
kdeconnect-cli --device d5dc0157df1a4402b7cc6534f6111722 --share ~/Documentos/release/WhatsAppViewer-v1.0-debug.apk
```

## Permisos
- **Android 11+**: `MANAGE_EXTERNAL_STORAGE` — abre `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` con el package URI para ir directo a la app en configuración.
- **Android 10**: `READ_EXTERNAL_STORAGE` + `requestLegacyExternalStorage="true"` en manifest.
- **Android 9-**: `READ_EXTERNAL_STORAGE` vía `RequestMultiplePermissions`.

Flujo: Diálogo explicativo → "Permitir" → abre settings de la app → `onResume()` detecta concesión y carga medios.

## Arquitectura
- `MainActivity.kt` — permisos, navegación, hosting del diálogo de permiso principal
- `WhatsAppViewerApp.kt` — inicializa Firebase + MobileAds
- `MediaViewModel.kt` — estado con `MutableStateFlow<MediaUiState>`
- `MediaRepository.kt` — con MANAGE_STORAGE usa `File.listFiles()` directo; fallback a MediaStore con filtro por DATA path conteniendo "WhatsApp"
- `AdManager.kt` — constantes con IDs reales AdMob en el código

## AdMob
IDs reales ya configurados en `AdManager.kt`:
- Banner: `ca-app-pub-1892613718311718/1805140488`
- Interstitial: `ca-app-pub-1892613718311718/7907514402`
- Rewarded: `ca-app-pub-1892613718311718/2424424290`
- App ID: `ca-app-pub-1892613718311718~3514103128` (en `strings.xml`)

Si no se ven anuncios en debug, es esperable (AdMob no sirve tráfico de test). Verificar con test IDs:
- Banner test: `ca-app-pub-3940256099942544/6300978111`
- Interstitial test: `ca-app-pub-3940256099942544/1033173712`
- Rewarded test: `ca-app-pub-3940256099942544/5224354917`

## google-services.json
Placeholder. Reemplazar por archivo real de Firebase Console antes de release.

## Pantallas
1. **MainScreen** (`LazyColumn`): resumen de almacenamiento, filas de imágenes/video/audio, banner ad abajo
2. **ImageViewerScreen**: `AsyncImage` fullscreen con topbar back/delete
3. **VideoPlayerScreen**: ExoPlayer con `DisposableEffect` para liberar recurso al salir

## Navegación
No usa Navigation Component. Usa `selectedMedia` en ViewModel. Si es != null, muestra detalle; si es null, MainScreen. `BackHandler` en detalle llama `clearSelectedMedia()`.

## Share
FileProvider con rutas a cada carpeta de WhatsApp en `file_paths.xml`.

## Dependencias clave
- Coil (AsyncImage)
- Media3 ExoPlayer
- Firebase BoM + AdMob
- Navigation Compose (dependencia pero no usada)
- Gson
- Lifecycle ViewModel Compose
