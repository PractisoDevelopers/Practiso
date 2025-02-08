import Foundation
import ComposeApp

extension ContentView {
    class Model: Observable, ObservableObject {
        @Published
        var destination: Destination? = .session
    }
}
