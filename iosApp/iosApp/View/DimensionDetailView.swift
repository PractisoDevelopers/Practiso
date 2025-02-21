import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct DimensionDetailView : View {
    private enum ViewState {
        case pending
        case ok([QuizIntensity])
        case unavailable
    }
    
    private let libraryService = LibraryService(db: Database.shared.app)
    private let categorizeService = CategorizeServiceSync(db: Database.shared.app)
    
    let option: DimensionOption
    
    @State private var state: ViewState = .pending
    
    var body: some View {
        Group {
            switch state {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading dimension...")
                }
            case .ok(let data):
                ScrollView {
                    LazyVGrid(columns: [.init(.adaptive(minimum: 100))], spacing: 24) {
                        ForEach(data) { item in
                            Item(
                                data: item,
                                onExclude: {
                                    categorizeService.disassociate(quizId: item.quiz.id, dimensionId: option.id)
                                }
                            )
                        }
                    }
                    .padding()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .dropDestination(for: QuizOption.self) { items, _ in
                    handleQuizDrop(items: items)
                }
            case .unavailable:
                VStack {
                    Placeholder(
                        image: Image(systemName: "questionmark.circle"),
                        text: Text("Dimension not available")
                    )
                }
            }
        }
        .task(id: option.id) {
            for await data in libraryService.getQuizIntensities(dimId: option.id) {
                state = .ok(data)
            }
        }
        .navigationTitle(option.dimension.name)
    }
    
    private func handleQuizDrop(items: [QuizOption]) -> Bool {
        for item in items {
            categorizeService.associate(quizId: item.id, dimensionId: option.id)
        }
        return true
    }
}


extension DimensionDetailView {
    struct Item : View {
        let data: QuizIntensity
        let onExclude: () -> Void
        
        private var quizName: String {
            data.quiz.name ?? String(localized: "Empty question")
        }
        
        var body: some View {
            VStack {
                Image("Document")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 54, height: 54)
                Text(quizName)
                Text("\(Int((data.intensity * 100).rounded()))%")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .contextMenu {
                Button("Exclude \(quizName)", systemImage: "folder.badge.minus", role: .destructive, action: onExclude)
            } preview: {
                QuestionPreview(data: data.quiz)
            }
        }
    }
}
