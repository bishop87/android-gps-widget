package com.bishop87.gpstracker.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

/**
 * OkHttp Interceptor che implementa retry con exponential backoff.
 * Ritenta la chiamata su errori di rete o risposte HTTP 5xx.
 *
 * @param maxRetries numero massimo di tentativi (default 3)
 * @param initialDelayMs ritardo iniziale in ms (default 1000ms)
 * @param factor moltiplicatore per ogni retry (default 2.0)
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val factor: Double = 2.0,
    private val maxDelayMs: Long = 30_000L
) : Interceptor {

    private val tag = "RetryInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)

                // Retry on 5xx server errors
                if (response.isSuccessful || response.code < 500) {
                    return response
                }

                response.close()
                Log.w(tag, "HTTP ${response.code} — tentativo ${attempt + 1}/$maxRetries")

            } catch (e: IOException) {
                lastException = e
                Log.w(tag, "Errore rete — tentativo ${attempt + 1}/$maxRetries: ${e.message}")
            }

            if (attempt < maxRetries) {
                val delayMs = min(
                    (initialDelayMs * factor.pow(attempt.toDouble())).toLong(),
                    maxDelayMs
                )
                Log.d(tag, "Retry tra ${delayMs}ms...")
                Thread.sleep(delayMs)
            }

            attempt++
        }

        throw lastException ?: IOException("Richiesta fallita dopo $maxRetries tentativi")
    }
}
