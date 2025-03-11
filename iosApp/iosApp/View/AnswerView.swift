import Foundation
import SwiftUI
import ComposeApp

struct AnswerView : View {
    @Environment(ContentView.Model.self) private var contentModel
    
    let takeId: Int64
    let namespace: Namespace.ID
    let service: TakeService
    
    @State private var data: DataState
    
    init(takeId: Int64, namespace: Namespace.ID, initialQuizFrames: QuizFrames? = nil) {
        self.takeId = takeId
        self.namespace = namespace
        self.service = TakeService(takeId: takeId, db: Database.shared.app)
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
                Group {
                    switch data {
                    case .pending:
                        VStack {
                            ProgressView()
                            Text("Loading Take...")
                        }
                    case .transition(let qf):
                        Page(quizFrames: qf, answer: [], service: service, namespace: namespace)
                            .padding()
                    case .ok(let qf, let answers):
                        GeometryReader { proxy in
                            ScrollView(.vertical) {
                                LazyVStack(spacing: 0) {
                                    ForEach(qf, id: \.quiz.id) { qf in
                                        Page(quizFrames: qf, answer: answers.filter { $0.quizId == qf.quiz.id }, service: service, namespace: namespace)
                                    }
                                    .padding()
                                    .frame(height: proxy.size.height + proxy.safeAreaInsets.top, alignment: .top)
                                    .padding(.bottom, proxy.safeAreaInsets.bottom)
                                    .offset(y: proxy.safeAreaInsets.top + (window.safeAreaInsets.top < 56 ? 56 : 0))
                                }
                                .scrollTargetLayout()
                            }
                            .scrollTargetBehavior(.paging)
                            .ignoresSafeArea(.container, edges: [.top, .bottom])
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
                    .onTapGesture {
                        withAnimation {
                            contentModel.answering = .idle
                        }
                    }
            }
            .task(id: takeId) {
                var qf: [QuizFrames]? = nil
                var answers: [PractisoAnswer]? = nil
                
                Task {
                    for await quiz in service.getQuizzes() {
                        qf = quiz
                        if let ans = answers {
                            data = .ok(qf: quiz, answers: ans)
                        }
                    }
                }
                Task {
                    for await ans in service.getAnswers() {
                        answers = ans
                        if let quizzes = qf {
                            data = .ok(qf: quizzes, answers: ans)
                        }
                    }
                }
            }
        }
    }
}
