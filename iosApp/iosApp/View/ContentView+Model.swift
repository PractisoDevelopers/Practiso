import Foundation
import SwiftUI
import ComposeApp

extension ContentView {
    class Model: Observable, ObservableObject {
        @Published var destination: Destination? = .session
        
    }
    
    class ErrorHandler: Observable, ObservableObject {
        @Published var shown = false
        @Published var message: String?
        
        func catchAndShowImmediately<T>(action: () async throws -> T) async -> T? {
            do {
                return try await action()
            } catch {
                message = error.localizedDescription
                shown = true
                return nil
            }
        }
        
        func catchAndShowImmediately<T>(action: () throws -> T) -> T? {
            do {
                return try action()
            } catch {
                message = error.localizedDescription
                shown = true
                return nil
            }
        }
    }
}
