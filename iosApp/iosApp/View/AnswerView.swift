import Foundation
import SwiftUI
import ComposeApp
import SwiftUIPager

struct AnswerView : View {
    @Environment(ContentView.Model.self) private var contentModel
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    let takeId: Int64
    let namespace: Namespace.ID
    let service: TakeService
    let isGesturesEnabled: Bool
    
    @Binding private var data: DataState
    @StateObject private var page: SwiftUIPager.Page = .first()
    @State private var buffer = Buffer()
    
    init(takeId: Int64, namespace: Namespace.ID, data: Binding<DataState>, isGesturesEnabled: Bool = true) {
        self.takeId = takeId
        self.namespace = namespace
        let service = TakeService(takeId: takeId, db: Database.shared.app)
        self.service = service
        self._data = data
        self.isGesturesEnabled = isGesturesEnabled
    }
    
    enum DataState {
        case pending
        case transition(qf: QuizFrames)
        case ok(qf: [QuizFrames], answers: [PractisoAnswer], currentQuizId: Int64)
    }
    
    var body: some View {
        GeometryReader { window in
            ZStack {
                switch data {
                case .pending:
                    VStack {
                        ProgressView()
                        Text("Loading Take...")
                    }
                case .transition(let qf):
                    Page(quizFrames: qf, answer: [], namespace: namespace)
                        .pageDefaults(safeAreaTop: window.safeAreaInsets.top)
                case .ok(let qf, let answers, _):
                    SwiftUIPager.Pager(page: page, data: qf, id: \.quiz.id) { qf in
                        Page(quizFrames: qf, answer: answers.filter { $0.quizId == qf.quiz.id }, namespace: namespace)
                            .pageDefaults(safeAreaTop: window.safeAreaInsets.top)
                            .background()
                    }
                    .vertical()
                    .alignment(.start)
                    .singlePagination(sensitivity: .high)
                    .interactive(opacity: 0.8)
                    .onPageChanged { index in
                        if index < qf.count {
                            dbUpdateCurrentQuiz(quizId: qf[index].quiz.id)
                        }
                    }
                    .gesture(
                        PanGesture(isEnabled: isGesturesEnabled)
                            .source([.mouse, .trackpad])
                            .onChange { location, translation, velocity in
                                if abs(translation.y) > 100 {
                                    withAnimation {
                                        if translation.y < 0 {
                                            page.update(.next)
                                        } else {
                                            page.update(.previous)
                                        }
                                        dbUpdateCurrentQuiz(quizId: qf[page.index].quiz.id)
                                    }
                                    return true
                                }
                                return false
                            }
                    )
                }
            }
            .background()
            .statusBarHidden()
            .matchedGeometryEffect(id: takeId, in: namespace, isSource: true)
            .overlay(alignment: .topTrailing) {
                ClosePushButton()
                    .padding(max(16, window.safeAreaInsets.top - 28))
                    .scalesOnTap(scale: 0.9)
                    .onTapGesture {
                        withAnimation {
                            _ = contentModel.path.popLast()
                        }
                    }
                    .ignoresSafeArea()
            }
            .overlay(alignment: .bottom) {
                AnswerView.Timer(takeId: takeId)
                    .padding(.bottom, 20)
            }
            .task(id: takeId) {
                for await quiz in service.getQuizzes() {
                    buffer.qf = quiz
                    initative()
                }
            }
            .task(id: takeId) {
                for await ans in service.getAnswers() {
                    buffer.answers = ans
                    initative()
                }
            }
            .task(id: takeId) {
                let curr: Int64 = if let id = (try? await service.getCurrentQuizId())?.int64Value {
                    id
                } else {
                    -1
                }
                buffer.currQuizId = curr
                initative()
            }
            .environment(\.takeService, service)
        }
    }
    
    func initative() {
        let state = buffer.dataState()
        if case .ok(let qf, _, let currentQuizId) = state {
            let firstInitativation = if case .ok(_, _, _) = data {
                false
            } else {
                true
            }
            data = state
            if firstInitativation {
                page.index = if currentQuizId >= 0 {
                    qf.firstIndex(where: { $0.quiz.id == currentQuizId }) ?? 0
                } else {
                    0
                }
            }
        }
    }

    func dbUpdateCurrentQuiz(quizId: Int64) {
        Task {
            await errorHandler.catchAndShowImmediately {
                try await service.updateCurrentQuizId(currentQuizId: quizId)
            }
        }
    }
    
    private struct Buffer {
        var qf: [QuizFrames]? = nil
        var answers: [PractisoAnswer]? = nil
        var currQuizId: Int64? = nil
        
        func dataState() -> DataState {
            if let qf = qf, let ans = answers, let currQuizId = currQuizId {
                .ok(qf: qf, answers: ans, currentQuizId: currQuizId)
            } else {
                .pending
            }
        }
    }
}

extension View {
    fileprivate func pageDefaults(safeAreaTop: Double) -> some View {
        padding()
            .offset(y: safeAreaTop < 56 ? 56 : 0)
            .frame(maxHeight: .infinity, alignment: .top)
    }
}
