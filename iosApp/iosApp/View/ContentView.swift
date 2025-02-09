import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @ObservedObject private var model = Model()
    @ObservedObject private var errorHandler = ErrorHandler()
    
    var body: some View {
        @Bindable var model = model
        @Bindable var errorHandler = errorHandler
        NavigationSplitView {
            LibraryView(destination: $model.destination)
                .navigationTitle("Library")
        } content: {
            switch model.destination {
            case .template:
                TemplateView()
                    .navigationTitle("Template")
            case .dimension:
                DimensionView()
                    .navigationTitle("Dimension")
            case .question:
                QuestionView()
                    .navigationTitle("Question")
            default:
                SessionView()
                    .navigationTitle("Session")
            }
        } detail: {
            Text("Detail here")
                .navigationTitle("Detail")
        }
        .alert(
            "Operation failed",
            isPresented: $errorHandler.shown,
            presenting: errorHandler.message
        ) { _ in
            Button("Cancel", role: .cancel) {
                errorHandler.shown = false
                errorHandler.message = nil
            }
        } message: { message in
            Text(message)
        }
        .environmentObject(model)
        .environmentObject(errorHandler)
    }
}

#Preview {
    ContentView()
}
