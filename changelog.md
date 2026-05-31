# Bitácora de Desarrollo - Dulce Player (Cyber Neon Edition)

Este archivo sirve como registro técnico pormenorizado de las etapas del proyecto y el estado actual de la desarrollo. El reproductor está diseñado bajo patrones de **Clean Architecture**, **MVVM (Model-View-ViewModel)** y **Material Design 3**, presentando una estética visual inmersiva de tipo **Glassmorphism / Cyber Neon**.

---

## 🛠️ ETAPA 1: Núcleo de Reproducción Multimedia (Completada)
*   **Decodificador Multi-formato de Audio/Video**: Implementación de reproducción de alta fidelidad basada en **Android Media3 (ExoPlayer)** enriquecido con soporte integrado para flujos de transmisión adaptativos.
    *   *Formatos de audio validados*: FLAC, MP3, WAV, AAC, Opus, M4A.
    *   *Formatos de video validados*: MP4, MKV, AVI, MOV, WMV.
*   **Controles de Navegación del Flujo**: Playback reactivo con soporte de deslizamiento continuo para buscar segundos (*Seeking*), saltos entre temas previas/siguientes, ciclo de repetición y modo aleatorio (*Shuffle*).
*   **Visualizador Espectral Fluido**: Animación dinámica simulada sobre la frecuencia Hertziana a través de 32 barras de altura flotante reactiva para el reproductor musical completo.

---

## 📡 ETAPA 2: Módulo de IPTV Avanzado (Completada)
*   **Parser de Listas M3U / M3U8**: Analizador de metadatos integrado capaz de extraer etiquetas `#EXTINF`, agrupar canales por `group-title`, asociar logotipos con `tvg-logo` o `logo` y resolver localización geográfica con `tvg-country`.
*   **Métodos Diversificados de Importación**:
    1.  *Texto Plano Crudo*: Panel directo de pegado textual enriquecido con un analizador secuencial asíncrono.
    2.  *URL Directa*: Extractor remoto que descarga archivos M3U mediante canales HTTP de red.
    3.  *Selector del Almacenamiento*: Canalización nativa del sistema mediante `ActivityResultContracts.OpenDocument`.
*   **Cliente Xtream Codes API**: Conexión al protocolo Xtream Codes que realiza llamadas autentificadas de categorías (`get_live_categories`) y descodificado de flujos (`get_live_streams`) para construir listas estructuradas con los endpoints `.ts`.

---

## 💾 ETAPA 3: Persistencia de Datos y "Mi Biblioteca" (Completada)
*   **Activación y Conexión de Room Database**: Creación y validación del almacenamiento local relacional persistente SQLite gestionado mediante un único `Room.databaseBuilder` integrado con la lógica del `PlayerViewModel`.
    *   *Esquema "iptv_playlists" & "iptv_channels"*: Guarda de manera persistente las listas IPTV importadas por el usuario por medio de listas locales e Xtream Codes, haciendo que los canales persistan e inicien reactivamente al reabrir la app.
    *   *Esquema "playback_history"*: Almacena cada tema/canal reproducido de forma cronológica (limitando los últimos 50 elementos de manera automática).
    *   *Esquema "user_playlists" & "user_playlist_items"*: Soporte para que los usuarios creen colecciones virtuales personalizadas, añadan canciones/videos de su biblioteca e independientemente quiten temas de sus listas.
    *   *Esquema "app_settings"*: Almacén básico de claves-valor de preferencias.
*   **Desarrollo de "LibraryScreen.kt" (Mi Biblioteca)**:
    *   *Estilo Premium*: Glassmorphism completo, gradientes neon, bordes con brillo y animaciones est stagger.
    *   *Escaner de Almacenamiento Local (ContentResolver)*: Escaneo automatizado asíncrono del dispositivo móvil mediante APIs nativas de Android de audio/video.
    *   *Manejo de Permisos Inteligentes Android 13+*: Solicitud reactiva de permisos `READ_MEDIA_AUDIO`/`READ_MEDIA_VIDEO` en Android 13+, y del permiso heredado `READ_EXTERNAL_STORAGE` en versiones anteriores de SO.
    *   *Fallback Elegante*: En caso de no existir ficheros directos de medios, se provee de transmisiones en la nube integradas en tiempo real para mantener la pantalla reactiva y animada.
    *   *Gestores de Reproducción*: Al pulsar sobre cualquier biblioteca se activa el inicio del ExoPlayer y redirecciona en secuencia directa a la pantalla de reproducción.

---

## 🔐 ETAPA 4: Sistema de Autenticación y Gestión de Perfiles de Usuario (Versión Definitiva)
*   **Separación Absoluta (Cuenta vs Perfiles)**:
    *   *Cuenta de Usuario (Cloud Accounts)*: Es la sesión principal autenticada de manera segura con proveedores en la nube (Google, Microsoft, Discord, GitHub) o mediante registro de credenciales por correo electrónico.
    *   *Soporte Multi-Perfil (Local Profiles)*: Una vez dentro de la Cuenta de Usuario, se despliega una interfaz de selección de perfiles locales independientes (tipo Netflix/Spotify) para que familiares compartan el dispositivo, guardando playlists, historiales y progresos blindados de manera privada por perfil (`profileId` indexado).
*   **Integración Funcional de Autenticación Cloud (SSO OAuth 2.0 Real)**:
    *   *Google Identity API*: Flujo OAuth 2 de Google que abre la pantalla de consentimiento segura, captura el código de retorno en el WebView inteligente de la app, e intercambia y descarga el perfil mediante REST endpoints hacia la DB.
    *   *Microsoft Accounts (Outlook)*: Integración OAuth 2.0 funcional completa con el endpoint Active Directory común, solicitando accesos y recuperando identificadores y correos del usuario.
    *   *Discord OAuth 2.0*: Redirect interactiva mediante la API oficial Discord v10, autenticando avatars y nombres virtuales del usuario.
    *   *GitHub OAuth 2.0*: Intercambio real asíncrono con el servicio oficial de `github.com/login/oauth/access_token` asistido del cliente de red OkHttp.
*   **Consola Dinámica de Desarrollo e Interactive Sandbox**:
    *   Para optimizar la experiencia, si las llaves de producción (`BuildConfig.GOOGLE_CLIENT_ID`, etc.) están ausentes o conservan sus valores temporales placeholders en `.env.example`, la aplicación despliega un elegante panel interactive de configuración de llaves para pruebas en tiempo real, junto a una suite de Sandbox oficial interactivo de simulación que emula los portales de autorización oficiales, resolviendo el flujo de manera impecable y segura sin pantallas rotas ni errores de API.
*   **Seguridad de Credenciales Tradicionales y Verificación OTP**:
    *   *Formulario de Registro y OTP*: Registro con validaciones avanzadas que genera y envía un código holográfico seguro de 4 dígitos renderizado en una tarjeta de notificación flotante, exigiendo coincidencia exacta para guardar la cuenta.
    *   *Seguridad Criptográfica*: Las contraseñas tradicionales se encriptan con algoritmos de hashing unidireccional SHA-256 antes de guardarse en Room.
    *   *Recuperación Holográfica*: Flujo completo de restablecimiento de contraseña mediante pins de un solo uso con reescritura directa y encriptada sobre la base de datos local SQLite.
*   **Sesión Persistente y Limpieza de Fugas**:
    *   La sesión activa permanece guardada indefinidamente a través del Room User Account de manera cohesionada. Al dar click a "Cerrar Sesión", la base de datos limpia de inmediato los registros de token, detiene los Jobs de fondo de corrutinas previniendo fugas de memoria y ANRs de tipo `InputDispatcher`, y redirige herméticamente al Login.


---

## 🤖 ETAPA 6: Asistente Inteligente "DULCE-BOT" (Completada)
*   **Cerebro Analítico Local (On-Device Cognitive Brain)**:
    *   *Análisis de Comportamiento*: Algoritmo local que lee el historial persistente de Room (`playback_history`) para determinar los géneros más escuchados, artistas predilectos y patrones horarios (por ejemplo, sugiriendo frecuencias relajantes en el huso nocturno).
    *   *Sugerencia Inteligente Dinámica*: Enlace directo a recomendaciones de contenido que, de forma contextual a la hora o a la última reproducción guardada por perfil, elabora y ofrece un ítem adaptativo con su propio botón integrado de reproducción.
*   **Procesador de Lenguaje Natural en Español (NLP Local)**:
    *   *Motor de Reglas Semánticas*: Capaz de analizar, categorizar y derivar intenciones complejas a partir de texto o dictado de voz en español de forma 100% offline (sin dependencias ni fugas de datos).
    *   *Comandos Integrados Soportados*:
        *   🗣️ *"Estudiar"* / *"Concentrarse"*: Autoplay de flujos instrumentales/chill, baja volumen gradualmente, activa el modo de estudio sin picos acústicos abruptos.
        *   🗣️ *"Noticias de [País]"*: Busca en listas de reproducción IPTV los canales de televisión locales asociados a noticias de Colombia, México, España, etc., y los reproduce inmediatamente.
        *   🗣️ *"¿Qué puedo ver hoy?"* / *"Recomiéndame algo"*: Analiza los metadatos de reproducción del perfil activo y proporciona feeds contextuales.
        *   🗣️ *"Modo Noche"*: Atenúa el brillo de la pantalla, arranca sintonías delta de descanso y activa el temporizador inteligente de apagado por fade-out sonoro a los 30 minutos.
        *   🗣️ *"Busca música de artistas locales"*: Promueve la herencia cultural regional buscando folclor autóctono (como Cumbia y Bossa-Nova).
        *   🗣️ Modos adicionales como *"Modo Fácil"*, *"Modo Conducción"*, *"Modo Familiar"* y *"Pausar/Reanudar"*.
*   **Modos de Uso Asistidos e Inclusivos (Accesibilidad y Bienestar)**:
    *   *Modo Fácil / Accesible*: Interfaz adaptativa diseñada minuciosamente para adultos mayores o personas con visión reducida. Agranda los textos a proporciones masivas, despliega botones gigantes de toque táctil seguro de 76dp, limpia barras de pestañas estándar por un menú de acceso secuencial simplificado y expone un gatillo de voz masivo y legible.
    *   *Modo Conducción*: Entorno de manejo simplificado libre de distracciones cinéticas que promueve ritmos energéticos de Synthwave estables de forma dictada.
    *   *Modo Familiar / Filtro Infantil*: Purga los canales IPTV dinámicos o películas de ciencia ficción oscuras, dejando únicamente contenido educativo, cultural tradicional o apto para menores.
    *   *Modo Bienestar / Meditación*: Despliega guías interactivas de respiración rítmica consciente sincronizada a ondulaciones cinéticas y ambient relajante de fondo.
*   **Interfaz Holística de Impacto Cyberpunk**:
    *   *Esfera Holográfica AI (Pulsing Orb)*: Botón flotante omnipresente visible en todas las pantallas. Posee animaciones interactivas basadas en estados de conversación en tiempo real (IDLE, LISTENING, THINKING, SPEAKING) con gradientes cíclicos y ondas neón.
    *   *Chat Bubble Dialog*: Bandeja de mensajería con estilo glassmorphic, transparencias fluidas, tarjetas de acción rápida, shortcuts inteligentes e integración total para controlar el ExoPlayer principal.

---

## 🔊 ETAPA 5: Ecualizador Gráfico de Audio y Utilidades de Copia de Seguridad (Completada)
*   **Ecualizado Fino por Hardware y Software**: Integración de un ecualizador gráfico interactivo de 5 bandas que se enlaza de forma nativa a la sesión de hardware de ExoPlayer (`audioSessionId`), permitiendo potenciar bajos (*Bass Boost*), aplicar efectos envolventes (*Virtualizer*) o elegir entre múltiples preajustes tradicionales (Pop, Rock, Classic, Jazz, Vocal).
*   **Copia de Seguridad y Sincronización JSON Offline**: Herramienta portable de copia de seguridad con protección de privacidad. Compacta playlists, configuraciones globales y elecciones estéticas del perfil activo en una cadena JSON cifrada compacta que se copia fácilmente al portapapeles o se exporta a almacenamiento para restablecer la fisionomía de la app en cualquier nuevo terminal de forma instantánea.

---

## 🎨 ETAPA 7: UI Adaptativa con Glassmorphism y Pantalla de Ajustes Definitiva (Completada)
*   **Pantalla de Ajustes Avanzada**: Creación de `/app/src/main/java/com/example/ui/settings/SettingsScreen.kt` bajo la firma visual de **Glassmorphism / Cyber Neon** que reúne de forma integrada las opciones de ecualizador, gestores de Chromecast, copia de seguridad, preferencias del asistente Dulce-Bot, PIN de seguridad de perfiles y gestores del almacenamiento local.
*   **Esquema de Temas Visuales Dinámicos**: Configuración de 4 paletas estéticas completas que reescriben los estilos CSS/Material 3 de la aplicación instantáneamente desde el reproductor:
    1.  🎨 **Cyber Neon (Luz de Neo-Tokio)**: Rosa eléctrico, cian cibernético y contrastes oscuros profundos.
    2.  🎨 **Classic Dark (Noche Minimalista)**: Escalas de grises puras, tonos mate suaves y luz blanca tenue.
    3.  🎨 **Electric Blue (Furia Eléctrica)**: Azul cobalto intenso, acentos cerúleos y fondos de plasma energéticos.
    4.  🎨 **Nature Green (Bosque Real)**: Verde esmeralda vivo, acentos oliva y degradados orgánicos relajantes.
*   **Doble Capa de Seguridad (PIN por Perfil)**: Blindaje de perfiles locales independientes para asegurar que cada miembro de la casa controle de manera privada e inviolable sus playlists y su historial, protegiendo a los menores de accesos indebidos sin comprometer la facilidad de uso.
*   **Optimización Adaptativa en Tableta o Paisajes**: Lógica responsiva automática en todas las pantallas principales (Biblioteca, IPTV, Explorador, Configuración) que reorganiza los paneles en una matriz inteligente de dos columnas siempre que el ancho disponible sea superior a `650.dp`, logrando una experiencia de uso ideal en tabletas, Chromebooks y terminales multipantalla plegables.
*   **Buscador Universal Inteligente "DULCE-SEARCH"**: Barra de búsqueda accesible desde la parte superior de cada pantalla que unifica:
    *   *Búsqueda Local Exhaustiva*: Filtra en milisegundos tus canciones de la biblioteca, videos de ciencia ficción, canales IPTV importados por nombre/grupo/país, tus carpetas de playlists físicas de Room e incluso tu historial reciente de reproducción.
    *   *Búsqueda Segura en Línea en Tiempo Real*: Si un término no existe en tu celular, el buscador llama a la pasarela de iTunes para encontrar canciones con autor, título y carátula cargados dinámicamente con Coil. Permite streaming directo con un solo clic.
    *   *Algoritmo de Corrección Ortográfica (Levenshtein Distance)*: Deducción predictiva que propone alternativas ante deslices de tipeo (ej. ="¿Quizás quisiste decir Salsa?"= al tipear ="Slasa"=).
    *   *Historial de Consultas Recientes*: Persistencia de tus búsquedas frecuentes para cargarlas de forma instantánea mediante burbujas interactivas neón.
    *   *Integración con el Asistente*: Dulce-Bot atiende comandos semánticos como *"busca reguetón"* o *"buscar jazz"* abriendo inmediatamente el panel centralizado de resultados.

---

## 📖 GUÍA DE USO PARA EL USUARIO (Manual Oficial DulcePlay)

### 🤖 1. Cómo interactuar con el Asistente Inteligente "DULCE-BOT"
Para comunicarte con la inteligencia de Dulce-Bot y automatizar tu reproducción, localiza la **Esfera Holográfica AI Flotante (Pulsing Orb)** presente en la esquina de la pantalla.
*   **Activación por Voz o Texto**: Toca la esfera neón y di en voz alta o escribe comandos sencillos en español como *"pon música para estudiar"*, *"modo noche"* o *"recomiéndame televisión de noticias"*.
*   **Sugerencias Cognitivas**: Dulce-Bot aprende tus géneros y horarios favoritos en segundo plano sin compartir datos en la nube. A través del tiempo, verás aparecer recomendaciones especialmente seleccionadas para ti en su bandeja de entrada.

### 👥 2. Configurando Perfiles Independientes y Seguridad
DulcePlay permite compartir el mismo dispositivo con familiares sin que se mezclen tus gustos y playlists:
1.  Inicia sesión en tu cuenta principal en la nube.
2.  Crea o selecciona tu perfil personal desde el panel principal.
3.  Si deseas blindar tu historial y playlists de miradas ajenas, ve a la pantalla de **Ajustes**, localiza la sección de **Seguridad del Perfil** y activa la opción **PONER PIN**. Tus datos estarán protegidos bajo una contraseña interna integrada en la base de datos cifrada Room.

### 🔊 3. Optimización del Sonido con el Ecualizador de 5 Bandas
Para sacar el máximo provecho de tus audífonos o parlantes:
1.  Ve a la pantalla de **Ajustes** y desplázate hasta la sección **Ecualizador Gráfico**.
2.  Activa el ecualizador y experimenta con preajustes diseñados por expertos (como *Bass Boost* para mayor pegada de bajos o *Vocal* para estudiar documentales), o desliza manualmente las 5 bandas hertzianas individuales para modelar las frecuencias a tu gusto.

### 📥 4. Importando Canales de TV por IPTV
Puedes ver canales de televisión abiertos, eventos culturales o listas remotas m3u en un par de segundos:
1.  Navega a la sección **IPTV Sat**.
2.  Pulsa el botón flotante de **Importar Lista**.
3.  Elige tu método favorito: copia y pega una lista en formato texto crudo, introduce una dirección web URL remota o carga un archivo descargado en tu celular a través del selector nativo de archivos del sistema.
4.  ¡Listo! Los canales se organizarán automáticamente por categorías y persistirán seguros en la base de datos Room para que estén disponibles cada vez que abras la aplicación.

### 🔎 5. El Buscador Universal "DULCE-SEARCH"
Localiza material local o satelital al instante:
1.  Toca la **burbuja de búsqueda con marco de cristal neón** en la parte superior de cualquier pantalla principal.
2.  Introduce las palabras clave; a partir del segundo carácter, se clasificarán tus pertenencias en el celular (*Canciones, Videos, Historial, IPTV o Listas*), separadas de los hallazgos en internet.
3.  Si cometes un error ortográfico, lee la alerta que sugiere la palabra correcta y tócala para actualizar la consulta.
4.  Pulsa el botón circular de **Play** al lado de cualquier resultado para iniciar su reproducción inmediata sin rodeos. El asistente ejecutará también comandos directos si le dices *"busca salsa clásica"*.

### 💡 6. Inclusividad, Accesibilidad y Bienestar Integral
Creemos firmemente en el poder de la tecnología para mejorar la calidad de vida de todos los seres humanos. DulcePlay se enorgullece de integrar herramientas específicas para el bienestar diario:
*   👵 **Modo Adulto Mayor (Modo Fácil / Accesible)**: Al activar el modo fácil, las interfaces complejas se limpian. Los textos se agrandan masivamente y se despliegan accesos rápidos gigantes con un touch target masivo de 76dp. Ideal para personas con visión disminuida o que prefieren una navegación clara y directa.
*   🧘 **Modo Bienestar y Respiración Consciente**: ¿Te sientes abrumado? Pídele a Dulce-Bot un momento de calma diciendo *"inicia ejercicio de respiración"*. El asistente desplegará guías de meditación interactivas y ritmos de respiración visuales sincronizados a audio relajante diseñados para disminuir el estrés y la ansiedad cotidiana.
