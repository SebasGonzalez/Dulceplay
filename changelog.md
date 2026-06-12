# Bitácora de Desarrollo - Dulce Player (Cyber Neon Edition)

---

## 🧠 ETAPA 24: Implementación IA Local DULCE-MIND (V3.6.0) 🤖
*Fecha: 2026-06-12 | Agente: Antigravity / Claude Sonnet 4.6 Thinking*

### 📋 Decisión de Arquitectura

Se investigó la viabilidad de MLC LLM + Llama 3 8B:
- **MLC LLM**: ❌ No tiene dependencia Gradle estándar. Requiere compilar desde fuente con NDK + Rust + TVM. No es viable.
- **Solución adoptada**: ✅ **Google MediaPipe `tasks-genai`** + **Gemma 2B Instruct** (cuantizado 4 bits, ~1.4 GB).
  - Tiene dependencia Gradle estándar (`com.google.mediapipe:tasks-genai:0.10.27`)
  - Modelo soporta español nativamente
  - Compatible con Android 10+ (API 29)
  - Funciona 100% offline una vez descargado

### ✅ Archivos Modificados / Creados

**`LocalAIEngine.kt` [NUEVO]** — `ui/assistant/LocalAIEngine.kt`:
- Singleton que gestiona el ciclo de vida completo del motor de IA
- Estados: `UNINITIALIZED → CHECKING → DOWNLOADING → LOADING → READY`
- Descarga automática solo por Wi-Fi con progreso 0-100%
- Carga del modelo con `LlmInference.createFromOptions()`
- `generate(userMessage, conversationHistory)` → respuesta de texto real
- System prompt configura Gemma 2B como DULCE-BOT musical colombiano
- Formato de prompt `<start_of_turn>user...<end_of_turn><start_of_turn>model` (Gemma Instruct)

**`AssistantCompanion.kt` [MODIFICADO]**:
- `IntelligenceCenterDialog`: Inicializa `LocalAIEngine.initialize(context)` al abrir el chat
- Añadido panel de estado IA: spinner de descarga con % de progreso, badge verde cuando está listo, aviso naranja cuando espera Wi-Fi, botón de reintento en caso de error
- `processAssistantQuery()`: Motor híbrido — si `LocalAIEngine.state == READY` usa Gemma 2B real; si no, usa `generateRuleBasedResponse()` como fallback
- `generateRuleBasedResponse()`: Las reglas antiguas refactorizadas como función privada de fallback, con prefijo informando el estado actual de la IA

**`build.gradle.kts` [MODIFICADO]**:
- Añadido: `implementation("com.google.mediapipe:tasks-genai:0.10.27")`
- Añadido: `implementation("androidx.work:work-runtime-ktx:2.9.1")`

### 🎯 Comportamiento Esperado

1. Usuario abre chat → `LocalAIEngine.initialize()` detecta si el modelo existe
2. **Sin modelo, con Wi-Fi** → descarga automática en segundo plano, barra de progreso en UI
3. **Sin modelo, sin Wi-Fi** → aviso naranja, toca para reintentar
4. **Modelo listo** → badge verde "DULCE-MIND activo"
5. **Usuario escribe "Recomiéndame 3 canciones de vallenato"** → Gemma 2B genera respuesta real con conocimiento musical
6. **Comandos de acción** (buscar, pausar, modo fácil) → siempre se ejecutan directamente sin pasar por la IA

---

## 🔧 ETAPA 23: Corrección Crítica de Reproducción - Invidious API (V3.5.0) 🎯
*Fecha: 2026-06-12 | Agente: Antigravity / Claude Sonnet 4.6 Thinking*

### 🐛 Bug Raíz Identificado y Corregido

**PROBLEMA 1 — "Recuperado" en menú de calidad:**
- El código anterior intentaba scraping HTML de `inv.nadeko.net/watch?v={id}` buscando el patrón `"itag":251.*?"url":"([^"]+)"` en el HTML.
- **Por qué fallaba**: Invidious devuelve una página HTML renderizada (no JSON). Los datos de streams están embebidos en JavaScript del lado del cliente, no en texto plano del HTML. El regex NUNCA hacía match.
- **Resultado**: La lista quedaba vacía, caía al bloque `catch`, y se añadían las opciones "Recuperado" con URLs falsas de `googlevideo.com` sin parámetros firmados.

**PROBLEMA 2 — Reproducción en 00:00, sin sonido:**
- Las URLs del fallback (`https://rr3---sn-5hne6nsd.googlevideo.com/videoplayback?id=X&itag=251`) son completamente inválidas.
- Los servidores de Google Video requieren parámetros firmados: `expire`, `sig`, `ipbits`, `ip`, `requiressl`, `mh`, `mm`, `mn`, `ms`, `mv`, `mvi`, `pl`, etc.
- ExoPlayer intentaba cargar esas URLs, recibía HTTP 403, y nunca avanzaba el tiempo.

### ✅ Solución Implementada

**`SearchEngine.kt` — Reescrito con Invidious API JSON:**
- Se usa `GET /api/v1/videos/{videoId}?fields=adaptiveFormats,formatStreams`
- Esta endpoint devuelve JSON con URLs completas, válidas y firmadas
- `formatStreams`: streams combinados (video+audio), itag 22 (720p) y 18 (360p)
- `adaptiveFormats`: audio solo, itag 251 (Opus 160kbps), 140 (AAC 128kbps), 250, 249
- Fallback automático entre 4 instancias públicas: `inv.nadeko.net`, `invidious.nerdvpn.de`, `yt.artemislena.eu`, `invidious.privacydev.net`
- Nombres descriptivos reales: "Audio Opus 160kbps 🎵", "Video 720p HD 🎬", etc.

**`PlayerViewModel.kt` — Mejorado:**
- `cargarOpcionesParaReproducir`: limpia lista anterior al inicio, reporta error si no hay streams
- `reproducirSeleccionado`: usa `Uri.parse()` explícito para máxima compatibilidad con ExoPlayer
- Mensajes de error visibles en UI via `_mediaError`

**`PlayerScreen.kt` — Mejorado:**
- `LaunchedEffect(calidades)`: abre el menú automáticamente cuando llegan los streams
- Muestra spinner durante la carga de streams
- Muestra errores en rojo si falla la extracción
- Iconos diferenciados: 🎧 para audio, 📹 para video en el menú desplegable
- Muestra título del medio actual bajo el reproductor

---

## 🛠️ ETAPA 22: El Salto Profesional - ExoPlayer & YouTube-DL (V3.4) 🎥🔊🔥
*   **Motor de Reproducción de Élite 🔊**:
    *   **Integración ExoPlayer (Media3)**: Migración total al reproductor estándar de la industria.
    *   **Controles Nativos**: Play/Pausa, barra de tiempo interactiva y visualización de progreso real sincronizada.
*   **Extracción de Enlaces Maestra 🧬**:
    *   **YouTube-DL para Android**: Implementación de la librería `youtubedl-android`.
    *   **Selección de Calidad**: El usuario puede elegir entre Solo Audio (HQ) o Video (hasta 1080p).
*   **Arquitectura y Estabilidad 🏗️**:
    *   **Diferimiento de Carga**: La búsqueda es instantánea (trae IDs); la conversión a URL reproducible ocurre solo al dar clic.
*   **Compilación V3.4 🚀**: Ajustes en `build.gradle.kts` para soporte NDK. Build exitoso y estable.

---

## 🛠️ ETAPA 21: Optimización de Búsqueda y Flujo de Datos (V3.3.3) ⚡🔍
*   **Búsqueda Ultra-Rápida**: Separación de los procesos de búsqueda y conversión.
*   **Conversión On-Demand**: La URL reproducible se obtiene al momento de la selección.
*   **Fallback de Conversión**: Sistema híbrido (Regex + yt1s API) para garantizar enlace funcional.

---

## 🛠️ ETAPA 20: Activación Master de YouTube y Conexión Final (V3.2) 🌐💎
*   **API Key Oficial**: Implementada en `SearchEngine.kt`.
*   **Filtrado por Categoría**: Búsquedas restringidas a la categoría "Música" (ID 10).
*   **Enlace de Datos UI/VM 🧬**: Vinculación total de `_onlineSearchResults` con el motor de búsqueda.

---
*Estado del Proyecto: V3.5.0 — Reproducción REAL activa vía Invidious API. Menú de calidad con nombres descriptivos.*
