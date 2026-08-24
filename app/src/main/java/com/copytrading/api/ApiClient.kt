package com.copytrading.api

import android.content.Context
import com.copytrading.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // Derniere erreur HTTP pour affichage dans l'UI
    var lastErrorCode: Int = 0
        private set
    var lastErrorMessage: String = ""
        private set

    fun clearError() {
        lastErrorCode = 0
        lastErrorMessage = ""
    }

    private fun getBaseUrl(): String {
        val prefs = context.getSharedPreferences("copytrading", Context.MODE_PRIVATE)
        val host = prefs.getString("server_host", "") ?: ""
        val port = prefs.getString("server_port", "8000") ?: "8000"
        return "http://$host:$port"
    }

    private fun getToken(): String {
        val prefs = context.getSharedPreferences("copytrading", Context.MODE_PRIVATE)
        return prefs.getString("api_token", "") ?: ""
    }

    private fun buildRequest(path: String, method: String = "GET", body: String? = null): Request {
        val url = "${getBaseUrl()}$path"
        val builder = Request.Builder().url(url)

        val token = getToken()
        if (token.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        when (method) {
            "POST" -> builder.post(
                (body ?: "").toRequestBody("application/json".toMediaType())
            )
            "PUT" -> builder.put(
                (body ?: "").toRequestBody("application/json".toMediaType())
            )
        }

        return builder.build()
    }

    private suspend fun <T> execute(request: Request, clazz: Class<T>): T? = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (response.isSuccessful) {
                clearError()
                gson.fromJson(body, clazz)
            } else {
                lastErrorCode = response.code
                lastErrorMessage = try {
                    val json = gson.fromJson(body, Map::class.java)
                    json["detail"] as? String ?: "Erreur HTTP ${response.code}"
                } catch (_: Exception) {
                    "Erreur HTTP ${response.code}"
                }
                null
            }
        } catch (e: Exception) {
            lastErrorCode = -1
            lastErrorMessage = e.message ?: "Erreur reseau"
            e.printStackTrace()
            null
        }
    }

    private suspend fun executeRaw(request: Request): String? = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- STATUS ---
    suspend fun getStatus(): StatusResponse? {
        val req = buildRequest("/api/status")
        return execute(req, StatusResponse::class.java)
    }

    // --- DASHBOARD ---
    suspend fun getDashboard(): DashboardResponse? {
        val req = buildRequest("/api/dashboard")
        return execute(req, DashboardResponse::class.java)
    }

    // --- POSITIONS ---
    suspend fun getPositions(): PositionsResponse? {
        val req = buildRequest("/api/positions")
        return execute(req, PositionsResponse::class.java)
    }

    // --- TRADES ---
    suspend fun getTrades(days: Int = 7, fromDate: String? = null, toDate: String? = null): TradesResponse? {
        val url = if (fromDate != null && toDate != null) {
            "/api/trades?from_date=$fromDate&to_date=$toDate"
        } else {
            "/api/trades?days=$days"
        }
        val req = buildRequest(url)
        return execute(req, TradesResponse::class.java)
    }

    // --- BOT START ---
    suspend fun startBot(): BotActionResponse? {
        val req = buildRequest("/api/bot/start", "POST")
        return execute(req, BotActionResponse::class.java)
    }

    // --- BOT STOP ---
    suspend fun stopBot(): BotActionResponse? {
        val req = buildRequest("/api/bot/stop", "POST")
        return execute(req, BotActionResponse::class.java)
    }

    // --- CONFIG ---
    suspend fun getConfig(): ConfigResponse? {
        val req = buildRequest("/api/config")
        return execute(req, ConfigResponse::class.java)
    }

    suspend fun updateConfig(values: Map<String, String>): Boolean {
        val body = gson.toJson(mapOf("values" to values))
        val req = buildRequest("/api/config", "PUT", body)
        val result = executeRaw(req)
        return result != null
    }

    // --- LOGS ---
    suspend fun getLogs(lines: Int = 100): LogsResponse? {
        val req = buildRequest("/api/logs?lines=$lines")
        return execute(req, LogsResponse::class.java)
    }

    // --- CLOSE POSITION ---
    suspend fun closePosition(ticket: Long): CloseResponse? {
        val req = buildRequest("/api/positions/$ticket/close", "POST")
        return execute(req, CloseResponse::class.java)
    }

    // --- CLOSE ALL ---
    suspend fun closeAll(): CloseAllResponse? {
        val req = buildRequest("/api/positions/close-all", "POST")
        return execute(req, CloseAllResponse::class.java)
    }

    // --- TEST CONNECTION ---
    suspend fun testConnection(): Boolean {
        val req = buildRequest("/api/status")
        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(req).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
}
