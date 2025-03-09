import Foundation
import SwiftUI
import ComposeApp

struct AnswerView : View {
    let takeId: Int64
    let namespace: Namespace.ID
    
    @State private var data: DataState
    
    init(takeId: Int64, namespace: Namespace.ID, initialQuizFrames: [PrioritizedFrame]? = nil) {
        self.takeId = takeId
        self.namespace = namespace
        self.data = if let initial = initialQuizFrames {
            .transition(frames: initial)
        } else {
            .pending
        }
    }
    
    let service = TakeService(db: Database.shared.app)
    
    enum DataState {
        case pending
        case transition(frames: [PrioritizedFrame])
        case ok(qf: [QuizFrames], answers: [Answer])
    }
    
    var body: some View {
        Group {
            switch data {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading Take...")
                }
            case .transition(let frames):
                Page(pframes: frames, answer: [], namespace: namespace)
                    .padding(.horizontal)
                    .scrollTargetLayout()
            case .ok(let qf, let answers):
                GeometryReader { proxy in
                    ScrollView(.vertical) {
                        LazyVStack(spacing: 0) {
                            ForEach(qf, id: \.quiz.id) { qf in
                                Page(pframes: qf.frames, answer: answers.filter { $0.quizId == qf.quiz.id }, namespace: namespace)
                                    .frame(height: proxy.size.height + proxy.safeAreaInsets.top, alignment: .top)
                                    .padding(.horizontal)
                                    .padding(.bottom, proxy.safeAreaInsets.bottom)
                                    .offset(y: proxy.safeAreaInsets.top)
                            }
                        }
                        .scrollTargetLayout()
                    }
                    .scrollTargetBehavior(.paging)
                    .ignoresSafeArea(.container, edges: [.top, .bottom])
                }
            }
        }
        .task(id: takeId) {
            var qf: [QuizFrames]? = nil
            var answers: [Answer]? = nil
            
            Task {
                for await quiz in service.getQuizzes(takeId: takeId) {
                    qf = quiz
                    if let ans = answers {
                        data = .ok(qf: quiz, answers: ans)
                    }
                }
            }
            Task {
                for await ans in service.getAnswers(takeId: takeId) {
                    answers = ans
                    if let quizzes = qf {
                        data = .ok(qf: quizzes, answers: ans)
                    }
                }
            }
        }
    }
}
