package opacity.client

import io.ktor.http.ParametersBuilder

data class SortOptions(val descending: Boolean = true, val keyword: Keyword = Keyword.UploadTime) {
    fun appendTo(parameters: ParametersBuilder) {
        parameters.append("by", keyword.queryParameter)
        parameters.append("order", if (descending) "desc" else "asc")
    }
}

enum class Keyword(val queryParameter: String) {
    Name("name"), Likes("likes"), UpdateTime("update"), UploadTime("upload")
}

