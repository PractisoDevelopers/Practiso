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
    @Namespace private var question
    
    init(option: QuizOption) {
        self.option = option
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
                
            case .unavailable:
                Placeholder(image: Image(systemName: "questionmark.circle"), text: Text("Question Unavailable"))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task(id: option.id) {
            alteredTitled = nil
            for await qf in libraryService.getQuizFrames(quizId: option.id) {
                if let qf = qf {
                    data = .ok(qf)
                } else {
                    data = .unavailable
                }
            }
        }
        .onChange(of: editMode) { oldValue, newValue in
            onEditModeChange(oldValue: oldValue, newValue: newValue)
        }
    }
    
    func onEditModeChange(oldValue: EditMode, newValue: EditMode) {
        if newValue != .inactive {
            return
        }
        Task {
            if let newData = await libraryService.getQuizFrames(quizId: option.id)
                .makeAsyncIterator()
                .next() {
                if let present = newData {
                    data = .ok(present)
                }
            }
        }
    }
    
    @State private var alteredTitled: String?
    private var titleBinding: Binding<String> {
        Binding {
            alteredTitled ?? option.quiz.name ?? String(localized: "New question")
        } set: { newValue in
            errorHandler.catchAndShowImmediately {
                try editService.saveModification(data: [Modification.renameQuiz(oldName: option.quiz.name, newName: newValue)], quizId: option.id)
                alteredTitled = newValue
            }
        }
    }
}
