package com.dulce.play.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dulce.play.ui.player.PlayerViewModel
import com.dulce.play.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: PlayerViewModel,
    onAuthSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Direct Login for testing / stability if session is broken
    val currentAccount by viewModel.currentAccount.collectAsState()
    LaunchedEffect(currentAccount) {
        if (currentAccount?.isLogged == true) {
            onAuthSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ElegantBlack), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Lock, null, tint = PremiumGold, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isRegistering) "Crea tu Cuenta" else "Bienvenido de Nuevo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Ingresa al Metaverso de DulcePlay",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isRegistering) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre Completo") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, focusedLabelColor = ElectricBlue, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, focusedLabelColor = ElectricBlue, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, focusedLabelColor = ElectricBlue, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    if (isRegistering) {
                        viewModel.register(email, password, displayName) { success, msg ->
                            isLoading = false
                            if (success) onAuthSuccess() else errorMessage = msg
                        }
                    } else {
                        viewModel.login(email, password) { success, msg ->
                            isLoading = false
                            if (success) onAuthSuccess() else errorMessage = msg
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(if (isRegistering) "REGISTRARSE" else "INICIAR SESIÓN", fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = { isRegistering = !isRegistering }) {
                Text(
                    if (isRegistering) "¿Ya tienes cuenta? Entra aquí" else "¿No tienes cuenta? Regístrate",
                    color = PremiumGold
                )
            }
            
            // Bypass button for testing / Immediate entry
            TextButton(onClick = { 
                viewModel.registerOrLoginOAuth("usuario@dulceplay.com", "Usuario", "Guest") { onAuthSuccess() }
            }) {
                Text("Entrar como Invitado (Acceso Rápido)", color = ElectricBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
