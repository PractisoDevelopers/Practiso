import Foundation
import ComposeApp

extension String {
    init(appScope: AppScope) {
        switch appScope {
        case .libraryIntentModel:
            self.init(localized: "library intent model")
        case .unknown:
            self.init(localized: "unknown scope")
        }
    }
}
