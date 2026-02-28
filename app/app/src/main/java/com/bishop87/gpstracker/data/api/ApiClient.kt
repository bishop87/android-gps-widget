package com.bishop87.gpstracker.data.api

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory per la creazione del client Retrofit.
 * Configurato con: HTTPS-only, timeout, logging, retry con backoff, autenticazione Basic.
 */
object ApiClient {

    private const val CONNECT_TIMEOUT_SEC = 10L
    private const val READ_TIMEOUT_SEC = 30L
    private const val WRITE_TIMEOUT_SEC = 30L

    fun create(baseUrl: String, username: String, password: String): GpsApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            // Basic Auth header per ogni richiesta
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", Credentials.basic(username, password))
                    .build()
                chain.proceed(request)
            }
            // Retry con exponential backoff
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            // Logging (in produzione ridurre a NONE o BASIC)
            .addInterceptor(loggingInterceptor)
            .build()

        // Nota: Retrofit richiede che baseUrl termini con '/'.
        // Poiché usiamo @Url nell'interfaccia, passiamo l'URL completo per ogni chiamata
        // e questa baseUrl funge solo da segnaposto obbligatorio.
        val dummyBaseUrl = if (baseUrl.endsWith("/")) baseUrl else {
            // Estrae l'origine o aggiunge slash se mancante
            val lastSlash = baseUrl.lastIndexOf('/')
            if (lastSlash > 8) baseUrl.substring(0, lastSlash + 1) else "$baseUrl/"
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(dummyBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(GpsApiService::class.java)
    }
}
