import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct QuestionDetailView : View {
    let option: QuizOption
    let libraryService = LibraryService(db: Database.shared.app)
    let editService = EditService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    @State private var quizFramesFlow: SkieSwiftOptionalFlow<QuizFrames>
    
    @State private var editMode: EditMode = .inactive
    @State private var staging: [Frame]? = nil
    @State private var editHistory = History()
    @Namespace private var question
    
    init(option: QuizOption) {
        self.option = option
        self.quizFramesFlow = libraryService.getQuizFrames(quizId: option.id)
    }
    
    var body: some View {
        Observing(quizFramesFlow) {
            VStack {
                ProgressView()
                Text("Loading Question...")
            }
            .navigationTitle(titleBinding)
        } content: { qf in
            if let quizFrames = qf {
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
                    .navigationTitle(titleBinding)
                    .toolbar {
                        ToolbarItem {
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
                        }
                    }
                } else {
                    ScrollView {
                        Question(
                            frames: quizFrames.frames,
                            namespace: question
                        )
                    }
                    .padding(.horizontal)
                    .navigationTitle(titleBinding.wrappedValue)
                    .navigationDocument(option, preview: SharePreview(option.view.header))
                    .toolbar {
                        ToolbarItem {
                            Button("Edit") {
                                editHistory = History() // editor always starts with empty history
                                withAnimation {
                                    editMode = .active
                                }
                            }
                        }
                    }
                }
            } else {
                Placeholder {
                    Image(systemName: "questionmark.circle")
                } content: {
                    Text("Question Unavailable")
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            alteredTitle = nil
            quizFramesFlow = libraryService.getQuizFrames(quizId: option.id)
        }
        .onChange(of: option) { _, newValue in
            quizFramesFlow = libraryService.getQuizFrames(quizId: newValue.id)
        }
        .onChange(of: editMode) { oldValue, newValue in
            onEditModeChange(oldValue: oldValue, newValue: newValue)
        }
    }
    
    func onEditModeChange(oldValue: EditMode, newValue: EditMode) {
        if newValue != .inactive {
            return
        }
    }
    
    @State private var alteredTitle: String?
    private var titleBinding: Binding<String> {
        Binding {
            alteredTitle ?? option.quiz.name ?? String(localized: "New question")
        } set: { newValue in
            errorHandler.catchAndShowImmediately {
                try editService.saveModification(data: [Modification.renameQuiz(oldName: option.quiz.name, newName: newValue)], quizId: option.id)
                alteredTitle = newValue
            }
        }
    }
}
