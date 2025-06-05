// Di file ApiService.kt
package com.example.auxshort // Sesuaikan package Anda

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class UrlResponse(
    @SerializedName("urls")
    val urls: List<UrlItem>?
)

data class UrlItem(
    @SerializedName("original")
    val original: String?,

    @SerializedName("shortened")
    val shortened: String?,

    @SerializedName("created_at")
    val createdAt: String?
)

interface ApiService {

    @GET("v1/auth/google/callback")
    fun getAccessToken(
        @Query("code") authCode: String
    ): Call<SimpleTokenResponse>

    @GET("v1/urls")
    fun getUrls(
        @Header("Authorization") token : String
    ) : Call<UrlResponse>
}