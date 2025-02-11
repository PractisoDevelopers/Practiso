import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct QuestionDetailView : View {
    enum DataState {
        case pending
        case ok(QuizFrames)
        case unavailable
    }
    
    let option: QuizOption
    let libraryService = LibraryService(db: Database.shared.app)
    
    @State private var data: DataState = .pending
    
    var body: some View {
        Group {
            switch data {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading Question...")
                }
                
            case .ok(let quizFrames):
                ScrollView {
                    Question(data: quizFrames.frames)
                }
                
            case .unavailable:
                Placeholder(image: Image(systemName: "questionmark.circle"), text: Text("Question Unavailable"))
            }
        }
        .task(id: option.id) {
            for await qf in libraryService.getQuizFrames(quizId: option.id) {
                if let qf = qf {
                    data = .ok(qf)
                } else {
                    data = .unavailable
                }
            }
        }
    }
}
