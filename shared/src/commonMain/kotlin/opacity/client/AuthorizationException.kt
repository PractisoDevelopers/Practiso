package opacity.client

class AuthorizationException(
    override val message: String?,
    statusCode: Int
) : HttpStatusAssertionException(statusCode)