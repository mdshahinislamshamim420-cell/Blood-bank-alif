package com.example.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Retrofit API Endpoints Mapping Interface
interface BloodConnectApiService {
    @GET("donors")
    suspend fun getDonors(): List<BloodDonor>

    @POST("donors")
    suspend fun registerDonor(@Body donor: BloodDonor): BloodDonor

    @GET("requests")
    suspend fun getRequests(): List<BloodRequest>

    @POST("requests")
    suspend fun createRequest(@Body request: BloodRequest): BloodRequest

    @GET("notifications")
    suspend fun getNotifications(): List<DonationNotification>

    @POST("notifications")
    suspend fun sendNotification(@Body notification: DonationNotification): DonationNotification
}

// Client Manager for dynamically configuring Base URL and performing Retrofit network calls
object BloodConnectApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val _apiUrl = MutableStateFlow("")
    val apiUrl: StateFlow<String> = _apiUrl.asStateFlow()

    private val _isRemoteConnected = MutableStateFlow(false)
    val isRemoteConnected: StateFlow<Boolean> = _isRemoteConnected.asStateFlow()

    private var apiService: BloodConnectApiService? = null

    // Set or Update the Remote API Base URL dynamically
    fun updateBaseUrl(url: String): Boolean {
        if (url.isBlank()) {
            _apiUrl.value = ""
            apiService = null
            _isRemoteConnected.value = false
            return true
        }

        // Standardize URL formatting
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }.replace(Regex("/+$"), "") + "/" // Ensure trailing slash

        return try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .writeTimeout(6, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(BloodConnectApiService::class.java)
            _apiUrl.value = formattedUrl
            _isRemoteConnected.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _isRemoteConnected.value = false
            false
        }
    }

    // Network request wrappers for safe Remote Integration
    suspend fun fetchDonors(): Result<List<BloodDonor>> {
        val service = apiService ?: return Result.failure(Exception("API URL is not set or configured."))
        return try {
            val list = service.getDonors()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerDonor(donor: BloodDonor): Result<BloodDonor> {
        val service = apiService ?: return Result.failure(Exception("API URL is not set. Action performed locally."))
        return try {
            val response = service.registerDonor(donor)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRequests(): Result<List<BloodRequest>> {
        val service = apiService ?: return Result.failure(Exception("API URL is not set or configured."))
        return try {
            val list = service.getRequests()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRequest(request: BloodRequest): Result<BloodRequest> {
        val service = apiService ?: return Result.failure(Exception("API URL is not set. Action performed locally."))
        return try {
            val response = service.createRequest(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
