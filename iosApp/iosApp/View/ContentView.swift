import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @ObservedObject private var model = Model()
    @ObservedObject private var errorHandler = ErrorHandler()
    @Namespace private var namespace
    
    var body: some View {
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
                SessionView(namespace: namespace)
                    .navigationTitle("Session")
            }
        } detail: {
            switch model.detail {
            case .question(let quizOption):
                QuestionDetailView(option: quizOption)
            case .dimension(let dimensionOption):
                DimensionDetailView(option: dimensionOption)
            case .template(let templateOption):
                TemplateDetailView()
            case .none:
                Text("Select an Item to Show")
            }
        }
        .alert(
            "Operation failed",
            isPresented: errorHandler.shown
        ) {
            Button("Cancel", role: .cancel) {
                errorHandler.state = .hidden
            }
        } message: {
            switch errorHandler.state {
            case .hidden:
                EmptyView()
            case .shown(let message):
                Text(message)
            }
        }
        .overlay {
            if case .shown(let takeId, let initial) = model.answering {
                NavigationStack {
                    AnswerView(takeId: takeId, namespace: namespace, initialQuizFrames: initial)
                        .toolbar {
                            ToolbarItem(placement: .primaryAction) {
                                Button("Close") {
                                    withAnimation {
                                        model.answering = .idle
                                    }
                                }
                            }
                        }
                }
                .matchedGeometryEffect(id: takeId, in: namespace)
            }
        }
        .environmentObject(model)
        .environmentObject(errorHandler)
    }
}

#Preview {
    ContentView()
}
