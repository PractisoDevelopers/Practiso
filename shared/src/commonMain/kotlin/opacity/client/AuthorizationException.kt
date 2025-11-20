package opacity.client

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

class AuthorizationException(
    override val message: String?,
    statusCode: Int
) : HttpStatusAssertionException(statusCode)

suspend fun AuthorizationException(from: HttpResponse) =
    AuthorizationException(from.bodyAsText(), from.status.value)