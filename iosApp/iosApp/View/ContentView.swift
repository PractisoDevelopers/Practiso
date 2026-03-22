import Combine
import ComposeApp
import SwiftUI

struct ContentView: View {
    @ObservedObject private var model = Model()
    @ObservedObject private var errorHandler = ErrorHandler()
    @ObservedObject private var takeStarterCache = TakeStarterDefaultLabel.Cache()
    @State private var feiState: FeiDbState? = nil
    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic

    var body: some View {
        Group {
            switch model.pathPeek {
            case .library:
                libraryApp
            case let .answer(takeId):
                answerApp(takeId)
            }
        }
        .environmentObject(model)
        .environmentObject(errorHandler)
        .alert(
            "An error occurred",
            isPresented: errorHandler.shown
        ) {
            Button("Cancel", role: .cancel) {
                errorHandler.state = .hidden
            }
        } message: {
            switch errorHandler.state {
            case .hidden:
                EmptyView()
            case let .shown(message):
                Text(message)
            }
        }
        .collect(flow: Database.shared.fei.getUpgradeState()) { latest in
            feiState = latest
        }
        .prefersStatusBarHidden(model.hideStatusBar)
    }

    func answerApp(_ takeId: Int64) -> some View {
        AnswerView(takeId: takeId, data: $model.answerData) {
            ClosePushButton {
                withAnimation {
                    _ = model.path.popLast()
                }
            }
        }
    }

    var libraryApp: some View {
        NavigationSplitView(columnVisibility: $columnVisibility, preferredCompactColumn: $model.column) {
            LibraryView(destination: $model.destination)
                .titleBar(title: "Library", feiState: feiState)
        } content: {
            Group {
                switch model.destination {
                case .template:
                    TemplateView()
                        .titleBar(title: "Template", feiState: feiState)
                case .dimension:
                    DimensionView()
                        .titleBar(title: "Dimension", feiState: feiState)
                case .question:
                    QuestionView()
                        .titleBar(title: "Question", feiState: feiState)
                default:
                    SessionView()
                        .titleBar(title: "Session", feiState: feiState)
                        .navigationBarTitleDisplayMode(.inline)
                        .environment(\.takeStarterCache, takeStarterCache)
                }
            }
        } detail: {
            Group {
                switch model.detail {
                case let .question(quizOption):
                    QuestionDetailView(option: quizOption)
                case let .dimension(dimensionOption):
                    DimensionDetailView(option: dimensionOption)
                case .template:
                    TemplateDetailView()
                case let .session(sessionOption):
                    SessionDetailView(option: sessionOption)
                case .none:
                    selectItemScreen
                }
            }
        }
    }

    var selectItemScreen: some View {
        VStack {
            Color.secondary
                .frame(width: 80, height: 80)
                .mask {
                    Image("AppIconMask")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                }
            Text("Select an Item to Show")
                .foregroundStyle(.secondary)
        }
    }
}

extension View {
    fileprivate func displaceEffect(_ displacement: CGPoint) -> some View {
        clipShape(RoundedRectangle(cornerSize: .init(width: 12, height: 12)))
            .scaleEffect(max(0.8, min(1, (500 - sqrt(pow(displacement.x, 2) + pow(displacement.y, 2))) / 500)))
    }
}

extension FeiDbState.MissingModel {
    var descriptiveMessage: String {
        let starter = if let currentModel = current {
            String(localized: "Current model doesn't support features:")
        } else {
            String(localized: "Currently no ML model is available. A model with following features is required:")
        }
        let features = missingFeatures.map { $0.description }.sorted().joined(separator: ", ")
        return "\(starter) \(features)"
    }
}

#Preview {
    ContentView()
}
