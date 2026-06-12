# DulcePlay V3.5.0 (Cyber Neon - Invidious API Engine) 🚀🎥🔊💎

DulcePlay es la cúspide de la ingeniería multimedia para Android. Una entidad digital diseñada para ofrecer una fidelidad de audio impecable, reproducción de video en Full HD y una inteligencia artificial que realmente te entiende.

## 🌟 Características Destacadas

- 🗣️ **Asistente Dulce (DULCE-MIND)**: IA conversacional integrada (Google Vertex Gemini 1.5 Flash) que controla tu música, brillo, volumen y entiende tus emociones.
- 🔊 **Motor ExoPlayer (Media3)**: Reproducción profesional sin pausas. Soporte nativo para flujos de alta velocidad y gestión inteligente de buffer.
- 🎥 **Calidad a tu Medida**: Elige entre **Audio Opus 160kbps**, **Audio AAC 128kbps**, **Video 720p HD** y **Video 360p** — extraídos en tiempo real.
- 🌐 **Motor Invidious API v1**: Extracción de streams reales usando la API JSON oficial de Invidious (`/api/v1/videos/{id}`), con fallback automático entre 4 instancias públicas. URLs válidas y firmadas que ExoPlayer reproduce sin problemas.
- 🌐 **Búsqueda Universal YouTube**: Integración total con YouTube Data API v3 para encontrar cualquier canción o video oficial instantáneamente.
- 🇨🇴 **Identidad Nacional**: Secciones dedicadas a los éxitos de **Colombia** y **México**, priorizando el folklore y el orgullo regional.
- 🎨 **Estética Glass-Neon**: Interfaz ultramoderna basada en Glassmorphism con fondos de plasma dinámicos.
- 📻 **IPTV Satelital**: Sintoniza canales de TV y emisoras de radio globales con carga instantánea.

## 🛠️ Estado Actual del Desarrollo

| Función | Estado |
|---------|--------|
| Búsqueda YouTube | ✅ FUNCIONA |
| Interfaz Cyber-Neon | ✅ FUNCIONA |
| Extracción de streams (Invidious API) | ✅ CORREGIDO v3.5 |
| Menú de calidad (nombres reales) | ✅ CORREGIDO v3.5 |
| Reproducción ExoPlayer | ✅ CORREGIDO v3.5 |
| IA Local (Llama 3 8B) | 🟡 Pendiente pruebas |

## 🛠️ Detalles Técnicos
- **Lenguaje**: Kotlin + Jetpack Compose
- **Reproductor**: Media3 / ExoPlayer
- **Extracción de streams**: Invidious API v1 JSON (4 instancias con fallback)
- **Arquitectura**: Clean Architecture (MVVM) con Room para persistencia local
- **Seguridad**: Autenticación Firebase e historial cifrado
- **Optimización**: Búsqueda diferida y carga de enlaces On-Demand

## 🚀 Despliegue
La APK debug se genera en:  
`app/build/outputs/apk/debug/app-debug.apk`

---
*Elevando la música al Metaverso. DulcePlay V3.5: Potencia, Estilo y Calidad. 🇨🇴💎✨*
