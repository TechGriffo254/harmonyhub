// RetrofitInstance.kt
package com.techgriffo254.harmonyhub.data.remote

import com.techgriffo254.harmonyhub.data.JamendoApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api.jamendo.com/v3.0/"

    val api: JamendoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JamendoApiService::class.java)
    }
}