# AGENT.md — Manual de Memoria para DulcePlay 🧠
# Leer esto PRIMERO antes de tocar cualquier código.

---

## 📦 Resumen del Proyecto

**DulcePlay** es un reproductor multimedia Android (Kotlin + Jetpack Compose) que:
1. Busca videos en YouTube vía YouTube Data API v3 (con fallback a Invidious)
2. Extrae URLs de streams reproducibles directamente desde la API InnerTube de YouTube
3. Reproduce con ExoPlayer (Media3)
4. Tiene secciones: Explorar, Retro Player, IPTV Sat, Biblioteca, Ajustes

**Versión actual**: 3.9.6  
**Entorno de desarrollo**: Antigravity IDE (en lugar de Android Studio)  
**Paquete**: `com.dulce.play`

---

## 🏗️ Arquitectura

```
com.dulce.play/
├── MainActivity.kt          # Actividad principal, navegación entre pantallas (DulceScreen enum)
├── DulcePlayApp.kt          # Application class
├── data/local/entity/
│   └── IPTVEntities.kt      # Room DB: UserAccount, UserProfile, IPTV, Playlist, History
├── domain/model/
│   └── Models.kt            # MediaItem, IPTVChannel, UserProfile, MediaType enum
├── ui/
│   ├── player/
│   │   ├── PlayerViewModel.kt   # ViewModel principal (ÚNICO, compartido entre pantallas)
│   │   └── PlayerScreen.kt      # Pantalla del reproductor / buscador
│   ├── explore/
│   │   └── ExploreScreen.kt     # Pantalla inicio con top charts
│   ├── assistant/               # Asistente IA DULCE-BOT
│   │   ├── AssistantCompanion.kt  # UI del chat + lógica híbrida IA/reglas
│   │   └── LocalAIEngine.kt     # Motor IA local (Gemma 2B via MediaPipe)
│   ├── auth/                    # Pantalla de autenticación
│   ├── components/              # Componentes reutilizables (GlassBox, etc.)
│   ├── iptv/                    # Pantalla IPTV
│   ├── library/                 # Pantalla biblioteca
│   ├── settings/                # Pantalla ajustes
│   └── theme/                   # Colores, tipografía
└── utils/
    ├── SearchEngine.kt          # Motor de búsqueda YouTube + extracción de streams
    └── LocalStorage.kt          # NEW: Persistencia de listas y favoritos en formato JSON
```

---

## 🔑 Claves y Configuraciones

- **YouTube API Key (Búsqueda)**: `AIzaSyCrzrUscZ5kEW-rQte8yFxmc4E2xUcDm-Q` — hardcodeada en `SearchEngine.kt`
- **YouTube API Key (Extracción)**: `AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8` — clave de reproducción probada usada en `extraerDirectoYouTubeConCliente`
- **Instancias Invidious** (Solo Búsqueda Fallback):
  1. `https://invidious.fdn.fr`
  2. `https://inv.nadeko.net`
  3. `https://invidious.privacydev.net`
  4. `https://invidious.lunar.icu`
  5. `https://inv.riverside.rocks`
  6. `https://invidious.nerdvpn.de`
- **Room DB**: `dulce_database`, versión 3, con `fallbackToDestructiveMigration()`

---

## ⚠️ DECISIONES CLAVE Y TRAMPAS CONOCIDAS

### 1. Extracción de streams — REPRODUCCIÓN POR PROXY DE INVIDIOUS (REDUCCIÓN DE ERROR 403)
- ❌ **NUNCA usar**: URLs directas de Google Video de forma directa sin proxy, ya que causan errores HTTP 403 Forbidden por discrepancia de IP y firmas.
- ✅ **SIEMPRE usar**: Consulta a la API JSON de Invidious (`/api/v1/videos/{id}?local=true`) y enrutar a través del endpoint de proxy `/videoplayback` de la instancia de Invidious correspondiente.
- **Firma y Host**: La función `formatearUrlProxyInvidious` reemplaza el host de Google Video (`*.googlevideo.com`) por el dominio del servidor Invidious para que el servidor Invidious actúe como proxy.
- **Filtro de Formatos**: Extraer únicamente `itag=140` (Audio AAC) e `itag=18` (Video 360p + Audio).
- **Fallback**: Si las instancias de Invidious fallan, se recurre a la extracción directa de InnerTube `ANDROID_MUSIC` como plan de respaldo final.

### 2. Flujo de reproducción
```
Usuario toca video en lista
  → viewModel.playMedia(item, autoPlay = true)
    → item.streamUrl = videoId (no empieza con "http")
    → cargarOpcionesParaReproducir(videoId, autoPlay = true)
      → SearchEngine.obtenerEnlaces(videoId) [coroutine IO]
        → 1. Consulta la API de Invidious con local=true y enruta la URL mediante el proxy de la instancia
        → 2. Fallback a InnerTube ANDROID_MUSIC si Invidious falla
        → Devuelve calidades enrutadas directamente al ViewModel
      → reproducirCalidadActual()
        → reproducirSeleccionado(opciones[intentandoIndiceCalidad].url)
          → exoPlayer.stop() -> clear -> setMediaItem(...).prepare().play() (usando User-Agent y Referer estándar de navegador móvil)
        → Si ExoPlayer lanza error (onPlayerError) → Reintenta una vez más la misma calidad. Si vuelve a fallar → intentandoIndiceCalidad++ → reproducirCalidadActual()
```

### 3. ExoPlayer está en el ViewModel (NO en la UI)
- `exoPlayer` es propiedad de `PlayerViewModel`, no se crea en ninguna pantalla.
- `PlayerView` en `PlayerScreen.kt` solo recibe `viewModel.exoPlayer` como referencia.
- **Nunca crear** un ExoPlayer adicional en ninguna pantalla.

### 4. MediaItem — streamUrl contiene el videoId (temporal)
- Al buscar, `streamUrl = it.id` (el videoId de YouTube)
- Al reproducir, si `streamUrl` no empieza con "http", se trata como videoId
- El código en `playMedia()` lo detecta y llama a `cargarOpcionesParaReproducir()`

### 5. Room Database
- Clase: `AppDatabase` en `IPTVEntities.kt` — contiene DAOs para: cuenta, perfil, IPTV, historial, playlists, ajustes
- `fallbackToDestructiveMigration()` activo — los cambios de schema destruyen y recrean la DB

---

## 🧠 IA Local DULCE-MIND (Gemma 2B on-device) — v3.6.0

- **Estado**: ✅ IMPLEMENTADO Y FUNCIONAL
- **Motor**: Google MediaPipe `com.google.mediapipe:tasks-genai:0.10.27`
- **Modelo**: Gemma 2B Instruct cuantizado 4 bits (~1.4 GB)
- **Archivo del motor**: `ui/assistant/LocalAIEngine.kt`
- **Archivo del modelo** (en dispositivo): `context.filesDir/dulce_ai_gemma2b.bin`

### Decisión de arquitectura (por qué NO MLC LLM)
- ❌ MLC LLM no tiene dependencia Gradle estándar. Requiere compilar NDK+Rust+TVM desde fuente.
- ✅ MediaPipe `tasks-genai` tiene dependencia Gradle estándar y soporta Gemma 2B directamente.

### Flujo del motor de IA
```
Abrir chat → LocalAIEngine.initialize(context)
  → checkModelExists() → ¿archivo .bin existe?
    SI → loadModel() → LlmInference.createFromOptions() → state = READY
    NO → checkWifi()
      WIFI → downloadModel() → HTTP GET modelo → FileOutputStream → loadModel()
      DATOS → state = WAITING_WIFI → UI muestra aviso naranja
```

### Estados del motor (`LocalAIEngine.EngineState`)
- `UNINITIALIZED` → No iniciado
- `CHECKING` → Verificando si el modelo existe
- `DOWNLOADING` → Descargando con progreso 0.0-1.0 en `downloadProgress` StateFlow
- `WAITING_WIFI` → Esperando Wi-Fi (solo datos móviles detectado)
- `LOADING` → Cargando modelo en RAM con MediaPipe
- `READY` → Motor listo, `generate()` disponible
- `ERROR` → Error irrecuperable

### Formato de prompt (Gemma Instruct)
```
<start_of_turn>user
[SYSTEM_PROMPT con personalidad DULCE-BOT musical]
[historial reciente opcional]
Usuario: [mensaje actual]<end_of_turn>
<start_of_turn>model
DULCE-BOT:
```

### Motor híbrido en AssistantCompanion.kt
- Si `LocalAIEngine.state == READY` → `LocalAIEngine.generate()` (IA real)
- Si no → `generateRuleBasedResponse()` (reglas como fallback)
- Comandos de acción (buscar, pausar, modos) → siempre se ejecutan directamente

---

## 📝 Reglas para el Agente

1. **NO renombrar** archivos, clases, variables o paquetes existentes
2. **ANALIZAR antes de cambiar** — leer el código relevante completo primero
3. **Actualizar los 4 docs** tras cada cambio: README.md, changelog.md, AGENT.md, y preparar commit
4. **Instancias Invidious pueden fallar** — si todas fallan, `_listaCalidades` queda vacío y `_mediaError` muestra mensaje
5. **No usar youtube-dl para extracción de streams** (ya está en build.gradle pero no implementado en el flujo actual — lo hace Invidious API)

---
*Última actualización: 2026-06-16 | Versión: 3.9.6*
