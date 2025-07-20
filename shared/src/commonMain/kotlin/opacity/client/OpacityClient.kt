package opacity.client

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json

class OpacityClient(val endpoint: String, httpClientFactory: HttpClientFactory) {
    private val http = httpClientFactory.create {
        install(ContentNegotiation) {
            json()
        }
    }
    suspend fun getArchiveList(sortOptions: SortOptions, predecessorId: String? = null): GetArchivesResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("archives")
                sortOptions.appendTo(parameters)
                predecessorId?.let { parameters.append("predecessor", it) }
            }
        }
        return response.body()
    }

    suspend fun getDimensionList(takeFirst: Int = 20): GetDimensionListResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("dimensions")
                parameter("first", takeFirst)
            }
        }
        return response.body()
    }
}

