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
                    case .dimension:
                        OptionListView(data: model.dimensions)
                            .navigationTitle("Dimensions")
                    case .question:
                        OptionListView(data: model.quizzes)
                            .navigationTitle("Questions")
                    case .template:
                        OptionListView(data: model.templates)
                            .navigationTitle("Templates")
                    default:
                        Text("should never reach here")
                    }
                }
        }
        .environmentObject(model)
        .task {
            await model.startObserving()
        }
    }
}

#Preview {
    ContentView()
}
