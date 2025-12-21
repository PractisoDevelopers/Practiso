@preconcurrency import ComposeApp
import Foundation
import SwiftUI

struct QuestionDetailView: View {
    let option: QuizOption
    let libraryService = LibraryService(db: Database.shared.app)
    let editService = EditService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler

    @State private var quizFramesFlow: SkieSwiftOptionalFlow<QuizFrames>

    @State private var editMode: EditMode = .inactive
    @State private var isCategorizeSheetShown = false
    @State private var isRenamingDialogShown = false
    @State private var renamingBuffer = ""
    @State private var staging: [Frame]? = nil
    @State private var editHistory = History()
    @Namespace private var question

    init(option: QuizOption) {
        self.option = option
        quizFramesFlow = libraryService.getQuizFrames(quizId: option.id)
    }

    var body: some View {
        Delay { _ in
            Observing(quizFramesFlow) {
                VStack {
                    ProgressView()
                    Text("Loading question...")
                }
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
                    } else {
                        ScrollView {
                            Question(
                                frames: quizFrames.frames,
                                namespace: question
                            )
                        }
                        .padding(.horizontal)
                        .navigationDocument(option, preview: SharePreview(option.view.header))
                        .categorizeSheet(isPresent: $isCategorizeSheetShown, quizId: option.quiz.id)
                    }
                } else {
                    Placeholder {
                        Image(systemName: "questionmark.circle")
                    } content: {
                        Text("Question Unavailable")
                    }
                }
            }
            .alert("Rename", isPresented: $isRenamingDialogShown) {
                TextField("Question name", text: $renamingBuffer)
                Button("OK") {
                    isRenamingDialogShown = false
                    titleBinding.wrappedValue = renamingBuffer
                }
                Button("Cancel", role: .cancel) {
                    isRenamingDialogShown = false
                }
            }
            .toolbar {
                if editMode.isEditing == true {
                    ToolbarItem(placement: .confirmationAction) {
                        EditButton()
                            .environment(\.editMode, $editMode)
                            .onTapGesture {
                                if !editHistory.isEmpty {
                                    errorHandler.catchAndShowImmediately {
                                        try editService.saveModification(data: editHistory.modifications, quizId: option.id)
                                    }
                                }
                            }
                    }
                } else {
                    ToolbarItem(placement: .bottomBar) {
                        Button("Categorize", systemImage: "tag") {
                            isCategorizeSheetShown = true
                        }
                    }
                    ToolbarItem(placement: .bottomBar) {
                        ShareLink(item: option, preview: SharePreview(option.view.header))
                    }
                    ToolbarItem(placement: .primaryAction) {
                        EditButton()
                            .environment(\.editMode, $editMode)
                    }
                    ToolbarItem(placement: .bottomBar) {
                        RenameButton()
                            .renameAction {
                                isRenamingDialogShown = true
                                renamingBuffer = titleBinding.wrappedValue
                            }
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .navigationTitle(titleBinding)
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
        
        editHistory = History() // editor always starts with empty history
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
