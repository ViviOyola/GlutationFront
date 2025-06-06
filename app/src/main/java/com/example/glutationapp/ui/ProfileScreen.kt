package com.example.glutationapp.ui

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.glutationapp.data.ProfileRepository
import com.example.glutationapp.model.UserData
import com.example.glutationapp.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Locale
import java.sql.Timestamp

// Credenciales de BD (considerar mover a un lugar común si se usan en múltiples pantallas)
private const val DB_URL = "jdbc:mysql://192.168.10.162:3306/glutation"
private const val DB_USER = "viviana"
private const val DB_PASSWORD = "1089628220"

// Nueva data class para el historial de pedidos
data class OrderHistoryItem(
    val pedidoId: Int,
    val fecha: String, // Formateada como String para mostrar
    val valorTotal: String // Formateada como String para mostrar
)

// Función para formatear el precio con separador de miles (si es necesario)
fun formatPriceForDisplay(priceString: String): String {
    // Asumimos que priceString es un número entero como string, ej. "150000"
    return try {
        val priceInt = priceString.toInt()
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale.GERMAN)
        val formatter = java.text.DecimalFormat("#,###", symbols)
        formatter.format(priceInt.toLong())
    } catch (e: NumberFormatException) {
        priceString // Devolver el original si no se puede parsear
    }
}

// Función para formatear Timestamp a String legible
fun formatTimestampToString(timestamp: Timestamp?): String {
    if (timestamp == null) return "Fecha no disponible"
    // Ejemplo de formato: "dd MMM yyyy, HH:mm"
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(timestamp)
}

suspend fun fetchOrderHistoryFromDb(userId: Int): Result<List<OrderHistoryItem>> {
    return withContext(Dispatchers.IO) {
        var connection: Connection? = null
        val orderList = mutableListOf<OrderHistoryItem>()
        try {
            Log.d("ProfileScreenDB", "Intentando conectar a: $DB_URL para historial de pedidos")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
            Log.d("ProfileScreenDB", "Conexión establecida para historial de pedidos.")

            connection?.let { conn ->
                // Asumiendo que la PK de pedidos es pedido_id y la FK a usuario es pedido_usuario
                val query = "SELECT pedido_id, pedido_fecha, valor_total FROM pedidos WHERE pedido_usuario = ? ORDER BY pedido_fecha DESC"
                val preparedStatement = conn.prepareStatement(query)
                preparedStatement.setInt(1, userId)
                val resultSet = preparedStatement.executeQuery()

                while (resultSet.next()) {
                    val order = OrderHistoryItem(
                        pedidoId = resultSet.getInt("pedido_id"),
                        fecha = formatTimestampToString(resultSet.getTimestamp("pedido_fecha")),
                        valorTotal = formatPriceForDisplay(resultSet.getString("valor_total") ?: "0")
                    )
                    orderList.add(order)
                }
                resultSet.close()
                preparedStatement.close()
                Log.d("ProfileScreenDB", "Historial de pedidos obtenidos: ${orderList.size}")
                Result.success(orderList)
            } ?: run {
                Log.e("ProfileScreenDB", "FALLO: DriverManager.getConnection() devolvió null para historial.")
                Result.failure(Exception("No se pudo conectar a la base de datos."))
            }
        } catch (e: SQLException) {
            Log.e("ProfileScreenDB", "Error SQL al obtener historial: ${e.message}", e)
            Result.failure(e)
        } catch (e: ClassNotFoundException) {
            Log.e("ProfileScreenDB", "Error de Driver MySQL para historial: ${e.message}", e)
            Result.failure(e)
        } catch (t: Throwable) {
            Log.e("ProfileScreenDB", "Error inesperado al obtener historial: ${t.message}", t)
            Result.failure(t)
        } finally {
            try {
                connection?.close()
                Log.d("ProfileScreenDB", "Conexión cerrada tras obtener historial.")
            } catch (e: SQLException) {
                Log.e("ProfileScreenDB", "Error al cerrar conexión de historial: ${e.message}", e)
            }
        }
    }
}

suspend fun deleteOrderFromDb(pedidoId: Int): Result<Boolean> {
    return withContext(Dispatchers.IO) {
        var connection: Connection? = null
        try {
            Log.d("ProfileScreenDB", "Intentando conectar para eliminar pedido ID: $pedidoId")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
            Log.d("ProfileScreenDB", "Conexión establecida para eliminar pedido.")

            connection?.let { conn ->
                conn.autoCommit = false // Iniciar transacción

                // 1. Eliminar de 'pedido_producto' (usando id_pedido según tu cambio)
                val deletePedidoProductoSql = "DELETE FROM pedido_producto WHERE id_pedido = ?"
                val ppStmt = conn.prepareStatement(deletePedidoProductoSql)
                ppStmt.setInt(1, pedidoId)
                val ppRowsAffected = ppStmt.executeUpdate() // Puede ser 0 si no había productos, no es error
                ppStmt.close()
                Log.d("ProfileScreenDB", "Filas eliminadas de pedido_producto para pedido ID $pedidoId: $ppRowsAffected")

                // 2. Eliminar de 'pedidos'
                val deletePedidoSql = "DELETE FROM pedidos WHERE pedido_id = ?"
                val pedidoStmt = conn.prepareStatement(deletePedidoSql)
                pedidoStmt.setInt(1, pedidoId)
                val pedidoRowsAffected = pedidoStmt.executeUpdate()
                pedidoStmt.close()

                if (pedidoRowsAffected > 0) {
                    conn.commit() // Finalizar transacción
                    Log.d("ProfileScreenDB", "Pedido ID $pedidoId eliminado exitosamente.")
                    Result.success(true)
                } else {
                    conn.rollback() // Si no se eliminó de pedidos (raro si existía)
                    Log.w("ProfileScreenDB", "No se eliminó el pedido ID $pedidoId de la tabla pedidos, rollback.")
                    Result.failure(SQLException("No se encontró el pedido principal para eliminar."))
                }
            } ?: run {
                Log.e("ProfileScreenDB", "FALLO: DriverManager.getConnection() devolvió null al eliminar.")
                Result.failure(Exception("No se pudo conectar a la base de datos para eliminar el pedido."))
            }
        } catch (e: SQLException) {
            Log.e("ProfileScreenDB", "Error SQL al eliminar pedido: ${e.message}", e)
            try { connection?.rollback() } catch (re: SQLException) { Log.e("ProfileScreenDB", "Error en rollback: ${re.message}", re) }
            Result.failure(e)
        } catch (e: ClassNotFoundException) {
            Log.e("ProfileScreenDB", "Error de Driver MySQL al eliminar: ${e.message}", e)
            Result.failure(e)
        } catch (t: Throwable) {
            Log.e("ProfileScreenDB", "Error inesperado al eliminar pedido: ${t.message}", t)
            try { connection?.rollback() } catch (re: SQLException) { Log.e("ProfileScreenDB", "Error en rollback: ${re.message}", re) }
            Result.failure(t)
        } finally {
            try {
                connection?.autoCommit = true // Restaurar autoCommit
                connection?.close()
            } catch (e: SQLException) {
                Log.e("ProfileScreenDB", "Error al cerrar conexión tras eliminar: ${e.message}", e)
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(ProfileRepository()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.generalError != null) {
        Toast.makeText(context, uiState.generalError, Toast.LENGTH_LONG).show()
    }

    if (uiState.showConfirmDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = { viewModel.confirmDeleteOrder() },
            onDismiss = { viewModel.onDismissConfirmDeleteDialog() },
            isDeleting = uiState.isDeletingOrder
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Mi Perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            ProfileInfoSection(
                uiState = uiState,
                onEditModeChange = { viewModel.onEditModeChange(it) },
                onNameChange = { viewModel.onNameChange(it) },
                onEmailChange = { viewModel.onEmailChange(it) },
                onAddressChange = { viewModel.onAddressChange(it) },
                onTelefonoChange = { viewModel.onTelefonoChange(it) },
                onSaveChanges = { viewModel.saveChanges() },
                onCancelChanges = { viewModel.resetAndDiscardChanges() }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                "Historial de Pedidos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isLoadingOrders) {
            item { CircularProgressIndicator() }
        } else if (uiState.orderHistoryError != null) {
            item { Text(uiState.orderHistoryError!!, color = MaterialTheme.colorScheme.error) }
        } else if (uiState.orderHistory.isEmpty()) {
            item { Text("No tienes pedidos anteriores.") }
        } else {
            items(uiState.orderHistory) { order ->
                OrderHistoryCard(
                    order = order,
                    onDelete = {
                        viewModel.onShowConfirmDeleteDialog(order.pedidoId)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar Sesión")
            }
        }
    }
}

@Composable
fun ProfileInfoSection(
    uiState: ProfileState,
    onEditModeChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onSaveChanges: () -> Unit,
    onCancelChanges: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (uiState.isEditMode) {
            ProfileTextField("Nombre Completo", uiState.editableName, onNameChange, uiState.nameError)
            ProfileTextField("Correo Electrónico", uiState.editableEmail, onEmailChange, uiState.emailError, KeyboardType.Email)
            ProfileTextField("Dirección", uiState.editableAddress, onAddressChange, uiState.addressError)
            ProfileTextField("Teléfono", uiState.editableTelefono, onTelefonoChange, uiState.telefonoError, KeyboardType.Phone)

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSaveChanges, enabled = !uiState.isLoading) {
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Guardar")
                }
                Button(onClick = onCancelChanges, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("Cancelar")
                }
            }
        } else {
            InfoRow("Nombre", UserSession.userName)
            InfoRow("Email", UserSession.userEmail)
            InfoRow("Dirección", UserSession.userAddress)
            InfoRow("Teléfono", UserSession.userTelefonoContacto)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onEditModeChange(true) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Editar Perfil")
            }
        }
    }
}

@Composable
fun ProfileTextField(label: String, value: String, onValueChange: (String) -> Unit, error: String?, keyboardType: KeyboardType = KeyboardType.Text) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun InfoRow(label: String, text: String?) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("$label:", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(text ?: "No disponible")
    }
}

@Composable
fun OrderHistoryCard(order: OrderHistoryItem, onDelete: () -> Unit) {
    Card(
                    modifier = Modifier
                        .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pedido #${order.pedidoId}", fontWeight = FontWeight.Bold)
                Text("Fecha: ${order.fecha}", style = MaterialTheme.typography.bodyMedium)
                Text("Total: $${order.valorTotal}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar Pedido", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, isDeleting: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Eliminación") },
        text = { Text("¿Estás seguro de que quieres eliminar este pedido? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Eliminar")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
} 