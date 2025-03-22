import UIKit
import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @Namespace private var namespace
    @Namespace private var internel
    
    @ObservedObject private var model = Model()
    @ObservedObject private var errorHandler = ErrorHandler()
    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic
    @State private var preferredColumn: NavigationSplitViewColumn = .content
    @State private var takeStatData: SessionView.DataState<TakeStat> = .pending
    @State private var sessionData: SessionView.DataState<OptionImpl<SessionOption>> = .pending
    
    var body: some View {
        app(topLevel: model.pathPeek)
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
        .environmentObject(model)
        .environmentObject(errorHandler)
    }
    
    func app(topLevel: TopLevel) -> some View {
        Group {
            switch topLevel {
            case .library:
                libraryApp
            case .answer(let takeId):
                AnswerView(takeId: takeId, namespace: namespace, data: $model.answerData)
            }
        }
    }
 
    var libraryApp: some View {
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
                SessionDetailView(option: sessionOption, namespace: namespace)
            case .none:
                Text("Select an Item to Show")
            }
        }
    }
}

extension View {
    fileprivate func displaceEffect(_ displacement: CGPoint) -> some View {
        clipShape(RoundedRectangle(cornerSize: .init(width: 12, height: 12)))
        .scaleEffect(max(0.8, min(1, (500 - sqrt(pow(displacement.x, 2) + pow(displacement.y, 2))) / 500)))
    }
}

#Preview {
    ContentView()
}
