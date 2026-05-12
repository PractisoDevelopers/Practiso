import Foundation

@Observable
class ArchiveResourceInfo {
    var resource: Result<ArchiveResource, ResourceError>? = nil
}

enum ArchiveResource: Equatable {
    case url(URL)
    case data(Data)
}

struct ResourceError: LocalizedError, Equatable {
    var errorDescription: String?
    
    init(_ err: any Error) {
        self.errorDescription = err.localizedDescription
    }
}
