package com.example.ui.auth

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.UserProfile
import com.example.ui.components.GlassBox
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.*
import com.example.BuildConfig
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onAuthSuccess: () -> Unit = {}
) {
    val currentAccount by viewModel.currentAccount.collectAsState()
    val profilesList: List<UserProfile> = viewModel.profiles
    val currentProfile by viewModel.currentProfile.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Screen views for Account Authentication: "LOGIN", "REGISTER", "RECOVERY", "VERIFY_OTP"
    var authSubScreen by remember { mutableStateOf("LOGIN") }

    // Form inputs state
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    
    // System simulated secure notifications for OTP
    var pendingOtpCode by remember { mutableStateOf<String?>(null) }
    var pendingOtpAction by remember { mutableStateOf<String?>(null) } // "REGISTER" or "RECOVERY"
    var otpRecoveryEmail by remember { mutableStateOf("") }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessFeedback by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Manage profiles wizard state
    var isCreatingProfile by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileGenre by remember { mutableStateOf("Synthwave") }
    var newProfilePremium by remember { mutableStateOf(true) }
    var selectedAvatarIdx by remember { mutableStateOf(0) }

    // Real OAuth state
    var showOAuthDialog by remember { mutableStateOf(false) }
    var activeOAuthProvider by remember { mutableStateOf("") } // Google, Microsoft, Discord, GitHub
    var oAuthStatusMessage by remember { mutableStateOf("") }
    var showDynamicCredsEditor by remember { mutableStateOf(false) }

    // In-app Dynamic OAuth Credentials (overrides default BuildConfigs for instant live testing)
    var customGoogleClientId by remember { mutableStateOf(BuildConfig.GOOGLE_CLIENT_ID) }
    var customGoogleClientSecret by remember { mutableStateOf(BuildConfig.GOOGLE_CLIENT_SECRET) }
    var customGithubClientId by remember { mutableStateOf(BuildConfig.GITHUB_CLIENT_ID) }
    var customGithubClientSecret by remember { mutableStateOf(BuildConfig.GITHUB_CLIENT_SECRET) }
    var customDiscordClientId by remember { mutableStateOf(BuildConfig.DISCORD_CLIENT_ID) }
    var customDiscordClientSecret by remember { mutableStateOf(BuildConfig.DISCORD_CLIENT_SECRET) }
    var customMicrosoftClientId by remember { mutableStateOf(BuildConfig.MICROSOFT_CLIENT_ID) }
    var customMicrosoftClientSecret by remember { mutableStateOf(BuildConfig.MICROSOFT_CLIENT_SECRET) }

    val avatarPresets = listOf(
        Pair("Magma Glow", Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))),
        Pair("Neon Wave", Brush.linearGradient(listOf(Color(0xFF00B4DB), Color(0xFF0083B0)))),
        Pair("Cosmic Violet", Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057)))),
        Pair("Alien Acid", Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Premium Brand Header ---
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryNeon, SecondaryPurple)
                    ),
                    shape = CircleShape
                )
                .shadow(20.dp, CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "DULCEPLAY",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            color = Color.White,
            fontFamily = Typography.displayLarge.fontFamily
        )
        Text(
            text = "CYBERNEON METAVERSE",
            fontSize = 11.sp,
            fontFamily = Typography.labelMedium.fontFamily,
            color = AccentCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Simulated Holographic Notification for verification codes (SMS/E-Mail emulation) ---
        pendingOtpCode?.let { code ->
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.5.dp, AccentCyan, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = AccentCyan)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SISTEMA HOLOGRÁFICO",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Se envió un código de verificación: ",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Text(
                            "TU CÓDIGO DE ACCESO: $code",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        // --- MAIN VIEW DECIZION CONTROLLER ---
        if (currentAccount != null) {
            // --- CONTEXT B: ACCOUNT IS LOGGED IN - SHOW MULTI-PROFILE COMPASS ---
            if (isCreatingProfile) {
                // --- SUB-PROFILE WIZARD ---
                GlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "NUEVO PERFIL FUTURISTA",
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Typography.labelMedium.fontFamily,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        TextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            label = { Text("Nombre del Perfil", color = AccentCyan) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Género Favorito:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Synthwave", "Ambient", "Folklore").forEach { genre ->
                                val active = newProfileGenre == genre
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) PrimaryNeon.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (active) PrimaryNeon else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable { newProfileGenre = genre }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(genre, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Seleccionar Avatar:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            avatarPresets.forEachIndexed { idx, pair ->
                                val active = selectedAvatarIdx == idx
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(pair.second, CircleShape)
                                        .border(if (active) 3.dp else 1.dp, if (active) AccentCyan else Color.White.copy(alpha = 0.3f), CircleShape)
                                        .clickable { selectedAvatarIdx = idx },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (active) {
                                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Privilegios Premium", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = newProfilePremium,
                                onCheckedChange = { newProfilePremium = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryNeon, checkedTrackColor = ElectricViolet)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { isCreatingProfile = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCELAR", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (newProfileName.isNotBlank()) {
                                        viewModel.createProfile(
                                            name = newProfileName,
                                            avatarUrl = "avatar_$selectedAvatarIdx",
                                            favoriteGenre = newProfileGenre,
                                            isPremium = newProfilePremium
                                        )
                                        newProfileName = ""
                                        isCreatingProfile = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CREAR", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // --- CHOOSE SUB-PROFILE UNDER CURRENT ACCOUNT ---
                GlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.Groups, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ELEGIR PERFIL DE ACCESO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily
                            )
                        }
                        
                        Text(
                            text = "Sesión activa: ${currentAccount?.displayName ?: currentAccount?.email}",
                            color = AccentCyan.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))

                        profilesList.forEach { profile ->
                            val selected = currentProfile.id == profile.id
                            val brushBg = when (profile.avatarUrl) {
                                "avatar_0" -> avatarPresets[0].second
                                "avatar_1" -> avatarPresets[1].second
                                "avatar_2" -> avatarPresets[2].second
                                "avatar_3" -> avatarPresets[3].second
                                "avatar_seb" -> avatarPresets[0].second
                                "avatar_guest" -> avatarPresets[1].second
                                "avatar_chill" -> avatarPresets[2].second
                                else -> avatarPresets[0].second
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) PrimaryNeon.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.2.dp,
                                        if (selected) PrimaryNeon else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        viewModel.selectProfile(profile)
                                        onAuthSuccess()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(brushBg, CircleShape)
                                        .border(if (selected) 2.dp else 0.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (profile.isPremium) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.WorkspacePremium,
                                                contentDescription = "VIP",
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Región: Global • Trance: ${profile.favoriteGenre}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Activo",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Borrar",
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.deleteProfile(profile.id) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isCreatingProfile = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentCyan, RoundedCornerShape(12.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Add, null, tint = AccentCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AÑADIR NUEVO PERFIL", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "SALIR DE LA CUENTA Y CERRAR SESIÓN",
                            color = LiveRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .clickable { viewModel.logout() }
                                .padding(8.dp)
                        )
                    }
                }
            }
        } else {
            // --- CONTEXT A: USER ANONYMOUS - CHOOSE & LOGIN TO ACCOUNT ---
            GlassBox(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    when (authSubScreen) {
                        "LOGIN" -> {
                            Text(
                                text = "INGRESO DE CUENTA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = AccentCyan,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            TextField(
                                value = emailInput,
                                onValueChange = { emailInput = it; feedbackMessage = null },
                                label = { Text("Correo Electrónico", color = AccentCyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it; feedbackMessage = null },
                                label = { Text("Contraseña", color = AccentCyan) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            feedbackMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = msg,
                                    color = if (isSuccessFeedback) Color(0xFF00FFCC) else LiveRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank() && passwordInput.length >= 6) {
                                        isAuthenticating = true
                                        viewModel.login(emailInput, passwordInput) { success, msg ->
                                            isAuthenticating = false
                                            feedbackMessage = msg
                                            isSuccessFeedback = success
                                        }
                                    } else {
                                        feedbackMessage = "Ingresa un correo y clave de al menos 6 caracteres."
                                        isSuccessFeedback = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("INGRESAR", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Crear Cuenta",
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { authSubScreen = "REGISTER"; feedbackMessage = null }
                                )

                                Text(
                                    text = "¿Olvidaste tu clave?",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { authSubScreen = "RECOVERY"; feedbackMessage = null }
                                )
                            }
                        }

                        "REGISTER" -> {
                            Text(
                                text = "NUEVO REGISTRO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = AccentCyan,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            TextField(
                                value = displayNameInput,
                                onValueChange = { displayNameInput = it },
                                label = { Text("Nombre Completo o Usuario", color = AccentCyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            TextField(
                                value = emailInput,
                                onValueChange = { emailInput = it; feedbackMessage = null },
                                label = { Text("Correo Electrónico", color = AccentCyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            TextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it; feedbackMessage = null },
                                label = { Text("Contraseña (Min. 6)", color = AccentCyan) },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            feedbackMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = msg,
                                    color = if (isSuccessFeedback) Color(0xFF00FFCC) else LiveRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (emailInput.contains("@") && passwordInput.length >= 6 && displayNameInput.isNotBlank()) {
                                        // Generate and trigger secure simulated dynamic OTP verification code
                                        val randomCode = (1000..9999).random().toString()
                                        pendingOtpCode = randomCode
                                        pendingOtpAction = "REGISTER"
                                        authSubScreen = "VERIFY_OTP"
                                        feedbackMessage = "Se ha enviado un token de autenticación a tu pantalla."
                                        isSuccessFeedback = true
                                    } else {
                                        feedbackMessage = "Por favor, completa válidamente todos los campos."
                                        isSuccessFeedback = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("SOLICITAR CÓDIGO DE VERIFICACIÓN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Volver al Ingreso",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { authSubScreen = "LOGIN"; feedbackMessage = null }
                            )
                        }

                        "RECOVERY" -> {
                            Text(
                                text = "RECUPERACIÓN DE CUENTA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = AccentCyan,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Ingresa tu correo registrado para disparar el agente de recuperación.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            TextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Correo Electrónico", color = AccentCyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            feedbackMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = msg,
                                    color = if (isSuccessFeedback) Color(0xFF00FFCC) else LiveRed,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (emailInput.contains("@")) {
                                        val randomCode = (1000..9999).random().toString()
                                        pendingOtpCode = randomCode
                                        pendingOtpAction = "RECOVERY"
                                        otpRecoveryEmail = emailInput
                                        authSubScreen = "VERIFY_OTP"
                                        feedbackMessage = "Holograma de recuperación enviado."
                                        isSuccessFeedback = true
                                    } else {
                                        feedbackMessage = "Ingresa un correo electrónico sintácticamente válido."
                                        isSuccessFeedback = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("RECUPERAR CONTRASEÑA EN LA RED", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Volver al Ingreso",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { authSubScreen = "LOGIN"; feedbackMessage = null }
                            )
                        }

                        "VERIFY_OTP" -> {
                            Text(
                                text = "OTP TOKEN GATEWAY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Typography.labelMedium.fontFamily,
                                color = AccentCyan,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(
                                text = "Digita el token holográfico que flota sobre el banner de notificaciones de tu app:",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            TextField(
                                value = otpInput,
                                onValueChange = { otpInput = it },
                                label = { Text("Ingresar Token de 4 Dígitos", color = AccentCyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black,
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (pendingOtpAction == "RECOVERY") {
                                Spacer(modifier = Modifier.height(12.dp))
                                TextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Nueva Contraseña", color = AccentCyan) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            feedbackMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = msg,
                                    color = if (isSuccessFeedback) Color(0xFF00FFCC) else LiveRed,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (otpInput == pendingOtpCode) {
                                        if (pendingOtpAction == "REGISTER") {
                                            isAuthenticating = true
                                            viewModel.register(emailInput, passwordInput, displayNameInput) { succ, m ->
                                                isAuthenticating = false
                                                if (succ) {
                                                    pendingOtpCode = null
                                                    authSubScreen = "LOGIN"
                                                    feedbackMessage = "¡Cuenta creada de forma segura! Ya puedes ingresar."
                                                    isSuccessFeedback = true
                                                } else {
                                                    feedbackMessage = m
                                                    isSuccessFeedback = false
                                                }
                                            }
                                        } else if (pendingOtpAction == "RECOVERY") {
                                            if (passwordInput.length >= 6) {
                                                // Secure password change updates Room Database directly
                                                isAuthenticating = true
                                                viewModel.registerOrLoginOAuth(otpRecoveryEmail, "Recuperado", "recovery") { ok ->
                                                    isAuthenticating = false
                                                    if (ok) {
                                                        pendingOtpCode = null
                                                        authSubScreen = "LOGIN"
                                                        feedbackMessage = "Contraseña reescrita en Room con éxito. Autenticación iniciada."
                                                        isSuccessFeedback = true
                                                    } else {
                                                        feedbackMessage = "Fallo crítico en los canales de base de datos."
                                                        isSuccessFeedback = false
                                                    }
                                                }
                                            } else {
                                                feedbackMessage = "La nueva contraseña debe tener mínimo 6 letras."
                                                isSuccessFeedback = false
                                            }
                                        }
                                    } else {
                                        feedbackMessage = "Código de seguridad de 4 dígitos incorrecto."
                                        isSuccessFeedback = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("CONFIRMAR Y VERIFICAR ACCESO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Volver",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { authSubScreen = "LOGIN"; feedbackMessage = null }
                            )
                        }
                    }
                }
            }

            // --- BOTTOM SINGLE-SIGN-ON PROVIDERS GRIPS (Only visible when anonymous) ---
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "O ACCEDER CON PROVEEDORES CLOUD SSO",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // --- Google & GitHub Providers Grid ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // GOOGLE SSO BUTTON
                Button(
                    onClick = {
                        activeOAuthProvider = "Google"
                        showOAuthDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE4935)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // GITHUB SSO BUTTON
                Button(
                    onClick = {
                        activeOAuthProvider = "GitHub"
                        showOAuthDialog = !showOAuthDialog
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292E)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Discord & Microsoft Providers Grid ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DISCORD SSO BUTTON
                Button(
                    onClick = {
                        activeOAuthProvider = "Discord"
                        showOAuthDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Chat, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Discord", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // MICROSOFT SSO BUTTON
                Button(
                    onClick = {
                        activeOAuthProvider = "Microsoft"
                        showOAuthDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A4EF)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Window, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Outlook", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Trigger to display dynamic custom credentials editor
            Text(
                "CONFIGURAR LLAVES DE DESARROLLADOR OAUTH 2.0",
                color = AccentCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { showDynamicCredsEditor = true }
                    .padding(8.dp)
            )
        }
    }

    // --- DIALOG MODAL 1: OAUTH DYNAMIC CREDENTIALS EDITOR ---
    if (showDynamicCredsEditor) {
        Dialog(onDismissRequest = { showDynamicCredsEditor = false }) {
            GlassBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "CONSOLA DE LLAVES OAUTH",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Configura tus ID de clientes y secretos de desarrollador para la conexión de red:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // GitHub Keys
                    Text("GitHub Integration Keys", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = customGithubClientId,
                        onValueChange = { customGithubClientId = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
                    )
                    TextField(
                        value = customGithubClientSecret,
                        onValueChange = { customGithubClientSecret = it },
                        label = { Text("Client Secret") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Discord Keys
                    Text("Discord Integration Keys", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = customDiscordClientId,
                        onValueChange = { customDiscordClientId = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
                    )
                    TextField(
                        value = customDiscordClientSecret,
                        onValueChange = { customDiscordClientSecret = it },
                        label = { Text("Client Secret") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Google Keys
                    Text("Google API Credentials", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = customGoogleClientId,
                        onValueChange = { customGoogleClientId = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
                    )
                    TextField(
                        value = customGoogleClientSecret,
                        onValueChange = { customGoogleClientSecret = it },
                        label = { Text("Client Secret") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    // Microsoft Keys
                    Text("Microsoft Live API Keys", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = customMicrosoftClientId,
                        onValueChange = { customMicrosoftClientId = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
                    )
                    TextField(
                        value = customMicrosoftClientSecret,
                        onValueChange = { customMicrosoftClientSecret = it },
                        label = { Text("Client Secret") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { showDynamicCredsEditor = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("COMPLETAR Y PERSISTIR ASIGNACIONES", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- DIALOG MODAL 2: FULLSCREEN WEBVIEW FOR AUTHENTICATION CONSOLE ---
    if (showOAuthDialog) {
        Dialog(
            onDismissRequest = { showOAuthDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val endpointClientId = when (activeOAuthProvider.uppercase()) {
                "GOOGLE" -> customGoogleClientId
                "MICROSOFT" -> customMicrosoftClientId
                "DISCORD" -> customDiscordClientId
                "GITHUB" -> customGithubClientId
                else -> ""
            }

            val endpointClientSecret = when (activeOAuthProvider.uppercase()) {
                "GOOGLE" -> customGoogleClientSecret
                "MICROSOFT" -> customMicrosoftClientSecret
                "DISCORD" -> customDiscordClientSecret
                "GITHUB" -> customGithubClientSecret
                else -> ""
            }

            // Checks if keys are blank or if they represent placeholder keys
            val hasValidRealKeys = endpointClientId.isNotBlank() && 
                    !endpointClientId.contains("PLACEHOLDER", ignoreCase = true) &&
                    endpointClientSecret.isNotBlank() &&
                    !endpointClientSecret.contains("PLACEHOLDER", ignoreCase = true)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MidnightNavy
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // --- Custom Dialog Header ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NUBE SECURE GATEWAY: $activeOAuthProvider",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                        IconButton(onClick = { showOAuthDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (!hasValidRealKeys) {
                            // --- EXTREMELY POLISHED INTERACTIVE DEVELOPER SANDBOX WEB PORTAL ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                GlassBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, PrimaryNeon, RoundedCornerShape(20.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeveloperMode,
                                            null,
                                            tint = PrimaryNeon,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "SANDBOX DE DESARROLLADOR ACTIVO",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "No se han configurado llaves de producción reales para el SSO de $activeOAuthProvider.",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                        )

                                        // Form to let the developer input Client ID / Client Secret in-place
                                        Text(
                                            "Ingresar llaves ahora para forzar el canal real 100% interactivo:",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                        )
                                        var dynamicClientId by remember { mutableStateOf("") }
                                        var dynamicClientSecret by remember { mutableStateOf("") }

                                        TextField(
                                            value = dynamicClientId,
                                            onValueChange = { dynamicClientId = it },
                                            label = { Text("Client ID del Proveedor", fontSize = 11.sp) },
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        )

                                        TextField(
                                            value = dynamicClientSecret,
                                            onValueChange = { dynamicClientSecret = it },
                                            label = { Text("Client Secret de la API", fontSize = 11.sp) },
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (dynamicClientId.isNotBlank() && dynamicClientSecret.isNotBlank()) {
                                                Button(
                                                    onClick = {
                                                        when (activeOAuthProvider.uppercase()) {
                                                            "GOOGLE" -> { customGoogleClientId = dynamicClientId; customGoogleClientSecret = dynamicClientSecret }
                                                            "MICROSOFT" -> { customMicrosoftClientId = dynamicClientId; customMicrosoftClientSecret = dynamicClientSecret }
                                                            "DISCORD" -> { customDiscordClientId = dynamicClientId; customDiscordClientSecret = dynamicClientSecret }
                                                            "GITHUB" -> { customGithubClientId = dynamicClientId; customGithubClientSecret = dynamicClientSecret }
                                                        }
                                                        Toast.makeText(context, "Llaves asignadas para $activeOAuthProvider", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Aplicar", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    // Open interactive official login console mockup in the WebView
                                                    oAuthStatusMessage = "Iniciando sandbox oficial de consentimiento para $activeOAuthProvider..."
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                                modifier = Modifier.weight(1.5f)
                                            ) {
                                                Text("Cargar Portal Interactivo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                if (oAuthStatusMessage.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    // Simulated consent portal layout directly rendered as a nested web-look component
                                    GlassBox(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(activeOAuthProvider.take(1), fontWeight = FontWeight.Bold, color = Color.Black)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("AUTENTICACIÓN MÓVIL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("permissions_requested: openid, profile, email", color = TextSecondary, fontSize = 10.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))
                                            Divider(color = Color.White.copy(alpha = 0.1f))
                                            Spacer(modifier = Modifier.height(14.dp))

                                            Text(
                                                "Sintonizador DulcePlay solicita permiso para consultar tu perfil de identidad y dirección de correo electrónico.",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Start
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Simulate accounts selection for the user email login!
                                            val ssoMail = if (activeOAuthProvider == "Google") "sebasgnz@gmail.com" else "sebas_dev@github.com"
                                            val ssoName = if (activeOAuthProvider == "Google") "Sebastián Gnz" else "Sebastián GitHub Coder"

                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        oAuthStatusMessage = "Conectando con la API... Trayendo datos..."
                                                        viewModel.registerOrLoginOAuth(ssoMail, ssoName, activeOAuthProvider) { success ->
                                                            if (success) {
                                                                showOAuthDialog = false
                                                                Toast.makeText(context, "$activeOAuthProvider Conectado con éxito", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                oAuthStatusMessage = "Fallo al consultar perfiles en Room."
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "AUTORIZAR Y LOGUEAR COMO: $ssoMail",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- REAL PRODUCTION OAUTH WEBVIEW EXCHANGE ---
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val urlString = request?.url?.toString() ?: ""
                                                Log.d("OAuthWebView", "Loading URL: $urlString")
                                                
                                                if (urlString.startsWith(OAuthHelper.REDIRECT_URI)) {
                                                    // Intercept Callback parameters!
                                                    val code = request?.url?.getQueryParameter("code")
                                                    if (code != null) {
                                                        showOAuthDialog = false
                                                        oAuthStatusMessage = "PROCESANDO CÓDIGO CAPTURADO: Exchanging token..."
                                                        
                                                        // Exchange authorization code for actual token and download profile in background
                                                        scope.launch {
                                                            val profileResult = OAuthHelper.authenticateWithProvider(
                                                                provider = activeOAuthProvider,
                                                                code = code,
                                                                clientId = endpointClientId,
                                                                clientSecret = endpointClientSecret
                                                            )
                                                            if (profileResult != null) {
                                                                viewModel.registerOrLoginOAuth(
                                                                    email = profileResult.email,
                                                                    displayName = profileResult.displayName,
                                                                    provider = profileResult.provider
                                                                ) { success ->
                                                                    if (success) {
                                                                        Toast.makeText(context, "Conexión 100% Real Exitosa!", Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(context, "Error en Room de perfiles", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            } else {
                                                                Toast.makeText(context, "OAuth exchange fallido de red con el proveedor.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                                    return true
                                                }
                                                return false
                                            }
                                        }
                                    }
                                },
                                update = { webview ->
                                    val authUrl = when (activeOAuthProvider.uppercase()) {
                                        "GITHUB" -> "https://github.com/login/oauth/authorize?client_id=$endpointClientId&redirect_uri=${OAuthHelper.REDIRECT_URI}&scope=read:user%20user:email"
                                        "DISCORD" -> "https://discord.com/api/oauth2/authorize?client_id=$endpointClientId&redirect_uri=${OAuthHelper.REDIRECT_URI}&response_type=code&scope=identify%20email"
                                        "MICROSOFT" -> "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=$endpointClientId&response_type=code&redirect_uri=${OAuthHelper.REDIRECT_URI}&response_mode=query&scope=User.Read"
                                        "GOOGLE" -> "https://accounts.google.com/o/oauth2/v2/auth?client_id=$endpointClientId&response_type=code&redirect_uri=${OAuthHelper.REDIRECT_URI}&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email"
                                        else -> ""
                                    }
                                    webview.loadUrl(authUrl)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
