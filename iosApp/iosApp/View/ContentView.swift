import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @ObservedObject private var model = Model()
    
    var body: some View {
        @Bindable var model = model
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
        .environmentObject(model)
    }
}

#Preview {
    ContentView()
}
