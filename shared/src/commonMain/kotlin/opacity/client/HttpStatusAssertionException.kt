package opacity.client

open class HttpStatusAssertionException(val statusCode: Int) :
    Exception("http $statusCode")