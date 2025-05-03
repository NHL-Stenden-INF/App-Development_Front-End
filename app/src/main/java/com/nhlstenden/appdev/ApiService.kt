package com.nhlstenden.appdev

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("user/")
    suspend fun registerUser(@Body request: RegisterRequest): Response<String> 

    // Login via Basic Auth (GET /user/)
    @GET("user/")
    suspend fun loginUser(@Header("Authorization") authHeader: String): Response<List<UserResponse>> 

} 