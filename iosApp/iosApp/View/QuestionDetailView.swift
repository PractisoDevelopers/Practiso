import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct QuestionDetailView : View {
    enum DataState : Equatable {
        case pending
        case ok(QuizFrames)
        case unavailable
    }
    
    let option: QuizOption
    let libraryService = LibraryService(db: Database.shared.app)
    let editService = EditService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    @State private var editMode: EditMode = .inactive
    @State private var data: DataState = .pending
    @State private var staging: [Frame]? = nil
    @State private var editHistory = History()
    @State private var cache = ImageFrameView.Cache()
    @State private var titleBuffer: String
    @Namespace private var question
    
    init(option: QuizOption) {
        self.option = option
        titleBuffer = option.quiz.name ?? String(localized: "New question")
    }
    
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
                        QuestionEditor(
                            data: Binding {
                                staging ?? quizFrames.frames.map(\.frame)
                            } set: {
                                staging = $0
                            },
                            namespace: question,
                            history: $editHistory
                        )
                        .onAppear {
                            staging = quizFrames.frames.map(\.frame)
                        }
                    } else {
                        Question(
                            frames: quizFrames.frames,
                            namespace: question
                        )
                    }
                }
                .environment(\.editMode, $editMode)
                .environmentObject(cache)
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        if editMode.isEditing {
                            Button("Done") {
                                if !editHistory.isEmpty {
                                    errorHandler.catchAndShowImmediately {
                                        try editService.saveModification(data: editHistory.modifications, quizId: option.id)
                                        withAnimation {
                                            editMode = .inactive
                                        }
                                    }
                                } else {
                                    withAnimation {
                                        editMode = .inactive
                                    }
                                }
                            }
                        } else {
                            Button("Edit") {
                                editHistory = History() // editor always starts with empty history
                                withAnimation {
                                    editMode = .active
                                }
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
                    titleBuffer = qf.quiz.name ?? String(localized: "New question")
                } else {
                    data = .unavailable
                }
            }
        }
        .onChange(of: titleBuffer) { oldValue, newValue in
            errorHandler.catchAndShowImmediately {
                try editService.saveModification(data: [Modification.renameQuiz(oldName: oldValue, newName: newValue)], quizId: option.id)
            }
        }
        .navigationTitle($titleBuffer)
        .navigationBarTitleDisplayMode(.inline)
        .navigationDocument(option, preview: SharePreview(option.view.header))
    }
}
