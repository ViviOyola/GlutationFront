package com.example.glutationapp.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthInputValidatorTest {

    private lateinit var validator: AuthInputValidator

    @Before
    fun setUp() {
        validator = AuthInputValidator()
    }

    @Test
    fun `validacion con credenciales correctas devuelve valido`() {
        val result = validator.validate(
            correo = "test@example.com",
            contrasena = "password123"
        )
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validacion con correo en blanco devuelve invalido y error`() {
        val result = validator.validate(
            correo = "",
            contrasena = "password123"
        )
        assertFalse(result.isValid)
        assertEquals("El correo es obligatorio.", result.errors["correo"])
    }

    @Test
    fun `validacion con formato de correo invalido devuelve invalido y error`() {
        val result = validator.validate(
            correo = "test-example.com",
            contrasena = "password123"
        )
        assertFalse(result.isValid)
        assertEquals("Correo electrónico no válido.", result.errors["correo"])
    }

    @Test
    fun `validacion con contrasena corta devuelve invalido y error`() {
        val result = validator.validate(
            correo = "test@example.com",
            contrasena = "1234"
        )
        assertFalse(result.isValid)
        assertEquals("La contraseña debe tener al menos 8 caracteres.", result.errors["contrasena"])
    }

    @Test
    fun `validacion con ambos campos incorrectos devuelve invalido y multiples errores`() {
        val result = validator.validate(
            correo = "",
            contrasena = "short"
        )
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals("El correo es obligatorio.", result.errors["correo"])
        assertEquals("La contraseña debe tener al menos 8 caracteres.", result.errors["contrasena"])
    }

    @Test
    fun `validacion con correo que solo contiene arroba se considera invalid`() {
        // Esta prueba documenta el comportamiento actual de la validación de correo simple.
        // Dependiendo de los requisitos, la función isEmailValid podría necesitar mejoras.
        val result = validator.validate(
            correo = "test@domain", // Un caso simple válido según la lógica actual
            contrasena = "password123"
        )
        assertFalse(result.isValid)
    }

    @Test
    fun `validacion con contrasena de exactamente 8 caracteres es valido`() {
        val result = validator.validate(
            correo = "test@example.com",
            contrasena = "12345678"
        )
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validacion con contrasena que contiene espacios es valido`() {
        val result = validator.validate(
            correo = "test@example.com",
            contrasena = "pass word 123"
        )
        assertTrue(result.isValid)
    }

    @Test
    fun `validacion con contrasena en blanco devuelve invalido y error`() {
        val result = validator.validate(
            correo = "test@example.com",
            contrasena = ""
        )
        assertFalse(result.isValid)
        assertEquals("La contraseña debe tener al menos 8 caracteres.", result.errors["contrasena"])
    }
} 