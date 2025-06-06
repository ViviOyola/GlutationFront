package com.example.glutationapp.ui

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glutationapp.model.UserData
import com.example.glutationapp.network.ApiClient
import com.example.glutationapp.network.RegisterUserRequest
import com.example.glutationapp.ui.theme.GlutationAppTheme
import com.example.glutationapp.util.RegisterInputValidator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(onBackToHomeClick: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var contrasenaVisible by rememberSaveable { mutableStateOf(false) }

    var nombreError by remember { mutableStateOf("") }
    var correoError by remember { mutableStateOf("") }
    var contrasenaError by remember { mutableStateOf("") }
    var direccionError by remember { mutableStateOf("") }
    var telefonoError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    val validator = remember { RegisterInputValidator() }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("¡Registrado correctamente!") },
            text = { Text("Tu cuenta ha sido creada con éxito.") },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    onBackToHomeClick()
                }) {
                    Text("Volver al inicio")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Registro",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        if (generalError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = generalError,
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Gray,
                unfocusedIndicatorColor = Color.LightGray
            ),
            isError = nombreError.isNotEmpty()
        )
        val nombreHelper = if (nombreError.isNotEmpty()) nombreError else "Este campo es obligatorio."
        Text(
            text = nombreHelper,
            fontSize = 12.sp,
            color = if (nombreError.isNotEmpty()) Color.Red else Color.Gray,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
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
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
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
            colors = androidx.compose.material3.TextFieldDefaults.colors(
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
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección de Envío") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Gray,
                unfocusedIndicatorColor = Color.LightGray
            ),
            isError = direccionError.isNotEmpty()
        )
        val direccionHelper = if (direccionError.isNotEmpty()) direccionError else "Ingresa tu dirección completa."
        Text(
            text = direccionHelper,
            fontSize = 12.sp,
            color = if (direccionError.isNotEmpty()) Color.Red else Color.Gray,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it.filter { char -> char.isDigit() } },
            label = { Text("Teléfono de Contacto") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Gray,
                unfocusedIndicatorColor = Color.LightGray
            ),
            isError = telefonoError.isNotEmpty()
        )
        val telefonoHelper = if (telefonoError.isNotEmpty()) telefonoError else "Ej: 1234567890 (solo números)"
        Text(
            text = telefonoHelper,
            fontSize = 12.sp,
            color = if (telefonoError.isNotEmpty()) Color.Red else Color.Gray,
            modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val validationResult = validator.validate(nombre, correo, contrasena, direccion, telefono)
                nombreError = validationResult.errors["nombre"] ?: ""
                correoError = validationResult.errors["correo"] ?: ""
                contrasenaError = validationResult.errors["contrasena"] ?: ""
                direccionError = validationResult.errors["direccion"] ?: ""
                telefonoError = validationResult.errors["telefono"] ?: ""
                generalError = ""

                if (validationResult.isValid) {
                    val request = RegisterUserRequest(
                        email = correo,
                        password = contrasena,
                        address = direccion,
                        name = nombre,
                        telefonoContacto = telefono
                    )
                    ApiClient.userApiService.registerUser(request).enqueue(object : Callback<UserData> {
                        override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                            if (response.isSuccessful && response.body() != null) {
                                showSuccessDialog = true
                            } else {
                                generalError = "No se pudo completar el registro. Intenta más tarde."
                            }
                        }
                        override fun onFailure(call: Call<UserData>, t: Throwable) {
                            generalError = "Error de red: " + t.localizedMessage
                        }
                    })
                } else {
                    Log.d("RegisterScreenEvent", "Validación de campos fallida.")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2C2C2C),
                contentColor = Color.White
            )
        ) {
            Text("Registrarse", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Regístrate para disfrutar de increíbles beneficios!",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackToHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF0F0F0),
                contentColor = Color.Black
            )
        ) {
            Text("Volver al inicio", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true, device = "spec:pixel_4")
@Composable
fun RegisterScreenPreview() {
    GlutationAppTheme {
        RegistroScreen(onBackToHomeClick = {})
    }
}