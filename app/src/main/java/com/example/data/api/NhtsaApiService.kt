package com.example.data.api

import com.example.data.model.NhtsaResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NhtsaApiService {
    @GET("vehicles/decodevin/{vin}")
    suspend fun decodeVin(
        @Path("vin") vin: String,
        @Query("format") format: String = "json"
    ): NhtsaResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://vpic.nhtsa.dot.gov/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val nhtsaService: NhtsaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NhtsaApiService::class.java)
    }
}
