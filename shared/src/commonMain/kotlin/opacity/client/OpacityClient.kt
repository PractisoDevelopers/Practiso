package opacity.client

import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.helper.filterFirstIsInstanceOrNull
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.buildHeaders
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import opacity.SetField

class OpacityClient(
    val endpoint: String,
    private val authToken: String? = null,
    httpClientFactory: HttpClientFactory
) {
    private val http = httpClientFactory.create {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            if (authToken != null) {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }
        }
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
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
        assertSuccess(response.status)
        return response.body()
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun getDimensionArchiveList(
        dimensionName: String,
        sortOptions: SortOptions,
        predecessorId: String? = null,
    ): GetArchivesResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("dimension", dimensionName, "archives")
                sortOptions.appendTo(parameters)
                predecessorId?.let { parameters.append("predecessor", it) }
            }
        }
        assertSuccess(response.status)
        return response.body()
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun getDimensionList(takeFirst: Int = 20): GetDimensionListResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("dimensions")
                parameter("first", takeFirst)
            }
        }
        assertSuccess(response.status)
        return response.body()
    }

    val ArchiveMetadata.resourceUrl: Url
        get() = buildUrl {
            takeFrom(endpoint)
            appendPathSegments("archive", id)
        }

    @Throws(IllegalStateException::class, CancellationException::class)
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
        val maxNameLength = properties.filterFirstIsInstanceOrNull<MaxNameLength>()
        return BonjourResponse(
            version = version,
            buildDate = buildDate,
            maxNameLength = maxNameLength,
            others = properties.filterIsInstance<GenericBonjourProperty>()
        )
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun getArchivePreview(archiveId: String): GetArchivePreviewResponse {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive", archiveId, "preview")
            }
        }
        assertSuccess(response.status)
        return response.body()
    }

    fun uploadArchive(
        content: Source,
        contentName: String,
        clientName: String? = null,
        ownerName: String? = null,
    ) = channelFlow {
        val response = http.put {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive")
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        if (clientName != null) {
                            append("\"client-name\"", clientName)
                        }
                        if (ownerName != null) {
                            append("\"owner-name\"", ownerName)
                        }
                        append(
                            "\"content\"",
                            content.readByteArray(),
                            buildHeaders {
                                append(HttpHeaders.ContentType, "application/gzip")
                                append(
                                    HttpHeaders.ContentDisposition,
                                    """filename="${contentName}.psarchive""""
                                )
                            }
                        )
                    }
                )
            )
            val stats = MutableStateFlow(TransferStats(-1, null))
            send(ArchiveUploadState.InProgress(stats))
            onUpload { bytesSentTotal, contentLength ->
                stats.emit(TransferStats(bytesSentTotal, contentLength))
            }
        }
        if (response.status.isSuccess()) {
            val success: ArchiveUploadState.Success = response.body()
            send(success)
        } else {
            val message = response.bodyAsText().takeIf(String::isNotBlank)
            send(ArchiveUploadState.Failure(statusCode = response.status, message = message))
        }
    }

    suspend fun getArchiveMetadata(archiveId: String): ArchiveMetadata? {
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive", archiveId, "metadata")
            }
        }
        if (response.status == HttpStatusCode.NotFound) {
            return null
        } else {
            val metadata: ArchiveMetadata = response.body()
            return metadata
        }
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun getWhoami(): Whoami? {
        if (authToken == null) {
            return null
        }
        val response = http.get {
            url {
                takeFrom(endpoint)
                appendPathSegments("whoami")
            }
        }
        assertSuccess(response.status)
        return response.body()
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun setWhoami(info: SetWhoami) {
        val response = http.patch {
            url {
                takeFrom(endpoint)
                appendPathSegments("whoami")
            }
            setBody(MultiPartFormDataContent(formData {
                if (info.name is SetField.Update) {
                    if (info.name.value == null) {
                        append("\"owner-name\"", "")
                    } else {
                        append("\"owner-name\"", info.name.value)
                    }
                }
                if (info.clientName is SetField.Update) {
                    append("\"client-name\"", info.clientName.value)
                }
            }))
        }
        assertSuccess(response.status)
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun deleteWhoami() {
        val response = http.delete {
            url {
                takeFrom(endpoint)
                appendPathSegments("whoami")
            }
        }
        assertSuccess(response.status)
    }

    @Throws(HttpStatusAssertionException::class, CancellationException::class)
    suspend fun forkWhoami(clientName: SetField<String>): AuthorizationToken {
        val response = http.submitForm(
            formParameters = parameters {
                if (clientName is SetField.Update) {
                    append("client-name", clientName.value)
                }
            }
        ) {
            url {
                takeFrom(endpoint)
                appendPathSegments("whoami", "fork")
            }
        }
        assertSuccess(response.status)
        val body: ForkWhoamiResponse = response.body()
        return AuthorizationToken(body.authToken)
    }

    @Throws(
        AuthorizationException::class, HttpStatusAssertionException::class,
        CancellationException::class
    )
    suspend fun deleteArchive(archiveId: String) {
        val response = http.delete {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive", archiveId)
            }
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw AuthorizationException(from = response)
        }
        assertSuccess(response.status)
    }

    @Throws(
        AuthorizationException::class,
        IllegalStateException::class,
        CancellationException::class
    )
    suspend fun like(archiveId: String) {
        val response = http.put {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive", archiveId, "like")
            }
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw AuthorizationException(from = response)
        }
        if (response.status == HttpStatusCode.Conflict) {
            throw IllegalStateException(response.bodyAsText())
        }
        assertSuccess(response.status)
    }

    @Throws(
        AuthorizationException::class,
        IllegalStateException::class,
        CancellationException::class
    )
    suspend fun removeLike(archiveId: String) {
        val response = http.delete {
            url {
                takeFrom(endpoint)
                appendPathSegments("archive", archiveId, "like")
            }
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw AuthorizationException(from = response)
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw IllegalStateException(response.bodyAsText())
        }
        assertSuccess(response.status)
    }

    private fun assertSuccess(status: HttpStatusCode) {
        if (!status.isSuccess()) {
            throw HttpStatusAssertionException(status.value)
        }
    }
}
