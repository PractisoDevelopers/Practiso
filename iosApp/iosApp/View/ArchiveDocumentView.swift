import Foundation
import SwiftUI
import SwiftUIPager
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
    @State private var isImportCompletionShown = false
    @StateObject private var page: SwiftUIPager.Page = .first()
    
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
    
    
    var body: some View {
        NavigationStack {
            switch data {
            case .ok(let questions):
                if questions.count > 0 {
                    browser(questions: questions)
                } else {
                    Text("This archive is empty")
                }
            case .error(let error):
                Text("An error occurred and the document is failed to load")
                Text(error.localizedDescription)
                    .foregroundStyle(.secondary)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button("Home", systemImage: "house", action: onClose)
                        }
                    }
            }
        }
        .onChange(of: url) { _, _ in
            isImporting = false
        }
        .importAlert(state: importState, isPresented: $isImporting)
        .toolbar {
            if isImportCompletionShown {
                ToolbarItem(placement: .status) {
                    Text("Import Completed")
                        .font(.footnote)
                }
            }
        }
        .task(id: isImporting) {
            if !isImporting {
                importState = .idle
                return
            }
            isImportCompletionShown = false
            var everyImported = false
            
            let service = ImportService(db: Database.shared.app)
            for await state in service.import(namedSource: NamedSource(url: self.url)) {
                self.importState = .init(kt: state)
                if case .importing(_, _) = importState {
                    everyImported = true
                }
            }
            
            if everyImported {
                isImportCompletionShown = true
                try? await Task.sleep(for: .seconds(10))
                isImportCompletionShown = false
            }
            isImporting = false
        }
    }
    
    func browser(questions: [QuizDocument]) -> some View {
        SwiftUIPager.Pager(page: page, data: questions.indices, id: \.self) { index in
            QuizDocumentView(data: questions[index])
                .frame(maxHeight: .infinity, alignment: .top)
                .background()
        }
        .vertical()
        .alignment(.start)
        .singlePagination(sensitivity: .high)
        .interactive(opacity: 0.8)
        .gesture(
            PanGesture(source: [.mouse, .trackpad]) { location, translation, velocity in
                if abs(translation.y) > 100 {
                    withAnimation {
                        if translation.y < 0 {
                            page.update(.next)
                        } else {
                            page.update(.previous)
                        }
                    }
                    return true
                }
                return false
            }
        )
        .navigationTitle(questions[page.index].name ?? String(localized: "Unnamed question"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu("More", systemImage: "ellipsis.circle") {
                    Button("Import Archive", systemImage: "square.and.arrow.down") {
                        isImporting = true
                    }
                }
            }
            ToolbarItem(placement: .topBarLeading) {
                Button("Home", systemImage: "house", action: onClose)
            }
        }
    }
    
    private struct QuizDocumentView : View {
        let data: QuizDocument
        @Namespace private var internel

        var body: some View {
            Question(frames: data.frames, namespace: internel)
                .environment(\.imageService, CachedImageService(data: Dictionary(quizDoc: data)))
                .padding(.horizontal)
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
            case .importing(let total, let done):
                toolbar {
                    ToolbarItem(placement: .status) {
                        Text("Importing \(total) items, \(done) completed...")
                            .font(.footnote)
                    }
                }
            default:
                self
            }
        }
    }
}
