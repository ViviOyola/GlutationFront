package com.example.glutationapp.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RegisterInputValidatorTest {

    private lateinit var validator: RegisterInputValidator

    @Before
    fun setUp() {
        validator = RegisterInputValidator()
    }

    @Test
    fun `validate with all fields correct returns valid`() {
        val result = validator.validate(
            nombre = "John Doe",
            correo = "john.doe@example.com",
            contrasena = "password123",
            direccion = "123 Main St",
            telefono = "1234567890"
        )
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate with blank name returns invalid and error message`() {
        val result = validator.validate(
            nombre = "",
            correo = "john.doe@example.com",
            contrasena = "password123",
            direccion = "123 Main St",
            telefono = "1234567890"
        )
        assertFalse(result.isValid)
        assertEquals("El nombre es obligatorio.", result.errors["nombre"])
    }

    @Test
    fun `validate with invalid email returns invalid and error message`() {
        val result = validator.validate(
            nombre = "John Doe",
            correo = "invalid-email",
            contrasena = "password123",
            direccion = "123 Main St",
            telefono = "1234567890"
        )
        assertFalse(result.isValid)
        assertEquals("Correo electrónico no válido.", result.errors["correo"])
    }

    @Test
    fun `validate with short password returns invalid and error message`() {
        val result = validator.validate(
            nombre = "John Doe",
            correo = "john.doe@example.com",
            contrasena = "123",
            direccion = "123 Main St",
            telefono = "1234567890"
        )
        assertFalse(result.isValid)
        assertEquals("La contraseña debe tener al menos 8 caracteres.", result.errors["contrasena"])
    }

    @Test
    fun `validate with blank address returns invalid and error message`() {
        val result = validator.validate(
            nombre = "John Doe",
            correo = "john.doe@example.com",
            contrasena = "password123",
            direccion = "",
            telefono = "1234567890"
        )
        assertFalse(result.isValid)
        assertEquals("La dirección de envío es obligatoria.", result.errors["direccion"])
    }
    
    @Test
    fun `validate with invalid phone number returns invalid and error message`() {
        val result = validator.validate(
            nombre = "John Doe",
            correo = "john.doe@example.com",
            contrasena = "password123",
            direccion = "123 Main St",
            telefono = "12345" // Not 10 digits
        )
        assertFalse(result.isValid)
        assertEquals("Teléfono no válido (solo números, 10 dígitos).", result.errors["telefono"])
    }

    @Test
    fun `validate with multiple errors returns invalid and all error messages`() {
        val result = validator.validate(
            nombre = "",
            correo = "invalid-email",
            contrasena = "short",
            direccion = "",
            telefono = "abc"
        )
        assertFalse(result.isValid)
        assertEquals(5, result.errors.size)
        assertEquals("El nombre es obligatorio.", result.errors["nombre"])
        assertEquals("Correo electrónico no válido.", result.errors["correo"])
        assertEquals("La contraseña debe tener al menos 8 caracteres.", result.errors["contrasena"])
        assertEquals("La dirección de envío es obligatoria.", result.errors["direccion"])
        assertEquals("Teléfono no válido (solo números, 10 dígitos).", result.errors["telefono"])
    }
} 