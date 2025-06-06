package com.example.glutationapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.ProductionQuantityLimits
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glutationapp.model.UserSession
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// --- INICIO DE FUNCIONES DE UTILIDAD ---
fun parsePriceStringToInt(priceString: String): Int {
    val cleanPriceString = priceString.replace(".", "") // Asume que '.' es solo separador de miles
    return cleanPriceString.toIntOrNull() ?: 0
}

fun formatPriceWithThousandsSeparator(price: Int): String {
    val symbols = DecimalFormatSymbols(Locale.GERMAN) // GERMAN usa '.' como separador de miles
    val formatter = DecimalFormat("#,###", symbols)
    return formatter.format(price.toLong())
}
// --- FIN DE FUNCIONES DE UTILIDAD ---

@Composable
fun CartScreen(
    cartItems: SnapshotStateMap<Producto, Int>,
    onIncrease: (Producto) -> Unit,
    onDecrease: (Producto) -> Unit,
    onRemoveProduct: (Producto) -> Unit,
    onProceedToCheckout: () -> Unit,
    onNavigateToProducts: () -> Unit
) {
    if (cartItems.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ProductionQuantityLimits,
                contentDescription = "Carrito vacío",
                modifier = Modifier.size(120.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Tu carrito está vacío", 
                fontSize = 22.sp, 
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
            UserSession.userName?.let {
                Text(
                    text = "Hola, $it. ¡Añade algunos productos!",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            } ?: run {
                 Text(
                    text = "¡Añade algunos productos para empezar!",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateToProducts,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Explorar Productos", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Carrito de Compras", 
                    fontSize = 28.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                UserSession.userName?.let {
                    Text(
                        text = "Para: $it",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
            items(cartItems.toList()) { (producto, cantidad) ->
                CartItemCard(
                    producto = producto, 
                    cantidad = cantidad, 
                    onIncrease = onIncrease, 
                    onDecrease = onDecrease,
                    onRemoveProduct = onRemoveProduct
                )
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val totalPriceInt = cartItems.entries.sumOf {
                    (product, quantity) -> parsePriceStringToInt(product.precio) * quantity
                }
                Text(
                    text = "Total: ${formatPriceWithThousandsSeparator(totalPriceInt)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.End)
                )
                Button(
                    onClick = onProceedToCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                ) {
                    Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = "Proceder con la compra", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proceder con la Compra", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    producto: Producto,
    cantidad: Int,
    onIncrease: (Producto) -> Unit,
    onDecrease: (Producto) -> Unit,
    onRemoveProduct: (Producto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = producto.imagen),
                contentDescription = producto.nombre,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(producto.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Precio Unitario: ${producto.precio}", fontSize = 14.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onDecrease(producto) }, Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrementar")
                    }
                    Text("$cantidad", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = { onIncrease(producto) }, Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Incrementar")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(onClick = { onRemoveProduct(producto) }, Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover del carrito", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
} 