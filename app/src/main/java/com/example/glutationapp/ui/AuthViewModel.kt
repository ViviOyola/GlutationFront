package com.example.glutationapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.glutationapp.model.UserData
import com.example.glutationapp.network.UserApiService
import com.example.glutationapp.util.AuthInputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userData: UserData) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val userApiService: UserApiService,
    private val validator: AuthInputValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    fun login(correo: String, contrasena: String) {
        val validationResult = validator.validate(correo, contrasena)
        _validationErrors.value = validationResult.errors

        if (validationResult.isValid) {
            _uiState.value = AuthUiState.Loading
            
            userApiService.loginUser(correo, contrasena).enqueue(object : Callback<UserData> {
                override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                    if (response.isSuccessful && response.body() != null) {
                        _uiState.value = AuthUiState.Success(response.body()!!)
                    } else {
                        _uiState.value = AuthUiState.Error("Correo o contraseña incorrectos.")
                    }
                }

                override fun onFailure(call: Call<UserData>, t: Throwable) {
                    _uiState.value = AuthUiState.Error("Error de red: ${t.message}")
                }
            })
        }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState.Idle
        _validationErrors.value = emptyMap()
    }
}

class AuthViewModelFactory(
    private val userApiService: UserApiService,
    private val validator: AuthInputValidator
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userApiService, validator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 