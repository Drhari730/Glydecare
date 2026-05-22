package com.diabeticcare.app.data.remote

import com.diabeticcare.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GlydecareApiClient {
    val isConfigured: Boolean
        get() = BuildConfig.GLYDECARE_API_BASE_URL.isNotBlank()

    val api: GlydecareApi? by lazy {
        val baseUrl = BuildConfig.GLYDECARE_API_BASE_URL.trim()
        if (baseUrl.isBlank()) return@lazy null

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GlydecareApi::class.java)
    }
}
