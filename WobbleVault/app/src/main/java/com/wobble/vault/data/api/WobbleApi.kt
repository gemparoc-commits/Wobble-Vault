package com.wobble.vault.data.api

import com.wobble.vault.data.model.AuthUser
import com.wobble.vault.data.model.CreateIncomeSourceRequest
import com.wobble.vault.data.model.CreateOrderRequest
import com.wobble.vault.data.model.CreateUserRequest
import com.wobble.vault.data.model.CsrfTokenResponse
import com.wobble.vault.data.model.DashboardStats
import com.wobble.vault.data.model.IncomeSourceDto
import com.wobble.vault.data.model.InventoryItem
import com.wobble.vault.data.model.InventoryRequest
import com.wobble.vault.data.model.LoginRequest
import com.wobble.vault.data.model.LoginResponse
import com.wobble.vault.data.model.OrderDto
import com.wobble.vault.data.model.PageResponse
import com.wobble.vault.data.model.PermissionDto
import com.wobble.vault.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WobbleApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("api/auth/csrf")
    suspend fun csrf(): Response<CsrfTokenResponse>

    @GET("api/auth/me")
    suspend fun me(): Response<AuthUser>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/dashboard/stats")
    suspend fun dashboardStats(): Response<DashboardStats>

    @GET("api/inventory")
    suspend fun inventory(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<InventoryItem>>

    @POST("api/inventory")
    suspend fun createInventory(@Body body: InventoryRequest): Response<InventoryItem>

    @PUT("api/inventory/{id}")
    suspend fun updateInventory(
        @Path("id") id: String,
        @Body body: InventoryRequest
    ): Response<InventoryItem>

    @DELETE("api/inventory/{id}")
    suspend fun deleteInventory(@Path("id") id: String): Response<Unit>

    @GET("api/orders")
    suspend fun orders(
        @Query("status") status: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<OrderDto>>

    @POST("api/orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): Response<OrderDto>

    @PUT("api/orders/{id}")
    suspend fun updateOrder(
        @Path("id") id: String,
        @Body body: CreateOrderRequest
    ): Response<OrderDto>

    @DELETE("api/orders/{id}")
    suspend fun deleteOrder(@Path("id") id: String): Response<Unit>

    @GET("api/income")
    suspend fun income(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<IncomeSourceDto>>

    @POST("api/income")
    suspend fun createIncome(@Body body: CreateIncomeSourceRequest): Response<IncomeSourceDto>

    @GET("api/income/date-range")
    suspend fun incomeDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<IncomeSourceDto>>

    @DELETE("api/income/{id}")
    suspend fun deleteIncome(@Path("id") id: String): Response<Unit>

    @GET("api/users")
    suspend fun users(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<PageResponse<UserDto>>

    @POST("api/users")
    suspend fun createUser(@Body body: CreateUserRequest): Response<UserDto>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<Unit>

    @POST("api/permissions/grant/{userId}/{pageName}")
    suspend fun grantPermission(
        @Path("userId") userId: String,
        @Path("pageName") pageName: String
    ): Response<PermissionDto>

    @DELETE("api/permissions/revoke/{userId}/{pageName}")
    suspend fun revokePermission(
        @Path("userId") userId: String,
        @Path("pageName") pageName: String
    ): Response<Unit>
}
