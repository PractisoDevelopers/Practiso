import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @ObservedObject private var model = Model()
    @ObservedObject private var errorHandler = ErrorHandler()
    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic
    @State private var preferredColumn: NavigationSplitViewColumn = .content
    @State private var takeStatData: SessionView.DataState<TakeStat> = .pending
    @State private var sessionData: SessionView.DataState<OptionImpl<SessionOption>> = .pending
    @Namespace private var namespace
    
    var body: some View {
        Group {
            if case .shown(let takeId, let initial) = model.answering {
                AnswerView(takeId: takeId, namespace: namespace, initialQuizFrames: initial)
                    .matchedGeometryEffect(id: takeId, in: namespace, isSource: true)
            } else {
                NavigationSplitView(columnVisibility: $columnVisibility, preferredCompactColumn: $preferredColumn) {
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
                        SessionView(namespace: namespace, sessions: $sessionData, takes: $takeStatData)
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
                    case .session(let sessionOption):
                        SessionDetailView(option: sessionOption)
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
            }
        }
        .environmentObject(model)
        .environmentObject(errorHandler)
    }
}

#Preview {
    ContentView()
}
