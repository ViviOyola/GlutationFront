package com.example.glutationapp.util

class AuthInputValidator {
    private fun isEmailValid(email: String): Boolean {
        // Reutilizamos la misma lógica simple para consistencia
        return email.contains("@") && email.contains(".")
    }

    fun validate(
        correo: String,
        contrasena: String
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (correo.isBlank()) {
            errors["correo"] = "El correo es obligatorio."
        } else if (!isEmailValid(correo)) {
            errors["correo"] = "Correo electrónico no válido."
        }
        if (contrasena.length < 8) {
            errors["contrasena"] = "La contraseña debe tener al menos 8 caracteres."
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
} 