package com.wvp.device.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * WVP API 客户端封装
 * 对应原始 Python 版本 api_client.py 中的 WVPClient 类
 */
class WVPClient(
    private val baseUrl: String,
    private val username: String,
    private val password: String
) {
    @Volatile
    private var accessToken: String? = null
    private val gson = Gson()

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        accessToken?.let {
            request.addHeader("access-token", it)
        }
        chain.proceed(request.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: WVPApiService = retrofit.create(WVPApiService::class.java)

    /**
     * 登录 - MD5 加密密码
     */
    suspend fun login(): String {
        val pwdMd5 = WVPClient.md5(password)
        val response = api.login(username, pwdMd5)
        
        if (!response.isSuccessful) {
            throw RuntimeException("登录失败: HTTP ${response.code()} ${response.message()}")
        }

        val body = response.body() ?: throw RuntimeException("登录失败: 响应为空")
        
        // 尝试从不同位置获取 token
        accessToken = when {
            body.has("accessToken") -> body.get("accessToken").asString
            body.has("data") && body.get("data").isJsonObject -> {
                val data = body.getAsJsonObject("data")
                if (data.has("accessToken")) data.get("accessToken").asString else null
            }
            else -> null
        }

        // 如果 body 中没有 token，可以从响应头获取
        if (accessToken == null) {
            accessToken = response.headers().get("access-token")
        }

        if (accessToken == null) {
            throw RuntimeException("登录失败: 无法获取访问令牌 - $body")
        }

        return accessToken!!
    }

    /**
     * 获取所有设备（分页）
     */
    private suspend fun ensureLoggedIn() {
        if (accessToken == null) {
            login()
        }
    }

    suspend fun getAllDevices(): List<Map<String, Any>> {
        ensureLoggedIn()
        val allDevices = mutableListOf<Map<String, Any>>()
        var page = 1
        val count = 100

        while (true) {
            val response = api.getDevices(page, count)
            if (!response.isSuccessful) {
                throw RuntimeException("获取设备列表失败: HTTP ${response.code()}")
            }

            val body = response.body() ?: break
            val raw = if (body.has("data") && body.get("data").isJsonObject) {
                body.getAsJsonObject("data")
            } else body

            val total = if (raw.has("total")) raw.get("total").asInt else 0
            val list = if (raw.has("list") && raw.get("list").isJsonArray) {
                gson.fromJson<Array<Map<String, Any>>>(raw.getAsJsonArray("list"), object : TypeToken<Array<Map<String, Any>>>() {}.type).toList()
            } else emptyList()

            allDevices.addAll(list)

            if (total > 0 && allDevices.size >= total) break
            if (list.size < count) break
            
            page++
        }

        return allDevices
    }

    /**
     * 获取设备详情
     */
    suspend fun getDeviceDetail(deviceId: String): Map<String, Any> {
        ensureLoggedIn()
        val response = api.getDeviceDetail(deviceId)
        if (!response.isSuccessful) {
            throw RuntimeException("获取设备详情失败: HTTP ${response.code()}")
        }
        val body = response.body() ?: throw RuntimeException("获取设备详情失败: 响应为空")
        val data = if (body.has("data") && body.get("data").isJsonObject) {
            body.getAsJsonObject("data")
        } else body
        return gson.fromJson<Map<String, Any>>(data, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
    }

    /**
     * 更新设备信息
     */
    suspend fun updateDevice(deviceData: Map<String, Any>): Map<String, Any> {
        ensureLoggedIn()
        val json = gson.toJsonTree(deviceData).asJsonObject
        val response = api.updateDevice(json)
        if (!response.isSuccessful) {
            throw RuntimeException("更新设备失败: HTTP ${response.code()}")
        }
        val body = response.body() ?: throw RuntimeException("更新设备失败: 响应为空")
        return gson.fromJson<Map<String, Any>>(body, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
    }

    /**
     * 获取通道列表（分页）
     */
    suspend fun getChannels(deviceId: String): List<Map<String, Any>> {
        ensureLoggedIn()
        val allChannels = mutableListOf<Map<String, Any>>()
        var page = 1
        val count = 100

        while (true) {
            val response = api.getChannels(deviceId, page, count)
            if (!response.isSuccessful) {
                throw RuntimeException("获取通道列表失败: HTTP ${response.code()}")
            }

            val body = response.body() ?: break
            val raw = if (body.has("data") && body.get("data").isJsonObject) {
                body.getAsJsonObject("data")
            } else body

            val total = if (raw.has("total")) raw.get("total").asInt else 0
            val list = if (raw.has("list") && raw.get("list").isJsonArray) {
                gson.fromJson<Array<Map<String, Any>>>(raw.getAsJsonArray("list"), object : TypeToken<Array<Map<String, Any>>>() {}.type).toList()
            } else emptyList()

            allChannels.addAll(list)

            if (total > 0 && allChannels.size >= total) break
            if (list.size < count) break
            
            page++
        }

        return allChannels
    }

    /**
     * 同步设备目录
     */
    suspend fun syncDevice(deviceId: String): Map<String, Any> {
        ensureLoggedIn()
        val response = api.syncDevice(deviceId)
        if (!response.isSuccessful) {
            throw RuntimeException("同步设备失败: HTTP ${response.code()}")
        }
        val body = response.body() ?: throw RuntimeException("同步设备失败: 响应为空")
        return gson.fromJson<Map<String, Any>>(body, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
    }

    /**
     * 设置传输模式 (TCP/UDP)
     */
    suspend fun setTransportMode(deviceId: String, streamMode: String): Map<String, Any> {
        ensureLoggedIn()
        val response = api.setTransportMode(deviceId, streamMode)
        if (!response.isSuccessful) {
            throw RuntimeException("设置传输模式失败: HTTP ${response.code()}")
        }
        val body = response.body() ?: throw RuntimeException("设置传输模式失败: 响应为空")
        return gson.fromJson<Map<String, Any>>(body, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
    }

    /**
     * 开始点播
     */
    suspend fun startPlay(deviceId: String, channelId: String): Map<String, Any> {
        ensureLoggedIn()
        val response = api.startPlay(deviceId, channelId)
        if (!response.isSuccessful) {
            throw RuntimeException("点播失败: HTTP ${response.code()}")
        }
        val body = response.body() ?: throw RuntimeException("点播失败: 响应为空")
        return gson.fromJson<Map<String, Any>>(body, object : TypeToken<Map<String, Any>>() {}.type) as Map<String, Any>
    }

    /**
     * 停止点播
     */
    suspend fun stopPlay(deviceId: String, channelId: String) {
        ensureLoggedIn()
        try {
            api.stopPlay(deviceId, channelId)
        } catch (e: Exception) {
            // 忽略停止播放的错误
        }
    }

    companion object {
        /**
         * MD5 哈希
         */
        fun md5(input: String): String {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}





