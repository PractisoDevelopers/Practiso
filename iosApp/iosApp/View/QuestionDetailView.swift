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
    
    @State private var editMode: EditMode = .inactive
    @State private var data: DataState = .pending
    @State private var staging: QuizFrames? = nil
    @State private var cache = ImageFrameView.Cache()
    @Namespace private var question

    var body: some View {
        Group {
            switch data {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading Question...")
                }
                
            case .ok(let quizFrames):
                Group {
                    if editMode.isEditing == true {
                        QuestionEditor(data: Binding {
                            staging ?? quizFrames
                        } set: {
                            staging = $0
                        }, namespace: question)
                        .onAppear {
                            staging = quizFrames
                        }
                    } else {
                        Question(data: quizFrames, namespace: question)
                    }
                }
                .environment(\.editMode, $editMode)
                .environmentObject(cache)
                .toolbar {
                    if editMode.isEditing {
                        Button("Done") {
                            withAnimation {
                                editMode = .inactive
                            }
                        }
                    } else {
                        Button("Edit") {
                            withAnimation {
                                editMode = .active
                            }
                        }
                    }
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
