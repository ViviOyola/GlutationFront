package com.example.glutationapp.model

data class PedidoProducto(
    val producto: ProductoIdOnly,
    val cantidad: Int
)

data class ProductoIdOnly(
    val productoId: Int
) 