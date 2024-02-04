package cz.jaro.dpmcb.data

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface DopravaApi {

    @POST("map/mapData")
    suspend fun mapData(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("mapapi/timetable")
    suspend fun timetable(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}