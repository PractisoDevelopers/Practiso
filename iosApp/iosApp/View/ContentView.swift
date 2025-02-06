import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @ObservedObject
    var model = ViewModel()
    
    var body: some View {
        NavigationStack(path: $model.navigationPath) {
            LibraryView()
                .navigationTitle("Library")
                .navigationDestination(for: Destination.self) { destination in
                    switch destination {
                    case .session:
                        SessionView()
                            .navigationTitle("Sessions")
                    default:
                        Text("should never reach here")
                    }
                }
        }
        .environmentObject(model)
    }
}

#Preview {
    ContentView()
}
