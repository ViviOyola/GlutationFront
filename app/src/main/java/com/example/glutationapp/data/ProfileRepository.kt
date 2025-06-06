package com.example.glutationapp.data

import android.util.Log
import com.example.glutationapp.model.UserData
import com.example.glutationapp.model.UserSession
import com.example.glutationapp.ui.OrderHistoryItem
import com.example.glutationapp.ui.formatPriceForDisplay
import com.example.glutationapp.ui.formatTimestampToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp

// Credenciales de BD
private const val DB_URL = "jdbc:mysql://192.168.10.162:3306/glutation"
private const val DB_USER = "viviana"
private const val DB_PASSWORD = "1089628220"

class ProfileRepository {

    private fun getConnection(): Connection {
        Class.forName("com.mysql.jdbc.Driver")
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
    }

    suspend fun fetchOrderHistory(userId: Int): Result<List<OrderHistoryItem>> {
        return withContext(Dispatchers.IO) {
            try {
                getConnection().use { connection ->
                    val query = "SELECT pedido_id, pedido_fecha, valor_total FROM pedidos WHERE pedido_usuario = ? ORDER BY pedido_fecha DESC"
                    connection.prepareStatement(query).use { preparedStatement ->
                        preparedStatement.setInt(1, userId)
                        preparedStatement.executeQuery().use { resultSet ->
                            val orderList = mutableListOf<OrderHistoryItem>()
                            while (resultSet.next()) {
                                val order = OrderHistoryItem(
                                    pedidoId = resultSet.getInt("pedido_id"),
                                    fecha = formatTimestampToString(resultSet.getTimestamp("pedido_fecha")),
                                    valorTotal = formatPriceForDisplay(resultSet.getString("valor_total") ?: "0")
                                )
                                orderList.add(order)
                            }
                            Log.d("ProfileRepository", "Order history fetched: ${orderList.size}")
                            Result.success(orderList)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error fetching order history", e)
                Result.failure(e)
            }
        }
    }

    suspend fun deleteOrder(pedidoId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            try {
                connection = getConnection()
                connection.autoCommit = false

                val deletePedidoProductoSql = "DELETE FROM pedido_producto WHERE id_pedido = ?"
                connection.prepareStatement(deletePedidoProductoSql).use { ppStmt ->
                    ppStmt.setInt(1, pedidoId)
                    ppStmt.executeUpdate()
                }

                val deletePedidoSql = "DELETE FROM pedidos WHERE pedido_id = ?"
                connection.prepareStatement(deletePedidoSql).use { pedidoStmt ->
                    pedidoStmt.setInt(1, pedidoId)
                    val pedidoRowsAffected = pedidoStmt.executeUpdate()
                    if (pedidoRowsAffected > 0) {
                        connection.commit()
                        Log.d("ProfileRepository", "Order ID $pedidoId deleted successfully.")
                        Result.success(true)
                    } else {
                        connection.rollback()
                        Log.w("ProfileRepository", "Could not delete order ID $pedidoId, rolling back.")
                        Result.failure(SQLException("Order not found for deletion."))
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error deleting order", e)
                try {
                    connection?.rollback()
                } catch (re: SQLException) {
                    Log.e("ProfileRepository", "Error on rollback", re)
                }
                Result.failure(e)
            } finally {
                try {
                    connection?.autoCommit = true
                    connection?.close()
                } catch (e: SQLException) {
                    Log.e("ProfileRepository", "Error closing connection after delete", e)
                }
            }
        }
    }

    suspend fun updateUserData(userId: Int, name: String, email: String, address: String, telefono: String): Result<UserData> {
        return withContext(Dispatchers.IO) {
            try {
                getConnection().use { connection ->
                    val query = "UPDATE usuarios SET nombre_completo = ?, correo_electronico = ?, direccion = ?, telefono_contacto = ? WHERE id = ?"
                    connection.prepareStatement(query).use { preparedStatement ->
                        preparedStatement.setString(1, name)
                        preparedStatement.setString(2, email)
                        preparedStatement.setString(3, address)
                        preparedStatement.setString(4, telefono)
                        preparedStatement.setInt(5, userId)

                        val rowsAffected = preparedStatement.executeUpdate()
                        if (rowsAffected > 0) {
                            val updatedUserData = UserData(userId, name, email, address, telefono)
                            Log.d("ProfileRepository", "User data updated for user ID: $userId")
                            Result.success(updatedUserData)
                        } else {
                            Result.failure(Exception("User not found or data is the same."))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Error updating user data", e)
                Result.failure(e)
            }
        }
    }
} 