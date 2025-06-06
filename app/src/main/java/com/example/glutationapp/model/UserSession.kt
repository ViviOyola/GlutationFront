package com.example.glutationapp.model

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val address: String,
    val telefonoContacto: String?
)

object UserSession {
    private var _currentUser by mutableStateOf<UserData?>(null)
    val currentUser: UserData?
        get() = _currentUser

    private val _isLoggedIn = mutableStateOf(false) // Usamos mutableStateOf para que los Composables puedan reaccionar
    val isLoggedIn: State<Boolean> = _isLoggedIn


    fun login(userData: UserData) {
        _currentUser = userData
        _isLoggedIn.value = true
         android.util.Log.d("UserSession", "User logged in: ${userData.email}, Name: ${userData.name}, Phone: ${userData.telefonoContacto}")
    }

    fun logout() {
        android.util.Log.d("UserSession", "User logged out: ${_currentUser?.email}")
        _currentUser = null
        _isLoggedIn.value = false
    }

    // Getters individuales para facilitar el acceso en Composables y para observar cambios si es necesario
    val userId: Int? get() = _currentUser?.id
    val userName: String? get() = _currentUser?.name
    val userEmail: String? get() = _currentUser?.email
    val userAddress: String? get() = _currentUser?.address
    val userTelefonoContacto: String? get() = _currentUser?.telefonoContacto
} 