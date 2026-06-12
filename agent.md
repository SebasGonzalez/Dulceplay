# AGENT.md — Manual de Memoria para DulcePlay 🧠
# Leer esto PRIMERO antes de tocar cualquier código.

---

## 📦 Resumen del Proyecto

**DulcePlay** es un reproductor multimedia Android (Kotlin + Jetpack Compose) que:
1. Busca videos en YouTube vía YouTube Data API v3
2. Extrae URLs de streams reproducibles vía Invidious API JSON
3. Reproduce con ExoPlayer (Media3)
4. Tiene secciones: Explorar, Retro Player, IPTV Sat, Biblioteca, Ajustes

**Versión actual**: 3.5.0  
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
│   ├── assistant/               # Botón flotante del asistente IA
│   ├── auth/                    # Pantalla de autenticación
│   ├── components/              # Componentes reutilizables (GlassBox, etc.)
│   ├── iptv/                    # Pantalla IPTV
│   ├── library/                 # Pantalla biblioteca
│   ├── settings/                # Pantalla ajustes
│   └── theme/                   # Colores, tipografía
└── utils/
    └── SearchEngine.kt          # Motor de búsqueda YouTube + extracción de streams
```

---

## 🔑 Claves y Configuraciones

- **YouTube API Key**: `AIzaSyCrzrUscZ5kEW-rQte8yFxmc4E2xUcDm-Q` — hardcodeada en `SearchEngine.kt`
- **Instancias Invidious** (en orden de prioridad):
  1. `https://inv.nadeko.net`
  2. `https://invidious.nerdvpn.de`
  3. `https://yt.artemislena.eu`
  4. `https://invidious.privacydev.net`
- **Room DB**: `dulce_database`, versión 3, con `fallbackToDestructiveMigration()`

---

## ⚠️ DECISIONES CLAVE Y TRAMPAS CONOCIDAS

### 1. Extracción de streams — USA LA API JSON, NO SCRAPING
- ❌ **NUNCA usar**: `GET /watch?v={id}` + regex en HTML. El HTML de Invidious no contiene el JSON de streams en texto plano.
- ✅ **SIEMPRE usar**: `GET /api/v1/videos/{videoId}?fields=adaptiveFormats,formatStreams`
  - `formatStreams` → video+audio combinados (itag 22=720p, 18=360p)
  - `adaptiveFormats` → audio solo (itag 251=Opus160k, 140=AAC128k) y video sin audio
- Las URLs que devuelve esta API incluyen `expire`, `sig`, y todos los parámetros firmados que Google requiere.
- ExoPlayer los acepta directamente con `MediaItem.fromUri(Uri.parse(url))`.

### 2. Flujo de reproducción
```
Usuario toca video en lista
  → viewModel.playMedia(item)
    → item.streamUrl = videoId (no empieza con "http")
    → cargarOpcionesParaReproducir(videoId)
      → SearchEngine.obtenerEnlaces(videoId) [coroutine IO]
        → Invidious API JSON → lista de Calidad
    → _listaCalidades actualizado
    → LaunchedEffect en PlayerScreen detecta cambio → abre DropdownMenu
  → Usuario selecciona calidad
    → viewModel.reproducirSeleccionado(calidad.url)
      → exoPlayer.setMediaItem(...).prepare().play()
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

## 🧠 Estado de la IA Local (Llama 3 8B)

- **Estado**: Código preparado, pendiente de activación y pruebas
- **Biblioteca**: MLC LLM
- **Comportamiento esperado**: Descarga automática solo por Wi-Fi, funciona sin internet después
- **Próximo paso**: Activar, verificar descarga en segundo plano, pruebas de respuesta

---

## 📝 Reglas para el Agente

1. **NO renombrar** archivos, clases, variables o paquetes existentes
2. **ANALIZAR antes de cambiar** — leer el código relevante completo primero
3. **Actualizar los 4 docs** tras cada cambio: README.md, changelog.md, AGENT.md, y preparar commit
4. **Instancias Invidious pueden fallar** — si todas fallan, `_listaCalidades` queda vacío y `_mediaError` muestra mensaje
5. **No usar youtube-dl para extracción de streams** (ya está en build.gradle pero no implementado en el flujo actual — lo hace Invidious API)

---
*Última actualización: 2026-06-12 | Versión: 3.5.0*
