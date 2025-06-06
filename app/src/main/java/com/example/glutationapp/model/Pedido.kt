package com.example.glutationapp.model

import java.time.LocalDateTime

// Simplificado para serialización/deserialización con Retrofit
// El campo pedidoId puede ser null al crear
// pedidoFecha puede ser null, el backend lo setea
// user debe tener al menos el id
// pedidoProductos es una lista de PedidoProducto

data class Pedido(
    val pedidoId: Int? = null,
    val pedidoFecha: String? = null, // Usar String para compatibilidad con JSON
    val user: UserIdOnly,
    val valorTotal: String,
    val pedidoProductos: List<PedidoProducto>
)

data class UserIdOnly(
    val id: Int
) 