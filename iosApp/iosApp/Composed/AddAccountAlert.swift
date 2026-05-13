import AsyncAlgorithms
import ComposeApp
import Foundation
import Shared
import SwiftUI

enum AddAccountState: Sendable {
    case working
    case success
    case error(AddAccountError)
}

enum AddAccountError: LocalizedError {
    case invalidIdentity
    case http(status: Int32)

    var errorDescription: String? {
        switch self {
        case .invalidIdentity:
            String(localized: "The credentials given are invalid")
        case let .http(status):
            String(localized: "Remote server responded with HTTP status code \(status)")
        }
    }
}

extension View {
    func addAccountAlert(_ token: Binding<String?>, stateNotifier: AsyncChannel<AddAccountState>) -> some View {
        alert("Add account", isPresented: token.isNotNil()) {
            Button("Use As Is") {
                guard let token = token.wrappedValue else {
                    return
                }
                Task {
                    await stateNotifier.send(.working)
                    try! await AppCommunityService.shared.setIdentity(token: token)
                    await stateNotifier.send(.success)
                }
            }
            Button("As New Device") {
                guard let token = token.wrappedValue else {
                    return
                }
                Task {
                    await stateNotifier.send(.working)
                    do {
                        try await AppCommunityService.shared.setIdentityAsNewDevice(token: token, clientName: SetFieldUpdateString(value: "\(DeviceIdentifier.default)"))
                        await stateNotifier.send(.success)
                    } catch let error as HttpStatusAssertionException {
                        switch error.statusCode {
                        case 403:
                            await stateNotifier.send(.error(.invalidIdentity))
                        default:
                            await stateNotifier.send(.error(.http(status: error.statusCode)))
                        }
                    }
                    stateNotifier.finish()
                }
            }
            Button("Cancel", role: .cancel) {
                token.wrappedValue = nil
            }
        } message: {
            if AppCommunityService.shared.isAuthenticated() {
                Text("Existing credentials would be overwritten, and you would have access no longer")
            }
        }
    }
}
