package opacity.client

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface HttpClientFactory {
    fun create(config: HttpClientConfig<*>.() -> Unit): HttpClient
}