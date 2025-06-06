package com.example.glutationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.glutationapp.ui.AuthScreen
import com.example.glutationapp.ui.ProductsScreen
import com.example.glutationapp.ui.RegistroScreen
import com.example.glutationapp.ui.theme.GlutationAppTheme
import com.example.glutationapp.model.UserSession

// Define an enum for screen states
enum class Screen {
    Auth,
    Registro,
    Products
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlutationAppTheme {
                var currentScreen by remember { mutableStateOf(Screen.Auth) }

                when (currentScreen) {
                    Screen.Auth -> AuthScreen(
                        onRegisterClick = { currentScreen = Screen.Registro },
                        onLoginSuccess = { currentScreen = Screen.Products }
                    )
                    Screen.Registro -> RegistroScreen(
                        onBackToHomeClick = { currentScreen = Screen.Auth }
                    )
                    Screen.Products -> ProductsScreen(
                        onSignOutGlobal = {
                            UserSession.logout()
                            currentScreen = Screen.Auth
                        }
                    )
                }
            }
        }
    }
}
