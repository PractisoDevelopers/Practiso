import Foundation
import SwiftUI
import ComposeApp

struct QuestionPreview : View {
    let data: Quiz
    private let errorHandler = ContentView.ErrorHandler()
    private let libraryService = LibraryService(db: Database.shared.app)
    
    enum DataState : Equatable {
        case pending
        case ok([PrioritizedFrame])
        case unavailable
    }
    
    @State private var state: DataState = .pending
    @Namespace private var internel

    var body: some View {
        Group {
            switch state {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading Question...")
                }
                
            case .ok(let frames):
                Question(
                    frames: frames,
                    namespace: internel
                )

            case .unavailable:
                Placeholder(image: Image(systemName: "questionmark.circle"), text: Text("\(data.name ?? "Qeustion") Unavailable"))
            }
        }
        .task {
            for await qf in libraryService.getQuizFrames(quizId: data.id) {
                if let qf = qf {
                    state = .ok(qf.frames)
                } else {
                    state = .unavailable
                }
            }
        }
    }
}
