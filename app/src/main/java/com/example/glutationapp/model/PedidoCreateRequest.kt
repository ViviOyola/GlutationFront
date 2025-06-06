package com.example.glutationapp.model

data class PedidoCreateRequest(
    val pedidoFecha: String,
    val user: UserIdOnly,
    val valorTotal: String
) 