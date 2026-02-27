package com.example.gpstracker.data.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Interfaccia Retrofit per l'API Server GPS.
 * Usa @FormUrlEncoded perché il server PHP legge dati da $_POST.
 */
interface GpsApiService {

    @FormUrlEncoded
    @POST
    suspend fun sendLocation(
        @Url url: String,
        @Field("device") device: String,
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("accuracy") accuracy: Float,
        @Field("timestamp") timestamp: Long,
        @Field("battery") battery: Int
    ): Response<ApiResponse>
}
