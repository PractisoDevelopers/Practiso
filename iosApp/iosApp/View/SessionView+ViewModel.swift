import Foundation
import ComposeApp

extension SessionView {
    class ViewModel: ObservableObject {
        @Published
        private(set) var sessions: [SessionOption] = []
        
        func startObserving() async {
            
        }
    }
}
