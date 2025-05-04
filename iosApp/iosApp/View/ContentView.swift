import SwiftUI
import ComposeApp
import Combine

struct ContentView: View {
    @Namespace private var namespace
    @Namespace private var internel
    
    @ObservedObject private var model = Model()
    @ObservedObject private var errorHandler = ErrorHandler()
    @ObservedObject private var takeStarterCache = TakeStarter.Cache()
    @State private var feiState: FeiDbState? = nil
    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic
    @State private var preferredColumn: NavigationSplitViewColumn = .content
    @State private var takeStatData: SessionView.DataState<TakeStat> = .pending
    @State private var sessionData: SessionView.DataState<OptionImpl<SessionOption>> = .pending
    @State private var appScale = 1.0
    @State private var isBackGestureActivated = false

    var body: some View {
        GeometryReader { window in
            ZStack {
                app(topLevel: model.pathPeek)
                    .animation(.easeInOut, value: appScale)
                    .overlay {
                        if appScale < 1 {
                            RoundedRectangle(cornerRadius: (1 - appScale) * 12 + 20)
                                .stroke(lineWidth: 0.8)
                                .fill(.foreground.opacity(0.8))
                        } else {
                            EmptyView()
                        }
                    }
                    .scaleEffect(appScale)
            }
            .gesture(backGesture(containerWidth: window.size.width))
        }
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
            case .shown(let message):
                Text(message)
            }
        }
        .task {
            for await state in Database.shared.fei.getUpgradeState() {
                feiState = state
            }
        }
    }
    
    func backGesture(containerWidth: Double) -> PanGesture {
        PanGesture { location, translation, velocity in
            if model.path.isEmpty {
                return false
            }
            if !isBackGestureActivated && translation.x > 1 && translation.x > abs(translation.y) || isBackGestureActivated {
                appScale = min(1, 1 - translation.x / containerWidth)
                isBackGestureActivated = true
            }
            return false
        } end: {
            if appScale < 0.92 {
                withAnimation {
                    _ = model.path.popLast()
                    appScale = 1
                    isBackGestureActivated = false
                }
            } else {
                withAnimation {
                    appScale = 1
                    isBackGestureActivated = false
                }
            }
        }
    }
    
    func app(topLevel: TopLevel) -> some View {
        Group {
            switch topLevel {
            case .library:
                libraryApp
            case .answer(let takeId):
                AnswerView(takeId: takeId, namespace: namespace, data: $model.answerData, isGesturesEnabled: Binding(get: {
                    !isBackGestureActivated
                }, set: { _ in
                }))
            }
        }
        .environmentObject(model)
        .environmentObject(errorHandler)
    }
    
    var libraryApp: some View {
        NavigationSplitView(columnVisibility: $columnVisibility, preferredCompactColumn: $preferredColumn) {
            LibraryView(destination: $model.destination)
                .navigationTitle("Library")
                .onAppear {
                    preferredColumn = .sidebar
                }
        } content: {
            Group {
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
                        .environment(\.takeStarterCache, takeStarterCache)
                }
            }
            .onAppear {
                preferredColumn = .content
            }
            .modifier(StatusBarModifier(feiState: feiState))
        } detail: {
            Group {
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
            .onAppear {
                preferredColumn = .detail
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

extension FeiDbState.MissingModel {
    var descriptiveMessage: String {
        let starter = if let currentModel = self.current {
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
