package com.example.glutationapp.network

import com.example.glutationapp.model.UserData
import com.example.glutationapp.model.Producto
import com.example.glutationapp.model.Pedido
import com.example.glutationapp.model.PedidoCreateRequest
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface UserApiService {
    @POST("api/users")
    fun registerUser(@Body user: RegisterUserRequest): Call<UserData>

    @GET("api/users/login")
    fun loginUser(
        @Query("email") email: String,
        @Query("password") password: String
    ): Call<UserData>
}

interface ProductoApiService {
    @GET("api/productos")
    fun getAllProductos(): Call<List<Producto>>

    @GET("api/productos/{id}")
    fun getProductoById(@Path("id") id: Int): Call<Producto>
}

interface PedidoApiService {
    @POST("api/pedidos")
    fun createPedido(@Body pedido: com.example.glutationapp.model.Pedido): Call<com.example.glutationapp.model.Pedido>
}

object ApiClient {
    private val client = OkHttpClient.Builder().build()
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.10.162:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
}

object ProductoApiClient {
    private val client = OkHttpClient.Builder().build()
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.10.162:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    val productoApiService: ProductoApiService = retrofit.create(ProductoApiService::class.java)
}

object PedidoApiClient {
    private val client = OkHttpClient.Builder().build()
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.10.162:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    val pedidoApiService: PedidoApiService = retrofit.create(PedidoApiService::class.java)
}

// Data class para registro (ajusta los nombres de campos si es necesario)
data class RegisterUserRequest(
    val email: String,
    val password: String,
    val address: String,
    val name: String,
    val telefonoContacto: String
) 