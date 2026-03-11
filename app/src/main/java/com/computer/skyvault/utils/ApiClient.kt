package com.computer.skyvault.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException


object ApiClient {
    const val TAG = "ApiClient"
    const val IP = "10.21.52.153"
    const val PORT = "8080"
    const val BASE_URL = "http://$IP:$PORT/v1/api/"
    const val GET = "GET"
    const val POST = "POST"
    const val DELETE = "DELETE"
    const val PUT = "PUT"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    fun onMain(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post { block() }
    }

    fun sendOkHttpRequest(address: String, method: String = GET, body: RequestBody? = null, callback: Callback) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${BASE_URL}${address}")
            .method(method, body)
            .build()
        client.newCall(request).enqueue(callback)
    }

    inline fun <reified T> sendOkHttpRequest(
        path: String,
        method: String = GET,
        body: RequestBody? = null,
        crossinline addHeaders: (Request.Builder) -> Unit = {},
        crossinline onFailure: (String) -> Unit = {},
        crossinline onResponse: (T) -> Unit
    ) {
        val url = BASE_URL + path
        val requestBuilder = Request.Builder().url(url)

        addHeaders(requestBuilder)

        // 处理get请求
        if (method.uppercase() == GET) {
            requestBuilder.get()
        } else {
            requestBuilder.method(method, body)
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Request failed: ${e.message}")
                onMain { onFailure("Network Error") }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val text = response.body?.bytes()?.toString(Charsets.UTF_8).orEmpty()
                if (!response.isSuccessful) {
                    onMain { onFailure("HTTP $code") }
                    return
                }
                if (text.isBlank()) {
                    onMain { onFailure("Empty response") }
                    return
                }
                // 解析JSON
                val obj = DataUtils.parseJsonObj<T>(text)
                if (obj == null) {
                    onMain { onFailure("Invalid JSON") }
                    return
                }
                onMain { onResponse(obj) }
            }
        })
    }
}