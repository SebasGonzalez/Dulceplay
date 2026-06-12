# DulcePlay V3.6.0 (DULCE-MIND — IA Local On-Device) 🚀🎥🔊🧠

DulcePlay es la cúspide de la ingeniería multimedia para Android. Una entidad digital diseñada para ofrecer una fidelidad de audio impecable, reproducción de video en Full HD y una inteligencia artificial real que funciona **sin internet**.

## 🌟 Características Destacadas

- 🧠 **DULCE-MIND (IA Local Real)**: Motor de inteligencia artificial on-device usando **Gemma 2B Instruct** via Google MediaPipe. Se descarga automáticamente por Wi-Fi (~1.4GB, solo una vez). Una vez descargado, responde preguntas de música, recomienda canciones y conversa inteligentemente **sin necesitar internet**.
- 🔊 **Motor ExoPlayer (Media3)**: Reproducción profesional sin pausas. Soporte nativo para flujos de alta velocidad y gestión inteligente de buffer.
- 🎥 **Calidad a tu Medida**: Elige entre **Audio Opus 160kbps**, **Audio AAC 128kbps**, **Video 720p HD** y **Video 360p** — extraídos en tiempo real.
- 🌐 **Motor Invidious API v1**: Extracción de streams reales usando la API JSON oficial de Invidious (`/api/v1/videos/{id}`), con fallback automático entre 4 instancias públicas.
- 🌐 **Búsqueda Universal YouTube**: Integración total con YouTube Data API v3.
- 🇨🇴 **Identidad Nacional**: Secciones dedicadas a los éxitos de **Colombia** y **México**.
- 🎨 **Estética Glass-Neon**: Interfaz ultramoderna basada en Glassmorphism con fondos de plasma dinámicos.
- 📻 **IPTV Satelital**: Sintoniza canales de TV y emisoras de radio globales.

## 🛠️ Estado Actual del Desarrollo

| Función | Estado |
|---------|--------|
| Búsqueda YouTube | ✅ FUNCIONA |
| Interfaz Cyber-Neon | ✅ FUNCIONA |
| Extracción de streams (Invidious API) | ✅ CORREGIDO v3.5 |
| Menú de calidad (nombres reales) | ✅ CORREGIDO v3.5 |
| Reproducción ExoPlayer | ✅ CORREGIDO v3.5 |
| IA Local DULCE-MIND (Gemma 2B on-device) | ✅ IMPLEMENTADO v3.6 |
| Descarga automática del modelo por Wi-Fi | ✅ IMPLEMENTADO v3.6 |
| Fallback a reglas cuando IA no disponible | ✅ IMPLEMENTADO v3.6 |

## 🧠 DULCE-MIND — IA Local

**Motor**: Google MediaPipe `tasks-genai` con Gemma 2B Instruct (cuantizado 4 bits)  
**Tamaño del modelo**: ~1.4 GB (descarga única por Wi-Fi)  
**Capacidades**:
- Recomendaciones musicales inteligentes en español
- Información sobre artistas y géneros
- Conversación contextual (recuerda el historial del chat)
- Funciona 100% sin internet una vez descargado
- System prompt configurado como asistente musical colombiano

**Flujo de activación**:
1. Al abrir el chat, detecta si el modelo está descargado
2. Si no → descarga automática solo por Wi-Fi con barra de progreso
3. Si hay datos móviles → modo básico con aviso al usuario
4. Una vez listo → IA real responde en lugar del sistema de reglas

## 🛠️ Detalles Técnicos
- **Lenguaje**: Kotlin + Jetpack Compose
- **Reproductor**: Media3 / ExoPlayer
- **IA Local**: Google MediaPipe `com.google.mediapipe:tasks-genai:0.10.27` + Gemma 2B Instruct
- **Extracción de streams**: Invidious API v1 JSON (4 instancias con fallback)
- **Arquitectura**: Clean Architecture (MVVM) con Room para persistencia local
- **Tareas en segundo plano**: WorkManager (descarga del modelo)

## 🚀 Despliegue
La APK debug se genera en:  
`app/build/outputs/apk/debug/app-debug.apk`

---
*Elevando la música al Metaverso. DulcePlay V3.6: IA Real, Sin Internet, Sin Límites. 🇨🇴🧠✨*
