package com.zhufucdev.practiso.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin

expect fun createHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient

val PractisoHeaderPlugin = createClientPlugin("PractisoHeaderPlugin") {
    onRequest {  req, _ ->
        req.headers.append("User-Agent", "Practiso ${getPlatform().name}")
    }
}