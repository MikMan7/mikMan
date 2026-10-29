package com.example.data.service

import com.example.data.db.entity.RouterEntity
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Data Transfer Objects (DTOs) for MikroTik RouterOS REST API ---

data class SystemResourceDto(
    @Json(name = "board-name") val boardName: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "uptime") val uptime: String? = null,
    @Json(name = "cpu-load") val cpuLoad: Int? = null,
    @Json(name = "total-memory") val totalMemory: Long? = null,
    @Json(name = "free-memory") val freeMemory: Long? = null
)

data class SimpleQueueDto(
    @Json(name = ".id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "target") val target: String? = null,
    // RouterOS returns "upload/download" bytes (e.g., "120392/8392019")
    @Json(name = "bytes") val bytes: String? = null,
    // RouterOS returns "upload/download" rate in bps (e.g., "128000/1048576")
    @Json(name = "rate") val rate: String? = null,
    @Json(name = "max-limit") val maxLimit: String? = null,
    @Json(name = "disabled") val disabled: String? = null
) {
    val uploadBytes: Long
        get() = bytes?.split("/")?.getOrNull(0)?.toLongOrNull() ?: 0L

    val downloadBytes: Long
        get() = bytes?.split("/")?.getOrNull(1)?.toLongOrNull() ?: 0L

    val uploadRateBps: Long
        get() = rate?.split("/")?.getOrNull(0)?.toLongOrNull() ?: 0L

    val downloadRateBps: Long
        get() = rate?.split("/")?.getOrNull(1)?.toLongOrNull() ?: 0L
}

data class HotspotActiveDto(
    @Json(name = ".id") val id: String? = null,
    @Json(name = "user") val user: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "mac-address") val macAddress: String? = null,
    @Json(name = "bytes-in") val bytesIn: Long? = null,
    @Json(name = "bytes-out") val bytesOut: Long? = null,
    @Json(name = "uptime") val uptime: String? = null
)

// --- Domain Models ---

data class RouterSystemInfo(
    val boardName: String,
    val version: String,
    val uptime: String,
    val cpuLoad: Int,
    val totalRamMb: Int,
    val freeRamMb: Int
)

data class LiveQueueInfo(
    val name: String,
    val target: String,
    val downloadBytes: Long,
    val uploadBytes: Long,
    val downloadRateBps: Long,
    val uploadRateBps: Long
)

// --- Retrofit API Definition for MikroTik RouterOS ---

interface MikroTikApi {

    @GET("rest/system/resource")
    suspend fun getSystemResource(): Response<SystemResourceDto>

    @GET("rest/queue/simple")
    suspend fun getSimpleQueues(): Response<List<SimpleQueueDto>>

    @GET("rest/ip/hotspot/active")
    suspend fun getActiveHotspotUsers(): Response<List<HotspotActiveDto>>

    @PATCH("rest/queue/simple/{id}")
    suspend fun updateQueueLimit(
        @Path("id") queueId: String,
        @Body body: Map<String, String>
    ): Response<Unit>
}

// --- MikroTikApiService Implementation using Retrofit ---

class MikroTikApiService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createRetrofit(
        ip: String,
        port: Int,
        username: String,
        password: String,
        useSsl: Boolean
    ): Retrofit {
        val protocol = if (useSsl) "https" else "http"
        val baseUrl = "$protocol://$ip:$port/"

        val authInterceptor = Interceptor { chain ->
            val credentials = Credentials.basic(username, password)
            val request = chain.request().newBuilder()
                .header("Authorization", credentials)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun getApi(
        ip: String,
        port: Int,
        username: String,
        password: String,
        useSsl: Boolean
    ): MikroTikApi {
        return createRetrofit(ip, port, username, password, useSsl).create(MikroTikApi::class.java)
    }

    suspend fun testConnection(
        ip: String,
        port: Int,
        username: String,
        password: String,
        useSsl: Boolean
    ): Result<RouterSystemInfo> = withContext(Dispatchers.IO) {
        try {
            val api = getApi(ip, port, username, password, useSsl)
            val response = api.getSystemResource()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP ${response.code()}: ${response.message()}")
                )
            }

            val body = response.body() ?: return@withContext Result.failure(Exception("Empty body"))
            val cpu = body.cpuLoad ?: 0
            val totalMem = ((body.totalMemory ?: 0L) / (1024 * 1024)).toInt()
            val freeMem = ((body.freeMemory ?: 0L) / (1024 * 1024)).toInt()

            Result.success(
                RouterSystemInfo(
                    boardName = body.boardName ?: "MikroTik Router",
                    version = body.version ?: "RouterOS",
                    uptime = body.uptime ?: "0s",
                    cpuLoad = cpu,
                    totalRamMb = totalMem,
                    freeRamMb = freeMem
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchQueues(
        ip: String,
        port: Int,
        username: String,
        password: String,
        useSsl: Boolean
    ): Result<List<LiveQueueInfo>> = withContext(Dispatchers.IO) {
        try {
            val api = getApi(ip, port, username, password, useSsl)
            val response = api.getSimpleQueues()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            }

            val queues = response.body() ?: emptyList()
            val list = queues.map { dto ->
                LiveQueueInfo(
                    name = dto.name ?: "Unknown",
                    target = dto.target ?: "",
                    downloadBytes = dto.downloadBytes,
                    uploadBytes = dto.uploadBytes,
                    downloadRateBps = dto.downloadRateBps,
                    uploadRateBps = dto.uploadRateBps
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHotspotActive(
        ip: String,
        port: Int,
        username: String,
        password: String,
        useSsl: Boolean
    ): Result<List<HotspotActiveDto>> = withContext(Dispatchers.IO) {
        try {
            val api = getApi(ip, port, username, password, useSsl)
            val response = api.getActiveHotspotUsers()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            }
            Result.success(response.body() ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper taking a RouterEntity directly
    suspend fun fetchUserConsumption(router: RouterEntity): Result<List<LiveQueueInfo>> {
        return fetchQueues(
            ip = router.ip,
            port = router.port,
            username = router.username,
            password = router.password,
            useSsl = router.useSsl
        )
    }
}
