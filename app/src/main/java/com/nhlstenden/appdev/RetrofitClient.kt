package com.nhlstenden.appdev

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // IMPORTANT: Replace with your machine's actual IP address if not using emulator default
    // For Android emulator talking to host machine: "http://10.0.2.2:3000/"
    // For physical device on same Wi-Fi: Find host machine's IP (ipconfig/ifconfig) e.g., "http://192.168.1.100:3000/"
    private const val BASE_URL = "http://10.0.2.2:3000/" // Emulator default

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) 
            .build()

        retrofit.create(ApiService::class.java)
    }
} 