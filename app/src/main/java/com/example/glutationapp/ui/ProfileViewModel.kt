package com.example.glutationapp.ui

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.glutationapp.data.ProfileRepository
import com.example.glutationapp.model.UserData
import com.example.glutationapp.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val editableName: String = UserSession.userName ?: "",
    val editableEmail: String = UserSession.userEmail ?: "",
    val editableAddress: String = UserSession.userAddress ?: "",
    val editableTelefono: String = UserSession.userTelefonoContacto ?: "",
    val nameError: String? = null,
    val emailError: String? = null,
    val addressError: String? = null,
    val telefonoError: String? = null,
    val generalError: String? = null,
    val orderHistory: List<OrderHistoryItem> = emptyList(),
    val isLoadingOrders: Boolean = true,
    val orderHistoryError: String? = null,
    val isDeletingOrder: Boolean = false,
    val showConfirmDeleteDialog: Boolean = false,
    val orderToDeleteId: Int? = null
)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _uiState.update {
            it.copy(
                editableName = UserSession.userName ?: "",
                editableEmail = UserSession.userEmail ?: "",
                editableAddress = UserSession.userAddress ?: "",
                editableTelefono = UserSession.userTelefonoContacto ?: ""
            )
        }
        fetchOrderHistory()
    }

    fun onEditModeChange(isEdit: Boolean) {
        _uiState.update { it.copy(isEditMode = isEdit) }
        if (!isEdit) {
            resetAndDiscardChanges()
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(editableName = name, nameError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(editableEmail = email, emailError = null) }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(editableAddress = address, addressError = null) }
    }

    fun onTelefonoChange(telefono: String) {
        _uiState.update { it.copy(editableTelefono = telefono, telefonoError = null) }
    }

    private fun validateInput(): Boolean {
        val state = _uiState.value
        var isValid = true
        var nameError: String? = null
        var emailError: String? = null
        var addressError: String? = null
        var telefonoError: String? = null

        if (state.editableName.isBlank()) {
            nameError = "El nombre no puede estar vacío"
            isValid = false
        }
        
        // Use a platform-independent regex for email validation
        val emailRegex = "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+".toRegex()
        if (!emailRegex.matches(state.editableEmail)) {
            emailError = "Correo electrónico inválido"
            isValid = false
        }

        if (state.editableAddress.isBlank()) {
            addressError = "La dirección no puede estar vacía"
            isValid = false
        }
        if (state.editableTelefono.isBlank()) {
            telefonoError = "El teléfono no puede estar vacío"
            isValid = false
        }

        _uiState.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                addressError = addressError,
                telefonoError = telefonoError
            )
        }
        return isValid
    }

    fun saveChanges() {
        if (!validateInput()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            UserSession.userId?.let { userId ->
                val result = repository.updateUserData(
                    userId,
                    _uiState.value.editableName,
                    _uiState.value.editableEmail,
                    _uiState.value.editableAddress,
                    _uiState.value.editableTelefono
                )
                result.onSuccess { updatedUserData ->
                    UserSession.login(updatedUserData)
                    _uiState.update {
                        it.copy(isLoading = false, isEditMode = false)
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, generalError = "Error al actualizar: ${e.message}")
                    }
                }
            } ?: _uiState.update {
                it.copy(isLoading = false, generalError = "Error: Usuario no identificado.")
            }
        }
    }

    fun resetAndDiscardChanges() {
        _uiState.update {
            it.copy(
                isEditMode = false,
                editableName = UserSession.userName ?: "",
                editableEmail = UserSession.userEmail ?: "",
                editableAddress = UserSession.userAddress ?: "",
                editableTelefono = UserSession.userTelefonoContacto ?: "",
                nameError = null,
                emailError = null,
                addressError = null,
                telefonoError = null,
                generalError = null,
                isLoading = false
            )
        }
    }

    fun fetchOrderHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrders = true, orderHistoryError = null) }
            UserSession.userId?.let { userId ->
                val result = repository.fetchOrderHistory(userId)
                result.onSuccess { orders ->
                    _uiState.update { it.copy(orderHistory = orders, isLoadingOrders = false) }
                }.onFailure { e ->
                    _uiState.update { it.copy(orderHistoryError = "Error al cargar historial: ${e.message}", isLoadingOrders = false) }
                }
            } ?: _uiState.update {
                it.copy(orderHistoryError = "No se pudo cargar el historial. Usuario no identificado.", isLoadingOrders = false)
            }
        }
    }

    fun confirmDeleteOrder() {
        viewModelScope.launch {
            _uiState.value.orderToDeleteId?.let { orderId ->
                _uiState.update { it.copy(isDeletingOrder = true) }
                val result = repository.deleteOrder(orderId)
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeletingOrder = false,
                            showConfirmDeleteDialog = false,
                            orderToDeleteId = null
                        )
                    }
                    fetchOrderHistory() // Refresh list
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isDeletingOrder = false,
                            generalError = "Error al eliminar el pedido: ${e.message}"
                        )
                    }
                }
            }
        }
    }
    
    fun onShowConfirmDeleteDialog(orderId: Int) {
        _uiState.update { it.copy(showConfirmDeleteDialog = true, orderToDeleteId = orderId) }
    }

    fun onDismissConfirmDeleteDialog() {
        _uiState.update { it.copy(showConfirmDeleteDialog = false, orderToDeleteId = null) }
    }

}

class ProfileViewModelFactory(private val repository: ProfileRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 