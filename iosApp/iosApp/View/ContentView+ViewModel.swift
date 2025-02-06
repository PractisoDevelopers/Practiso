import Foundation

extension ContentView {
    enum Destination {
        case session
        case library
    }
    
    
    class ViewModel: Observable, ObservableObject {
        @Published
        var navigationPath: [Destination] = [.session]
    }
}
