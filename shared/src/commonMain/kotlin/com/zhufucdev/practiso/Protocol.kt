package com.zhufucdev.practiso

import com.zhufucdev.practiso.datamodel.AuthorizationToken
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.parseUrl

val PractisoURLProtocol = URLProtocol("practiso", 0)

class Protocol(private val url: Url) {
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
            host = "import"
            appendPathSegments(
                "auth_token",
                token.toString()
            )
        })
    }
}

sealed class ProtocolAction {
    data class ImportAuthToken(val token: AuthorizationToken) : ProtocolAction()

    companion object {
        fun of(url: Url): ProtocolAction =
            when (url.host) {
                "import" -> {
                    require(url.segments.size >= 2) {
                        "Less then 2 segments"
                    }
                    when (url.segments[0]) {
                        "auth_token" -> {
                            ImportAuthToken(
                                AuthorizationToken(
                                    value = url.segments[1]
                                )
                            )
                        }

                        else -> error("Unknown token: ${url.segments[0]}")
                    }
                }

                else -> error("Unknown token: ${url.host}")
            }
    }
}