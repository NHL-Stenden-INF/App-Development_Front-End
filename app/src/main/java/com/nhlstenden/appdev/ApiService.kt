package com.nhlstenden.appdev

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ApiService {
    @GET("auth/")
    suspend fun login(@Header("Authorization") authHeader: String): Response<UserResponse>

    @POST("user/")
    suspend fun registerUser(@Body request: RegisterRequest): Response<String> 

    @GET("user/")
    suspend fun getUsers(@Header("Authorization") authHeader: String): Response<List<UserResponse>>

    @GET("user/{user-id}")
    suspend fun getUser(@Header("Authorization") authHeader: String): Response<UserResponse>

    @PATCH("user/{user-id}")
    suspend fun updateUser(@Header("Authorization") authHeader: String): Response<UserResponse>

    @DELETE("user/{user-id}")
    suspend fun deleteUser(@Header("Authorization") authHeader: String): Response<String>
} 