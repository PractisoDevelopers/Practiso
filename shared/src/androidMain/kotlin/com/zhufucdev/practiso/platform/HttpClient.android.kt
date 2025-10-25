package com.zhufucdev.practiso.platform

import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import com.zhufucdev.practiso.SharedContext
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.NetworkUnavailableException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        config()
        engine {
            addInterceptor { chain ->
                val cm = SharedContext.getSystemService<ConnectivityManager>()
                cm?.activeNetwork ?: throw NetworkUnavailableException(AppScope.Unknown)
                chain.proceed(chain.request())
            }
        }
    }