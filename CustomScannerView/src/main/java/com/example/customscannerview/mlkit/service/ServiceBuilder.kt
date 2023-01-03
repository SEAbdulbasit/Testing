package com.example.customscannerview.mlkit.service

import com.example.customscannerview.mlkit.Environment
import com.example.customscannerview.mlkit.utils.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceBuilder {
    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.URL) // change this IP for testing by your actual machine IP
        .addConverterFactory(GsonConverterFactory.create()).client(client).build()

    fun <T> buildService(service: Class<T>): T {
        return retrofit.create(service)
    }

    fun getQAUrl(environment: Environment): String {
        return when (environment) {
            Environment.DEV -> TODO()
            Environment.PRODUCTION -> TODO()
            Environment.QA -> TODO()
            Environment.SANDBOX -> TODO()
            Environment.STAGING -> "https://v1.packagex.io/iex/api/extract"
        }
    }

    fun getUrl(environment: Environment): String {
        return when (environment) {
            Environment.DEV -> "https://dev--api.packagex.io/v1/scans"
            Environment.PRODUCTION -> "https://api.packagex.io/v1/scans"
            Environment.QA -> "https://qa--api.packagex.io/v1/scans"
            Environment.SANDBOX -> "https://sandbox--api.packagex.io/v1/scans"
            Environment.STAGING -> "https://staging--api.packagex.io/v1/scans"
        }
    }

}