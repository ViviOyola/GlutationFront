package com.example.glutationapp.util

data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
)

class RegisterInputValidator {
    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    fun validate(
        nombre: String,
        correo: String,
        contrasena: String,
        direccion: String,
        telefono: String
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (nombre.isBlank()) {
            errors["nombre"] = "El nombre es obligatorio."
        }
        if (correo.isBlank()) {
            errors["correo"] = "El correo es obligatorio."
        } else if (!isEmailValid(correo)) {
            errors["correo"] = "Correo electrónico no válido."
        }
        if (contrasena.length < 8) {
            errors["contrasena"] = "La contraseña debe tener al menos 8 caracteres."
        }
        if (direccion.isBlank()) {
            errors["direccion"] = "La dirección de envío es obligatoria."
        }
        if (telefono.isBlank()) {
            errors["telefono"] = "El teléfono de contacto es obligatorio."
        } else if (!telefono.all { it.isDigit() } || telefono.length != 10) {
            errors["telefono"] = "Teléfono no válido (solo números, 10 dígitos)."
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
} 