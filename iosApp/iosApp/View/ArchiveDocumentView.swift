import Foundation
import SwiftUI
import ComposeApp

struct ArchiveDocumentView : View {
    enum DataState {
        case ok([QuizDocument])
        case error(Error)
    }
    
    let data: DataState
    let url: URL
    let onClose: () -> Void
    @State private var importState: ImportState = .idle
    @State private var isImporting = false
    
    init(url: URL, onClose: @escaping () -> Void) {
        self.url = url
        self.onClose = onClose
        do {
            let questions = try DocumentService.shared.unarchive(namedSource: NamedSource(url: url))
            data = .ok(questions)
        } catch {
            data = .error(error)
        }
    }
    
    @Namespace private var internel
    
    var body: some View {
        NavigationStack {
            Group {
                switch data {
                case .ok(let questions):
                    if let question = questions.first {
                        ScrollView {
                            Question(frames: question.frames, namespace: internel)
                                .environment(\.imageService, CachedImageService(data: Dictionary(quizDoc: question)))
                                .padding(.horizontal)
                        }
                        .navigationTitle(question.name ?? String(localized: "Unnamed question"))
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItem(placement: .primaryAction) {
                                Menu("More", systemImage: "ellipsis.circle") {
                                    Button("Import Archive", systemImage: "square.and.arrow.down") {
                                        isImporting = true
                                    }
                                }
                            }
                        }
                        .importAlert(state: importState, isPresented: $isImporting)
                    } else {
                        Text("This archive is empty")
                    }
                case .error(let error):
                    Text("An error occurred and the document is failed to load")
                    Text(error.localizedDescription)
                        .foregroundStyle(.secondary)
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Home", systemImage: "house", action: onClose)
                }
            }
        }
        .task(id: isImporting) {
            if !isImporting {
                importState = .idle
                return
            }
            
            let service = ImportService(db: Database.shared.app)
            for await state in service.import(namedSource: NamedSource(url: self.url)) {
                self.importState = .init(kt: state)
            }
            isImporting = false
        }
        .onChange(of: url) { _, _ in
            isImporting = false
        }
    }
}

fileprivate extension View {
    func importAlert(state: ImportState, isPresented: Binding<Bool>) -> some View {
        return Group {
            switch state {
            case .confirmation(let total, let proceed, let cancel):
                alert("Confirmation", isPresented: Binding.constant(true)) {
                    Button("Proceed") {
                        proceed.trySend(element: nil)
                    }
                    Button("Cancel") {
                        cancel.trySend(element: nil)
                    }
                } message: {
                    Text("Would you like to import \(total) questions?")
                }
            case .error(let model, let cancel, let skip, let retry, let ignore):
                alert("Error", isPresented: Binding.constant(true)) {
                    Button("Cancel", role: .cancel) {
                        cancel.trySend(element: nil)
                    }
                    if let retry = retry {
                        Button("Retry") {
                            retry.trySend(element: nil)
                        }
                    }
                    if let skip = skip {
                        Button("Skip") {
                            skip.trySend(element: nil)
                        }
                    }
                    if let ignore = ignore {
                        Button("Ignore") {
                            ignore.trySend(element: nil)
                        }
                    }
                } message: {
                    Text("An error has occurred in \(String(appScope: model.scope)).")
                    if let err = model.exception {
                        Text(err.description())
                    }
                }
            default:
                self
            }
        }
    }
}
