import Foundation
import SwiftUI
import ComposeApp

extension ContentView {
    class Model: Observable, ObservableObject {
        @Published var destination: Destination? = .session
        @Published var detail: Detail?
    }
    
    @MainActor
    class ErrorHandler: Observable, ObservableObject {
        @Published var shown = false
        @Published var message: String?
        
        func show(error: Error) {
            show(message: error.localizedDescription)
        }
        
        func show(message: String) {
            self.message = message
            shown = true
        }
        
        func catchAndShowImmediately<T>(action: @MainActor () async throws -> T) async -> T? {
            do {
                return try await action()
            } catch {
                show(message: error.localizedDescription)
                return nil
            }
        }
        
        func catchAndShowImmediately<T>(action: () throws -> T) -> T? {
            do {
                return try action()
            } catch {
                show(message: error.localizedDescription)
                return nil
            }
        }
    }
}
