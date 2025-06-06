package com.example.glutationapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CheckoutScreen(
    cartItems: SnapshotStateMap<Producto, Int>,
    shippingAddress: String,
    onDismissRequest: () -> Unit,
    onFinalizePurchase: () -> Unit
) {
    val totalPrice = cartItems.entries.sumOf { (product, quantity) ->
        product.precio.toDoubleOrNull()?.times(quantity) ?: 0.0
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f) // Take up 95% of screen width
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Resumen de Compra", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                // Items Summary
                if (cartItems.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp) // Limit height of item list
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems.toList()) { (product, quantity) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${product.nombre} (x$quantity)", fontSize = 14.sp, modifier = Modifier.weight(1f))
                                val itemTotalPrice = product.precio.toDoubleOrNull()?.times(quantity) ?: 0.0
                                Text(String.format("$%.2f", itemTotalPrice), fontSize = 14.sp)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                } else {
                    Text("No hay items en el carrito.", modifier = Modifier.padding(bottom = 16.dp))
                }

                // Total Price
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total a Pagar:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("$%.2f", totalPrice), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }

                // Shipping Address (Uses the passed address)
                Text("Dirección de Envío:", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                Text(shippingAddress, fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(bottom = 16.dp))
                Divider()

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Volver al Carrito")
                    }
                    Button(
                        onClick = onFinalizePurchase,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Text("Finalizar Compra", color = Color.White)
                    }
                }
            }
        }
    }
} 