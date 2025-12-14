import Foundation
import ComposeApp

extension String {
    init(appScope: AppScope) {
        switch appScope {
        case .libraryIntentModel:
            self.init(localized: "library intent model")
        case .feiInitialization:
            self.init(localized: "frame embedding inference initialization")
        case .feiResource:
            self.init(localized: "frame embedding inference resource")
        case .downloadExecutor:
            self.init(localized: "download executor")
        case .communityService:
            self.init(localized: "community service")
        case .barcodeScanner:
            self.init(localized: "barcode scanner")
        case .unknown:
            self.init(localized: "unknown scope")
        }
    }
    
    init(errorMessage: AppMessage) {
        switch onEnum(of: errorMessage) {
        case .accountRemoved(_):
            self.init(localized: "Account removed.")
        case .barcodeNotFound(_):
            self.init(localized: "Barcode not found.")
        case .genericFailure(_):
            self.init(localized: "Generic error.")
        case .genericHttpFailure(_):
            self.init(localized: "Generic HTTP failed.")
        case .httpStatusFailure(let data):
            self.init(localized: "Unexpected HTTP status (\(data.statusCode))")
        case .httpTransactionFailure(_):
            self.init(localized: "HTTP transaction failed.")
        case .incompatibleModel(_):
            self.init(localized: "Incompatible model.")
        case .insufficientSpace(_):
            self.init(localized: "Insufficient disk space.")
        case .invalidFileFormat(_):
            self.init(localized: "Invalid file format.")
        case .networkUnavailable(_):
            self.init(localized: "Network is unavailable.")
        case .raw(let data):
            self.init(stringLiteral: data.content)
        case .resourceError(let data):
            if let location = data.location {
                if let requester = data.requester {
                    self.init(localized: "Resource \(data.resource) required by \(requester) at \(location) wsa not found.")
                } else {
                    self.init(localized: "Resource \(data.resource) at \(location) was not found.")
                }
            } else if let requester = data.requester {
                self.init(localized: "Resource \(data.resource) required by \(requester) was not found.")
            } else {
                self.init(localized: "Resource \(data.resource) was not found.")
            }
        }
    }
}
