package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.AuthorizationToken
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.parseUrl
import kotlin.jvm.JvmStatic

val PractisoURLProtocol = URLProtocol("practiso", 0)

class Protocol(private val url: Url) {
    @Throws(IllegalArgumentException::class)
    constructor(urlString: String) : this(
        parseUrl(urlString) ?: throw IllegalArgumentException("urlString is not an valid URL")
    )

    val action = ProtocolAction.of(url)

    init {
        require(url.protocol == PractisoURLProtocol) { "Unknown protocol: ${url.protocol.name}" }
    }

    override fun toString(): String = url.toString()

    companion object {
        fun isValid(urlString: String) =
            parseUrl(urlString)?.let { it.protocol == PractisoURLProtocol } == true

        fun importAuthToken(token: AuthorizationToken) = Protocol(buildUrl {
            protocol = PractisoURLProtocol
            host = "import_auth_token"
            appendPathSegments(token.toString())
        })

        fun revealCommunityArchive(id: String?) = Protocol(buildUrl {
            protocol = PractisoURLProtocol
            host = "community"
            appendPathSegments("archive")
            if (id != null) {
                appendPathSegments(id)
            }
        })
    }
}

sealed class ProtocolAction {
    data class ImportAuthToken(val token: AuthorizationToken) : ProtocolAction() {
        // for Apple platforms
        val tokenString: String get() = token.toString()
    }
    data class RevealCommunityArchive(val id: String?) : ProtocolAction()

    companion object {
        @Throws(IllegalArgumentException::class)
        fun of(url: Url): ProtocolAction {
            when (url.host) {
                "import_auth_token" -> {
                    require(url.segments.isNotEmpty()) {
                        "No token data to import"
                    }

                    return ImportAuthToken(
                        AuthorizationToken(
                            value = url.segments[0]
                        )
                    )
                }

                "community" -> {
                    when (url.segments.firstOrNull()) {
                        "archive" -> {
                            return RevealCommunityArchive(url.segments.getOrNull(2))
                        }
                    }
                }
            }

            throw IllegalArgumentException("Unknown token: ${url.host}")
        }
    }
}