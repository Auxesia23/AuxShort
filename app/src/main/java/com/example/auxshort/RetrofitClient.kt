// Di file RetrofitClient.kt
package com.example.auxshort // Sesuaikan package Anda

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // GANTI DENGAN BASE URL SERVER ANDA YANG SEBENARNYA
    private const val BASE_URL = "https://b5db-125-163-13-14.ngrok-free.app/" // Contoh: "http://10.0.2.2:8080/" untuk emulator ke localhost host

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}