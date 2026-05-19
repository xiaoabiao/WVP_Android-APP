package com.wvp.device.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

/**
 * WVP-PRO API 接口定义
 * 对应原始 Python 版本中 api_client.py 的功能
 */
interface WVPApiService {

    @GET("/api/user/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<JsonObject>

    @GET("/api/device/query/devices")
    suspend fun getDevices(
        @Query("page") page: Int,
        @Query("count") count: Int,
        @Query("query") query: String = ""
    ): Response<JsonObject>

    @GET("/api/device/query/devices/{deviceId}")
    suspend fun getDeviceDetail(
        @Path("deviceId") deviceId: String
    ): Response<JsonObject>

    @POST("/api/device/query/device/update")
    suspend fun updateDevice(
        @Body deviceData: JsonObject
    ): Response<JsonObject>

    @GET("/api/device/query/devices/{deviceId}/channels")
    suspend fun getChannels(
        @Path("deviceId") deviceId: String,
        @Query("page") page: Int,
        @Query("count") count: Int,
        @Query("query") query: String = "",
        @Query("online") online: String = "",
        @Query("channelType") channelType: String = ""
    ): Response<JsonObject>

    @GET("/api/device/query/devices/{deviceId}/sync")
    suspend fun syncDevice(
        @Path("deviceId") deviceId: String
    ): Response<JsonObject>

    @POST("/api/device/query/transport/{deviceId}/{streamMode}")
    suspend fun setTransportMode(
        @Path("deviceId") deviceId: String,
        @Path("streamMode") streamMode: String
    ): Response<JsonObject>

    @GET("/api/play/start/{deviceId}/{channelId}")
    suspend fun startPlay(
        @Path("deviceId") deviceId: String,
        @Path("channelId") channelId: String
    ): Response<JsonObject>

    @GET("/api/play/stop/{deviceId}/{channelId}")
    suspend fun stopPlay(
        @Path("deviceId") deviceId: String,
        @Path("channelId") channelId: String
    ): Response<JsonObject>
}
