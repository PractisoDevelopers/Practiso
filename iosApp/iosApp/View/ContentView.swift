import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @State
    private var model = Model()
    
    @State var splitVisibility = NavigationSplitViewVisibility.doubleColumn
    
    var body: some View {
        @Bindable var model = model
        NavigationSplitView(columnVisibility: $splitVisibility) {
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
            Text("Details here")
        }
        .environmentObject(model)
    }
}

#Preview {
    ContentView()
}
