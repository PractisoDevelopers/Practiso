package opacity.client

import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json

class OpacityClient(val endpoint: String, httpClientFactory: HttpClientFactory) {
    private val http = httpClientFactory.create {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun getArchiveList(
        sortOptions: SortOptions,
        predecessorId: String? = null,
    ): GetArchivesResponse {
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

    val ArchiveMetadata.resourceUrl: Url
        get() = buildUrl {
            takeFrom(endpoint)
            appendPathSegments("archive", id)
        }

    suspend fun getBonjour(): BonjourResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("bonjour")
            }
        }
        val splits = response.bodyAsText().split(' ')
        if (splits.size < 3 || splits[0] != "opacity") {
            throw IllegalStateException("Server doesn't reply a valid Bonjour response")
        }
        val properties = splits.drop(1).map(Bonjour::parseProperty)
        val version = properties.filterFirstIsInstanceOrNull<CompatibilityVersion>()
            ?: throw IllegalStateException("Server response is missing version property")
        val buildDate = properties.filterFirstIsInstanceOrNull<BuildDate>()
            ?: throw IllegalStateException("Server response is missing build date property")
        return BonjourResponse(
            version = version,
            buildDate = buildDate,
            others = properties.filterIsInstance<GenericBonjourProperty>()
        )
    }

    suspend fun getArchivePreview(archiveId: String): GetArchivePreviewResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive", archiveId)
            }
        }
        return response.body()
    }
}

