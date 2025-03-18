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
    
    @State private var data: DataState
    @StateObject private var page: SwiftUIPager.Page = .first()
    
    init(takeId: Int64, namespace: Namespace.ID, initialQuizFrames: QuizFrames? = nil) {
        self.takeId = takeId
        self.namespace = namespace
        let service = TakeService(takeId: takeId, db: Database.shared.app)
        self.service = service
        self.data = if let initial = initialQuizFrames {
            .transition(qf: initial)
        } else {
            .pending
        }
    }
    
    enum DataState {
        case pending
        case transition(qf: QuizFrames)
        case ok(qf: [QuizFrames], answers: [PractisoAnswer])
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
                    Page(quizFrames: qf, answer: [], service: service, namespace: namespace)
                        .pageDefaults(proxy: window, safeAreaTop: window.safeAreaInsets.top)
                case .ok(let qf, let answers):
                    SwiftUIPager.Pager(page: page, data: qf, id: \.quiz.id) { qf in
                        Page(quizFrames: qf, answer: answers.filter { $0.quizId == qf.quiz.id }, service: service, namespace: namespace)
                            .pageDefaults(proxy: window, safeAreaTop: window.safeAreaInsets.top)
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
                }
            }
            .background()
            .statusBarHidden()
            .matchedGeometryEffect(id: takeId, in: namespace, isSource: true)
            .overlay(alignment: .topTrailing) {
                ClosePushButton()
                    .padding(max(16, window.safeAreaInsets.top - 28))
                    .ignoresSafeArea()
                    .scalesOnTap()
                    .onTapGesture {
                        withAnimation {
                            contentModel.answering = .idle
                        }
                    }
            }
            .task(id: takeId) {
                var qf: [QuizFrames]? = nil
                var answers: [PractisoAnswer]? = nil
                var currQuizId: Int64? = nil
                
                @MainActor
                func initative(quiz: [QuizFrames], ans: [PractisoAnswer], currId: Int64) {
                    data = .ok(qf: quiz, answers: ans)
                    page.index = if currId >= 0 {
                        quiz.firstIndex(where: { $0.quiz.id == currId }) ?? 0
                    } else {
                        0
                    }
                }
                
                Task {
                    for await quiz in service.getQuizzes() {
                        qf = quiz
                        if let ans = answers, let curr = currQuizId {
                            initative(quiz: quiz, ans: ans, currId: curr)
                        }
                    }
                }
                Task {
                    for await ans in service.getAnswers() {
                        answers = ans
                        if let quizzes = qf, let curr = currQuizId {
                            initative(quiz: quizzes, ans: ans, currId: curr)
                        }
                    }
                }
                Task {
                    let curr: Int64 = if let id = (try? await service.getCurrentQuizId())?.int64Value {
                        id
                    } else {
                        -1
                    }
                    currQuizId = curr
                    if let quizzes = qf, let ans = answers {
                        initative(quiz: quizzes, ans: ans, currId: curr)
                    }
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
}

extension View {
    fileprivate func pageDefaults(proxy: GeometryProxy, safeAreaTop: Double) -> some View {
        padding()
            .offset(y: proxy.safeAreaInsets.top + (safeAreaTop < 56 ? 56 : 0))
            .frame(maxHeight: .infinity, alignment: .top)
    }
}
