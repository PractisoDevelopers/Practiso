import ComposeApp
import Foundation
import SwiftUI
import SwiftUIPager
import Transmission

struct AnswerView<CloseButton: View>: View {
    @Environment(ContentView.ErrorHandler.self) private var errorHandler

    let takeId: Int64
    let service: TakeService
    @ViewBuilder let closeButton: () -> CloseButton

    @Binding private var data: AnswerViewDataState
    @Binding private var isGesturesEnabled: Bool
    @StateObject private var page: SwiftUIPager.Page = .first()
    @State private var buffer = Buffer()

    init(takeId: Int64, data: Binding<AnswerViewDataState>, closeButton: @escaping () -> CloseButton, isGesturesEnabled: Binding<Bool> = .constant(true)) {
        self.takeId = takeId
        self.closeButton = closeButton
        let service = TakeService(takeId: takeId, db: Database.shared.app)
        self.service = service
        _data = data
        _isGesturesEnabled = isGesturesEnabled
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
                case let .transition(qf):
                    Page(quizFrames: qf, answer: [])
                        .pageDefaults(safeAreaTop: window.safeAreaInsets.top)
                case let .ok(qf, answers, _):
                    SwiftUIPager.Pager(page: page, data: qf, id: \.quiz.id) { qf in
                        Page(quizFrames: qf, answer: answers.filter { $0.quizId == qf.quiz.id })
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
                        PanGesture(isEnabled: $isGesturesEnabled, source: [.mouse, .trackpad]) { _, translation, _ in
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
            .hideUIKitStatusBar()
            .overlay(alignment: .topTrailing) {
                closeButton()
                    .padding(max(14, window.safeAreaInsets.top - 40))
                    .scalesOnTap()
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
                let curr: Int64 = if let id = service.getCurrentQuizId()?.int64Value {
                    id
                } else {
                    -1
                }
                buffer.currQuizId = curr
                initative()
            }
            .task(id: takeId) {
                try? await service.updateAccessTime()
            }
            .environment(\.takeService, service)
        }
    }

    func initative() {
        let state = buffer.dataState()
        if case let .ok(qf, _, currentQuizId) = state {
            let firstInitativation = if case .ok = data {
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

        func dataState() -> AnswerViewDataState {
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

enum AnswerViewDataState {
    case pending
    case transition(qf: QuizFrames)
    case ok(qf: [QuizFrames], answers: [PractisoAnswer], currentQuizId: Int64)
}
