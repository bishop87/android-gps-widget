package com.bishop87.gpstracker.data.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
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

    /**
     * Recupera i punti GPS da map_data2.php.
     * Le date devono essere nel formato "YYYY-MM-DD HH:mm" (vincolo backend).
     */
    @GET
    suspend fun getMapData(
        @Url url: String,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("device") device: String? = null
    ): Response<MapDataResponse>
}

