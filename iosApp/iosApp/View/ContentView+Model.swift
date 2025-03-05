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
        enum State {
            case hidden
            case shown(message: String)
        }
        
        @Published var state: State = .hidden
        
        var shown: Binding<Bool> {
            Binding {
                switch self.state {
                case .hidden:
                    return false
                case .shown(_):
                    return true
                }
            } set: { newValue in
                if !newValue {
                    self.state = .hidden
                }
            }

        }
        
        func show(error: Error) {
            show(message: error.localizedDescription)
        }
        
        func show(message: String) {
            state = .shown(message: message)
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
