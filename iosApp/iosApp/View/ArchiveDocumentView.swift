import Foundation
import SwiftUI
import ComposeApp

struct ArchiveDocumentView : View {
    enum State {
        case ok([QuizDocument])
        case error(Error)
    }
    
    let state: State
    let url: URL
    let onClose: () -> Void
    init(url: URL, onClose: @escaping () -> Void) {
        self.url = url
        self.onClose = onClose
        do {
            let questions = try DocumentService.shared.unarchive(importable: Importable(url: url))
            state = .ok(questions)
        } catch {
            state = .error(error)
        }
    }
    
    @Namespace private var internel
    var body: some View {
        NavigationStack {
            Group {
                switch state {
                case .ok(let questions):
                    if let question = questions.first {
                        ScrollView {
                            Question(frames: question.frames, namespace: internel)
                                .environment(\.imageService, CachedImageService(data: Dictionary(quizDoc: question)))
                        }
                        .navigationTitle(question.name ?? String(localized: "Unnamed question"))
                        .navigationBarTitleDisplayMode(.inline)
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
    }
}
