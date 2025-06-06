// Di file ApiService.kt
package com.example.auxshort // Sesuaikan package Anda

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


data class AnalyticResponse(
    @SerializedName("analytic")
    val analytic: AnalyticData?,

    @SerializedName("url")
    val url: UrlItem?
)

data class AnalyticData(
    @SerializedName("total_click")
    val totalClick: Int?,

    @SerializedName("clicks_per_day")
    val clicksPerDay: List<ClicksPerDay>?,

    @SerializedName("clicks_per_country")
    val clicksPerCountry: List<ClicksPerCountry>?,

    @SerializedName("clicks_per_user_agent")
    val clicksPerUserAgent: List<ClicksPerUserAgent>?
)

data class ClicksPerDay(
    @SerializedName("date")
    val date: String?,

    @SerializedName("count")
    val count: Int?
)

data class ClicksPerCountry(
    @SerializedName("name")
    val name: String?,

    @SerializedName("count")
    val count: Int?
)

data class ClicksPerUserAgent(
    @SerializedName("name")
    val name: String?,

    @SerializedName("count")
    val count: Int?
)

data class UrlResponse(
    @SerializedName("urls")
    val urls: List<UrlItem>?
)

data class CreateUrlRequest(
    @SerializedName("original")
    val original: String,

    @SerializedName("shortened")
    val shortened : String
)

data class UrlItem(
    @SerializedName("original")
    val original: String?,

    @SerializedName("shortened")
    val shortened: String?,

    @SerializedName("created_at")
    val createdAt: String?
)

data class ApiErrorResponse(val error: String)

interface ApiService {

    @GET("v1/auth/google/callback")
    fun getAccessToken(
        @Query("code") authCode: String
    ): Call<SimpleTokenResponse>

    @POST("v1/urls")
    fun createUrl(
        @Header("Authorization") token : String,
        @Body requestBody: CreateUrlRequest
    ) : Call<UrlItem>

    @GET("v1/urls/{id}")
    fun getUrlAndAnalytic(
        @Header("Authorization") token : String,
        @Path("id", encoded = false) id : String,
    ) : Call<AnalyticResponse>

    @GET("v1/urls")
    fun getUrls(
        @Header("Authorization") token : String
    ) : Call<UrlResponse>

    @DELETE("v1/urls/{id}")
    fun deleteUrl(
        @Header("Authorization") token: String,
        @Path("id", encoded = false) id : String,
    ): Call<Void>
}