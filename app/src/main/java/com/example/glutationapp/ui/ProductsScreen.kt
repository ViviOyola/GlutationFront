package com.example.glutationapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.glutationapp.R
import com.example.glutationapp.model.Pedido
import com.example.glutationapp.model.PedidoCreateRequest
import com.example.glutationapp.model.UserIdOnly
import com.example.glutationapp.model.UserSession
import com.example.glutationapp.network.PedidoApiClient
import com.example.glutationapp.network.ProductoApiClient
import com.example.glutationapp.ui.theme.GlutationAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import com.example.glutationapp.model.Producto as ProductoRest
import com.google.gson.Gson

// Modelo simple de producto
data class Producto(
    val id: Int,
    val nombre: String,
    val precio: String,
    val imagen: Int,
    val description: String,
    val quantityDetails: String, // e.g., "350ml", "500g"
    val brand: String
)

// Enum para representar las secciones de la pantalla
enum class ScreenSection {
    PRODUCTS,
    CART,
    PROFILE
}

// Custom Saver for SnapshotStateMap<Producto, Int>
fun cartSaver(allProducts: List<Producto>): Saver<SnapshotStateMap<Producto, Int>, List<Pair<Int, Int>>> =
    Saver(
        save = { cartMap -> cartMap.map { (product, quantity) -> product.id to quantity } },
        restore = { savedList ->
            val map = mutableStateMapOf<Producto, Int>()
            savedList.forEach { (productId, quantity) ->
                allProducts.find { it.id == productId }?.let {
                    map[it] = quantity
                }
            }
            map
        }
    )

// Credenciales de BD (pueden ser movidas a un objeto/constantes comunes si se usan en múltiples sitios)
// private const val DB_URL = "jdbc:mysql://192.168.10.13:3306/glutation"
// private const val DB_USER = "root"
// private const val DB_PASSWORD = "s3cur3_db"

suspend fun fetchProductsFromDb(): Result<List<Producto>> {
    return withContext(Dispatchers.IO) {
        val result = kotlin.coroutines.suspendCoroutine<Result<List<Producto>>> { continuation ->
            ProductoApiClient.productoApiService.getAllProductos().enqueue(object : Callback<List<ProductoRest>> {
                override fun onResponse(call: Call<List<ProductoRest>>, response: Response<List<ProductoRest>>) {
                    if (response.isSuccessful && response.body() != null) {
                        val productosRest = response.body()!!
                        val productos = productosRest.map { p ->
                            val imageResId = when (p.imageName) {
                                "glutathione_prod" -> R.drawable.glutathione_prod
                                "bigo_prod" -> R.drawable.bigo_prod
                                else -> R.drawable.auth_glutation_img_1
                            }
                            Producto(
                                id = p.productoId,
                                nombre = p.nombre,
                                precio = p.valor,
                                imagen = imageResId,
                                description = p.description,
                                quantityDetails = p.medida,
                                brand = p.marca
                            )
                        }
                        continuation.resume(Result.success(productos))
                    } else {
                        continuation.resume(Result.failure(Exception("No se pudo obtener la lista de productos.")))
                    }
                }
                override fun onFailure(call: Call<List<ProductoRest>>, t: Throwable) {
                    continuation.resume(Result.failure(Exception("Error de red: ${t.localizedMessage}", t)))
                }
            })
        }
        result
    }
}

// Helper para parsear precio, similar al de CartScreen
private fun parsePriceStringToIntHelperProducts(priceString: String): Int {
    val cleanPriceString = priceString.replace(".", "")
    return cleanPriceString.toIntOrNull() ?: 0
}

// Reemplazar saveOrderToDb para usar REST
suspend fun saveOrderToDb(
    userId: Int,
    cartItems: SnapshotStateMap<Producto, Int>,
    shippingAddress: String,
    totalAmountInt: Int
): Result<Long> {
    return withContext(Dispatchers.IO) {
        val pedidoProductosApi = cartItems.map { (productoUi, cantidad) ->
            com.example.glutationapp.model.PedidoProducto(
                producto = com.example.glutationapp.model.ProductoIdOnly(productoUi.id),
                cantidad = cantidad
            )
        }

        val pedidoApi = com.example.glutationapp.model.Pedido(
            pedidoId = null, 
            pedidoFecha = null, 
            user = com.example.glutationapp.model.UserIdOnly(userId),
            valorTotal = totalAmountInt.toString(),
            pedidoProductos = pedidoProductosApi
        )
        Log.d("SaveOrderAPI", "Enviando Pedido: ${com.google.gson.Gson().toJson(pedidoApi)}")

        val result = kotlin.coroutines.suspendCoroutine<Result<Long>> { continuation ->
            PedidoApiClient.pedidoApiService.createPedido(pedidoApi).enqueue(object : Callback<com.example.glutationapp.model.Pedido> {
                override fun onResponse(call: Call<com.example.glutationapp.model.Pedido>, response: Response<com.example.glutationapp.model.Pedido>) {
                    if (response.isSuccessful && response.body() != null) {
                        val createdPedido = response.body()!!
                        val id = createdPedido.pedidoId?.toLong() ?: -1L
                        Log.d("SaveOrderAPI", "Pedido creado con ID: $id. Respuesta: ${com.google.gson.Gson().toJson(response.body())}")
                        continuation.resume(Result.success(id))
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("SaveOrderAPI", "Error al crear pedido. Código: ${response.code()}, Mensaje: ${response.message()}, ErrorBody: $errorBody")
                        continuation.resume(Result.failure(Exception("No se pudo crear el pedido. Código: ${response.code()}")))
                    }
                }
                override fun onFailure(call: Call<com.example.glutationapp.model.Pedido>, t: Throwable) {
                    Log.e("SaveOrderAPI", "Fallo en la llamada API para crear pedido: ${t.localizedMessage}", t)
                    continuation.resume(Result.failure(Exception("Error de red: ${t.localizedMessage}", t)))
                }
            })
        }
        result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onSignOutGlobal: () -> Unit // Callback para manejar el cierre de sesión a nivel de app
) {
    var search by remember { mutableStateOf("") }
    // Estado para la lista de productos, error y carga
    var productosState by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cargar productos al iniciar la pantalla
    LaunchedEffect(key1 = Unit) { // key1 = Unit para que se ejecute solo una vez
        isLoading = true
        errorMessage = null
        val result = fetchProductsFromDb()
        result.fold(
            onSuccess = { fetchedProducts ->
                productosState = fetchedProducts
                isLoading = false
            },
            onFailure = { exception ->
                errorMessage = exception.message ?: "Error desconocido al cargar productos."
                isLoading = false
                Log.e("ProductsScreen", "Fallo al cargar productos: $errorMessage")
            }
        )
    }
    
    val filteredProducts = productosState.filter { 
        it.nombre.contains(search, ignoreCase = true) || 
        it.brand.contains(search, ignoreCase = true) ||
        it.description.contains(search, ignoreCase = true)
    }
    
    var currentScreen by rememberSaveable { mutableStateOf(ScreenSection.PRODUCTS) }

    var showProductDetailDialog by remember { mutableStateOf(false) }
    var selectedProductForDetail by remember { mutableStateOf<Producto?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var isProcessingOrder by remember { mutableStateOf(false) } // Nuevo estado para Checkout

    var userShippingAddress by rememberSaveable { mutableStateOf(UserSession.userAddress ?: "Calle Ejemplo 123, Ciudad, País") }

    val cartItems =
        rememberSaveable(saver = cartSaver(productosState)) { mutableStateMapOf<Producto, Int>() }

    val context = LocalContext.current // Necesario para Toasts
    val scope = rememberCoroutineScope() // Necesario para lanzar coroutines

    fun addToCart(product: Producto) {
        cartItems[product] = (cartItems[product] ?: 0) + 1
    }

    fun decreaseCartItem(product: Producto) {
        val currentQuantity = cartItems[product]
        if (currentQuantity != null) {
            if (currentQuantity > 1) {
                cartItems[product] = currentQuantity - 1
            } else {
                cartItems.remove(product)
            }
        }
    }

    fun removeProductFromCart(product: Producto) {
        cartItems.remove(product)
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun getCartTotalItemCount(): Int {
        return cartItems.values.sum()
    }

    val loggedInUserName by remember { mutableStateOf(UserSession.userName) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val homeIconTint =
                    if (currentScreen == ScreenSection.PRODUCTS) LocalContentColor.current else Color.Gray
                val cartIconTint =
                    if (currentScreen == ScreenSection.CART) LocalContentColor.current else Color.Gray
                val profileIconTint =
                    if (currentScreen == ScreenSection.PROFILE) LocalContentColor.current else Color.Gray

                NavigationBarItem(
                    selected = currentScreen == ScreenSection.PRODUCTS,
                    onClick = { currentScreen = ScreenSection.PRODUCTS },
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Inicio",
                            tint = homeIconTint
                        )
                    },
                    label = { Text("INICIO") }
                )
                NavigationBarItem(
                    selected = currentScreen == ScreenSection.CART,
                    onClick = { currentScreen = ScreenSection.CART },
                    icon = {
                        BadgedBox(badge = {
                            if (getCartTotalItemCount() > 0) {
                                Badge { Text("${getCartTotalItemCount()}") }
                            }
                        }) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = "Carrito",
                                tint = cartIconTint
                            )
                        }
                    },
                    label = { Text("Carrito") }
                )
                NavigationBarItem(
                    selected = currentScreen == ScreenSection.PROFILE,
                    onClick = { currentScreen = ScreenSection.PROFILE },
                    icon = {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Perfil",
                            tint = profileIconTint
                        )
                    },
                    label = { Text("Perfil") }
                )
            }
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            when (currentScreen) {
                ScreenSection.PRODUCTS -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Productos",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    UserSession.userName?.let {
                        Text(
                            text = "Hola, $it",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(if (UserSession.userName != null) 8.dp else 16.dp))
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Buscar productos") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (errorMessage != null) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Error: $errorMessage", 
                                color = MaterialTheme.colorScheme.error, 
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (productosState.isEmpty()) {
                         Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No hay productos disponibles en este momento.", 
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    }else {
                        ProductsGrid(
                            productos = filteredProducts,
                            onProductClick = {
                                selectedProductForDetail = it
                                showProductDetailDialog = true
                            },
                            onAddToCartClick = { product ->
                                addToCart(product)
                            }
                        )
                    }
                }
                ScreenSection.CART -> {
                    CartScreen(
                        cartItems = cartItems,
                        onIncrease = { addToCart(it) },
                        onDecrease = { decreaseCartItem(it) },
                        onRemoveProduct = { removeProductFromCart(it) },
                        onProceedToCheckout = { 
                            if (UserSession.isLoggedIn.value && UserSession.userId != null) {
                                showCheckoutDialog = true 
                            } else {
                                Toast.makeText(context, "Debes iniciar sesión para proceder.", Toast.LENGTH_SHORT).show()
                                // Opcionalmente, navegar a la pantalla de login/registro
                            }
                        },
                        onNavigateToProducts = { currentScreen = ScreenSection.PRODUCTS }
                    )
                }
                ScreenSection.PROFILE -> {
                    ProfileScreen(onSignOut = onSignOutGlobal) // Usar el ProfileScreen real
                }
            }
        }

        if (showProductDetailDialog && selectedProductForDetail != null) {
            ProductDetailDialog(
                producto = selectedProductForDetail!!,
                onDismiss = { showProductDetailDialog = false }
            )
        }

        if (showCheckoutDialog) {
            val totalAmountForCheckout = cartItems.entries.sumOf { (product, quantity) ->
                parsePriceStringToIntHelperProducts(product.precio) * quantity
            }
            CheckoutScreen(
                cartItems = cartItems,
                shippingAddress = userShippingAddress, // Podrías permitir editarla aquí si quieres
                totalAmount = totalAmountForCheckout,
                isProcessing = isProcessingOrder,
                onDismissRequest = { if (!isProcessingOrder) showCheckoutDialog = false },
                onFinalizePurchase = {
                    UserSession.userId?.let { userId ->
                        scope.launch {
                            isProcessingOrder = true
                            val saveResult = saveOrderToDb(
                                userId = userId,
                                cartItems = cartItems,
                                shippingAddress = userShippingAddress,
                                totalAmountInt = totalAmountForCheckout
                            )
                            saveResult.fold(
                                onSuccess = { pedidoId ->
                                    Log.i("Checkout", "Pedido #$pedidoId guardado exitosamente.")
                                    Toast.makeText(context, "¡Pedido #${pedidoId} realizado con éxito!", Toast.LENGTH_LONG).show()
                                    clearCart()
                                    showCheckoutDialog = false
                                    currentScreen = ScreenSection.PRODUCTS
                                    // Considera actualizar la dirección de envío por defecto si se modificó
                                    // UserSession.updateUserAddressLocally(userShippingAddress) // si implementas esto
                                },
                                onFailure = { exception ->
                                    Log.e("Checkout", "Error al guardar el pedido: ${exception.message}", exception)
                                    Toast.makeText(context, "Error al procesar el pedido: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                            isProcessingOrder = false
                        }
                    } ?: run {
                        Log.e("Checkout", "Error: UserID no disponible para finalizar la compra.")
                        Toast.makeText(context, "Error: Sesión de usuario no encontrada. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
                        // Podrías incluso cerrar el diálogo de checkout y/o navegar a login
                        // showCheckoutDialog = false 
                    }
                }
            )
        }
    }
}

@Composable
fun ProductsGrid(
    productos: List<Producto>,
    onProductClick: (Producto) -> Unit,
    onAddToCartClick: (Producto) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        for (row in productos.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (producto in row) {
                    ProductCard(
                        producto = producto,
                        modifier = Modifier.weight(1f),
                        onClick = { onProductClick(producto) },
                        onAddToCartClick = { onAddToCartClick(producto) }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProductCard(
    producto: Producto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAddToCartClick: (Producto) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = producto.imagen),
                contentDescription = producto.nombre,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                alpha = 0.6f
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                producto.nombre,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text("$${producto.precio}", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onAddToCartClick(producto) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2C),
                    contentColor = Color.White
                )
            ) {
                Text("Agregar al Carrito", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    producto: Producto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(producto.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        },
        text = {
            Column {
                Image(
                    painter = painterResource(id = producto.imagen),
                    contentDescription = producto.nombre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Descripción:", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(producto.description, fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Marca: ${producto.brand}", fontSize = 14.sp, color = Color.DarkGray)
                Text("Medida: ${producto.quantityDetails}", fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Precio: ${producto.precio}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            // No hay confirmButton, solo un dismissButton estilizado
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2C),
                    contentColor = Color.White
                )
            ) {
                Text("Cerrar", fontSize = 16.sp)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun ProductsScreenPreview() {
    GlutationAppTheme {
        ProductsScreen(onSignOutGlobal = {})
    }
}

@Preview
@Composable
fun ProductDetailDialogPreview() {
    GlutationAppTheme {
        ProductDetailDialog(
            producto = Producto(
                id = 1,
                nombre = "Gluthation Super Max",
                precio = "25.99",
                imagen = R.drawable.glutathione_prod,
                description = "Este es un suplemento increíble con múltiples beneficios para la salud, antioxidante maestro.",
                quantityDetails = "500ml / 60 cápsulas",
                brand = "SaludTotal Labs"
            ),
            onDismiss = {}
        )
    }
}

@Composable
fun CheckoutScreen(
    cartItems: SnapshotStateMap<Producto, Int>,
    shippingAddress: String,
    totalAmount: Int,
    isProcessing: Boolean,
    onDismissRequest: () -> Unit,
    onFinalizePurchase: () -> Unit
) {
    // Funciones de utilidad para formatear precios (pueden estar en un archivo Utils)
    fun parsePriceStringToInt(priceString: String): Int {
        return priceString.replace(".", "").toIntOrNull() ?: 0
    }
    fun formatPriceWithThousandsSeparator(price: Int): String {
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale.GERMAN)
        val formatter = java.text.DecimalFormat("#,###", symbols)
        return formatter.format(price.toLong())
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Confirmar Pedido", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                // Resumen de la dirección
                Text("Enviar a:", fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start))
                Text(shippingAddress, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp))
                Divider()

                // Resumen de productos (simplificado)
                Text("Productos en tu carrito:", fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp))
                cartItems.forEach { (producto, cantidad) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$cantidad x ${producto.nombre}", modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text(formatPriceWithThousandsSeparator(parsePriceStringToInt(producto.precio) * cantidad), fontSize = 14.sp)
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Total a Pagar: ${formatPriceWithThousandsSeparator(totalAmount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp).align(Alignment.End)
                )

                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    Button(
                        onClick = onFinalizePurchase,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Text("Finalizar Compra", color = Color.White, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { if (!isProcessing) onDismissRequest() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", color = Color.Gray, fontSize = 16.sp)
                }
            }
        }
    }
} 