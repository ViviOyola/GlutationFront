package com.example.glutationapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glutationapp.R
import com.example.glutationapp.model.UserData
import com.example.glutationapp.model.UserSession
import com.example.glutationapp.ui.theme.GlutationAppTheme
import android.util.Patterns
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.glutationapp.network.UserApiService
import com.example.glutationapp.network.ApiClient
import com.example.glutationapp.util.AuthInputValidator
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onRegisterClick: () -> Unit, onLoginSuccess: () -> Unit) {
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            userApiService = ApiClient.userApiService,
            validator = AuthInputValidator()
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()

    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var contrasenaVisible by rememberSaveable { mutableStateOf(false) }

    val correoError = validationErrors["correo"] ?: ""
    val contrasenaError = validationErrors["contrasena"] ?: ""
    var loginError by remember { mutableStateOf("") }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var emailForRecovery by rememberSaveable { mutableStateOf("") }
    var recoveryEmailError by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                UserSession.login(state.userData)
                loginError = ""
                onLoginSuccess()
                viewModel.resetState()
            }
            is AuthUiState.Error -> {
                loginError = state.message
            }
            is AuthUiState.Loading -> {
                loginError = ""
            }
            is AuthUiState.Idle -> {
                loginError = ""
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetState()
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false 
                emailForRecovery = ""
                recoveryEmailError = ""
            },
            title = { Text("Recuperar Contraseña") },
            text = {
                Column {
                    Text("Ingresa tu correo electrónico registrado para enviarte un enlace de recuperación.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = emailForRecovery,
                        onValueChange = { emailForRecovery = it },
                        label = { Text("Correo Electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = recoveryEmailError.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recoveryEmailError.isNotEmpty()) {
                        Text(recoveryEmailError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        recoveryEmailError = ""
                        if (emailForRecovery.isBlank()) {
                            recoveryEmailError = "El correo es obligatorio."
                        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailForRecovery).matches()) {
                            recoveryEmailError = "Correo electrónico no válido."
                        } else {
                            android.util.Log.d("AuthScreen", "Simulando envío de correo de recuperación a: $emailForRecovery")
                            Toast.makeText(context, "Enlace de recuperación enviado a $emailForRecovery (simulación)", Toast.LENGTH_LONG).show()
                            showForgotPasswordDialog = false
                            emailForRecovery = ""
                        }
                    }
                ) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showForgotPasswordDialog = false 
                        emailForRecovery = ""
                        recoveryEmailError = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "\"Bienvenido a Glutatión Alemán.\"",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Text(
            text = "Compra productos saludables fácilmente",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.auth_glutation_img_1),
                contentDescription = "Promotional image with logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Dot(isActive = true)
                Dot(isActive = false)
                Dot(isActive = false)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Gray,
                unfocusedIndicatorColor = Color.LightGray
            ),
            isError = correoError.isNotEmpty()
        )
        val correoHelper = if (correoError.isNotEmpty()) correoError else "Por favor, ingresa un correo electrónico válido."
        Text(
            text = correoHelper,
            fontSize = 12.sp,
            color = if (correoError.isNotEmpty()) Color.Red else Color.Gray,
            modifier = Modifier.align(Alignment.Start).padding(start = 24.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (contrasenaVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff
                val description = if (contrasenaVisible) "Hide password" else "Show password"
                IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                    Icon(imageVector = image, description)
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Gray,
                unfocusedIndicatorColor = Color.LightGray
            ),
            isError = contrasenaError.isNotEmpty()
        )
        val contrasenaHelper = if (contrasenaError.isNotEmpty()) contrasenaError else "La contraseña debe tener al menos 8 caracteres."
        Text(
            text = contrasenaHelper,
            fontSize = 12.sp,
            color = if (contrasenaError.isNotEmpty()) Color.Red else Color.Gray,
            modifier = Modifier.align(Alignment.Start).padding(start = 24.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { 
                emailForRecovery = ""
                recoveryEmailError = ""
                showForgotPasswordDialog = true
            },
            modifier = Modifier.align(Alignment.End).padding(end = 16.dp)
        ) {
            Text("¿Olvidaste tu contraseña?", color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loginError.isNotEmpty()) {
            Text(
                text = loginError,
                color = if (loginError.contains("exitoso")) Color(0xFF4CAF50) else Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.login(correo, contrasena)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2C),
                contentColor = Color.White
            )
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Iniciar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRegisterClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2C),
                contentColor = Color.White
            )
        ) {
            Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun Dot(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.DarkGray else Color.LightGray)
    )
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun AuthScreenPreview() {
    GlutationAppTheme {
        AuthScreen(onRegisterClick = {}, onLoginSuccess = {})
    }
}

