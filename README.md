# DulcePlay V3.9.1 (DULCE-MIND — Edición Definitiva) 🚀🎥🎧🔊🧠

DulcePlay es la cúspide de la ingeniería multimedia para Android. Una entidad digital diseñada para ofrecer una fidelidad de audio impecable, reproducción de video en Full HD, reproducción en segundo plano, y una inteligencia artificial real que controla la app y funciona **sin internet**.

## 🌟 Características Destacadas

- 🧠 **DULCE-MIND (IA Local Real)**: Motor de inteligencia artificial on-device usando **Gemma 2B Instruct** via Google MediaPipe. Se descarga automáticamente por Wi-Fi (~1.4GB, solo una vez). Una vez descargado, responde preguntas de música, recomienda canciones y conversa inteligentemente **sin necesitar internet**.
- 🔊 **Motor ExoPlayer (Media3)**: Reproducción profesional sin pausas. Soporte nativo para flujos de alta velocidad y gestión inteligente de buffer.
- 🔁 **Cola de Reproducción Inteligente (Auto-Queue)**: Carga automática de todos los resultados de búsqueda en la cola de reproducción para reproducción continua (siguiente tema automático, modo shuffle y repeat personalizables).
- 📂 **Listas y Favoritos Offline**: Persistencia local en tiempo real usando formato JSON de alta velocidad (`LocalStorage.kt`) para almacenar favoritos y listas personalizadas que no se pierden al cerrar la app.
- 🛠️ **Plan B de Respaldo Doble**: Extracción de streams combinada usando la API JSON de Invidious (con 6 instancias y fallback en cascada de proxy) y **extracción directa de YouTube** como Plan B de respaldo (InnerTube API con emulación de clientes de bajo nivel).
- 🧪 **Validación de Enlaces (Probing)**: Comprobación automática de URLs mediante peticiones concurrentes rápidas (`HEAD`/`GET` parcial) para evitar congelamientos en `00:00` y reproducir de manera instantánea.
- 🎧 **Reproducción en Segundo Plano**: Servicio de primer plano integrado (`PlaybackService` con `MediaSessionService` de Media3) y notificación persistente en el sistema con controles multimedia (⏸️ ⏭️ ⏮️) para seguir escuchando con la pantalla apagada o usando otras aplicaciones.
- 🎨 **Asistente Flotante Draggable**: El orbe holográfico de `DULCE-BOT` ahora es flotante y arrastrable por toda la pantalla en Compose, permitiendo minimizar el chat para usar la aplicación libremente mientras el bot sigue activo.
- 🇨🇴 **Identidad Nacional e Inicio Real**: 10 secciones de exploración con contenido dinámico real (Top 50 YouTube, Tendencias Colombia, Lo Más Escuchado, Vallenato, Salsa, Urbano, Popular, etc.).
- 🎨 **Estética Cyber-Neon**: Interfaz ultramoderna basada en Glassmorphism con fondos de plasma dinámicos, barras de ecualizador y partículas interactivas.
- 📻 **IPTV Satelital**: Sintoniza canales de TV y emisoras de radio globales.

## 🛠️ Estado Actual del Desarrollo

| Función | Estado |
|---------|--------|
| Búsqueda YouTube | ✅ FUNCIONA |
| Interfaz Cyber-Neon | ✅ FUNCIONA |
| Cola de Reproducción Continua | ✅ IMPLEMENTADO v3.9 |
| Persistencia de Favoritos y Listas (JSON) | ✅ IMPLEMENTADO v3.9 |
| Plan B de Extracción Directa de YouTube | ✅ IMPLEMENTADO v3.9.1 |
| Validación de Red contra Congelamientos (00:00) | ✅ IMPLEMENTADO v3.9.1 |
| Reproducción de fondo con controles y carátula | ✅ IMPLEMENTADO v3.7 |
| Botón de chat flotante y arrastrable | ✅ IMPLEMENTADO v3.7 |
| Puente de comandos bot ↔ reproductor | ✅ IMPLEMENTADO v3.7 |
| IA Local DULCE-MIND (Gemma 2B on-device) | ✅ IMPLEMENTADO v3.6 |
| Descarga automática del modelo por Wi-Fi | ✅ IMPLEMENTADO v3.6 |

## 🧠 DULCE-MIND — IA Local

**Motor**: Google MediaPipe `tasks-genai` con Gemma 2B Instruct (cuantizado 4 bits)  
**Tamaño del modelo**: ~1.4 GB (descarga única por Wi-Fi)  
**Capacidades**:
- Recomendaciones musicales inteligentes en español
- Información sobre artistas y géneros
- Conversación contextual (recuerda el historial del chat y el tema que está sonando)
- Control total del reproductor mediante comandos hablados o escritos (*"pon salsa"*, *"agrega a favoritos"*, *"siguiente"*, *"mezclar"*, etc.)
- Funciona 100% sin internet una vez descargado
- System prompt configurado como asistente musical colombiano

## 🛠️ Detalles Técnicos
- **Lenguaje**: Kotlin + Jetpack Compose
- **Reproductor**: Media3 / ExoPlayer
- **IA Local**: Google MediaPipe `com.google.mediapipe:tasks-genai:0.10.27` + Gemma 2B Instruct
- **Extracción de streams**: Invidious API v1 JSON (6 instancias con fallback de proxy) + Fallback directo YouTubei (Android VR / Android TestSuite)
- **Validación de red**: HEAD / GET Range (1KB) antes de ExoPlayer
- **Persistencia**: Local JSON de alta velocidad y Room Database (IPTV/Historial/Perfiles)
- **Arquitectura**: MVVM con Shared ViewModel único

## 🚀 Despliegue
La APK debug se genera en:  
`app/build/outputs/apk/debug/app-debug.apk`

---
*Elevando la música al Metaverso. DulcePlay V3.9.1: IA Real, Sin Internet, Sin Límites. 🇨🇴🎧🧠✨*
