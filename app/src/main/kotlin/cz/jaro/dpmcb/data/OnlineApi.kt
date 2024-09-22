package cz.jaro.dpmcb.data

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface OnlineApi {

    @GET("map/connections?query=325")
    suspend fun mapData(): Response<ResponseBody>

    @POST("mapapi/timetable")
    suspend fun timetable(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST("map/pathData")
    suspend fun pathData(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}