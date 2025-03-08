import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct QuestionSelector : View {
    enum DataState {
        case pending
        case ok([QuizOption], [DimensionQuizzes])
    }
    
    let service = LibraryService(db: Database.shared.app)
    
    @Binding private var selection: SessionParameters.Selection
    var searchText: String = ""
    
    private var dataBinding: Binding<DataState>?
    @State private var ownData: DataState
    
    init(selection: Binding<SessionParameters.Selection>, searchText: String, data: Binding<DataState?>? = nil) {
        self._selection = selection
        self.searchText = searchText
        self.ownData = .pending
        
        if let data = data {
            self.dataBinding = Binding(get: {
                data.wrappedValue ?? .pending
            }, set: { newValue in
                data.wrappedValue = newValue
            })
        } else {
            self.dataBinding = nil
        }
    }
    
    var body: some View {
        Group {
            switch dataBinding?.wrappedValue ?? ownData {
            case .pending:
                VStack {
                    ProgressView()
                    Text("Loading questions...")
                }
                .foregroundStyle(.secondary)
            case .ok(let options, let dims):
                FileGrid {
                    ForEach({if searchText.isEmpty { options } else { try! options.filter(shouldInclude) }}(), id: \.id) { option in
                        Item(data: option, isOn: getIsOnBinding(quiz: option, dims: dims))
                    }
                }
            }
        }
        .task {
            var quizzes: [QuizOption]? = nil
            var dimensions: [DimensionQuizzes]? = nil
            Task {
                for await option in service.getQuizzes() {
                    let q = option.sorted(by: { $0.creationCompare > $1.creationCompare })
                    quizzes = q
                    if let dims = dimensions {
                        setData(newState: .ok(q, dims))
                    }
                }
            }
            Task {
                for await dims in service.getDimensionQuizzes() {
                    dimensions = dims
                    if let qz = quizzes {
                        setData(newState: .ok(qz, dims))
                    }
                }
            }
        }
    }
    
    private func getIsOnBinding(quiz: QuizOption, dims: [DimensionQuizzes]) -> Binding<Bool> {
        Binding {
            selection.quizIds.contains(quiz.id)
            || selection.dimensionIds.contains(where: { id in
                dims.first(where: { $0.dimension.id == id })?.quizzes.first(where: {$0.id == quiz.id}) != nil
            })
        } set: { newValue in
            if newValue {
                selection = SessionParameters.Selection(quizIds: selection.quizIds.union([quiz.id]), dimensionIds: selection.dimensionIds)
            } else {
                if selection.quizIds.contains(quiz.id) {
                    selection = SessionParameters.Selection(quizIds: selection.quizIds.subtracting([quiz.id]), dimensionIds: selection.dimensionIds)
                } else {
                    let relatedDims = selection.dimensionIds.map { id in
                        dims.first(where: {$0.dimension.id == id})
                    }.filter { q in
                        q != nil
                    } as! [DimensionQuizzes]
                    var relatedQuiz = selection.quizIds
                    for dim in relatedDims {
                        for q in dim.quizzes {
                            if q.id == quiz.id {
                                continue
                            }
                            relatedQuiz.insert(q.id)
                        }
                    }
                    selection = SessionParameters.Selection(quizIds: relatedQuiz, dimensionIds: selection.dimensionIds.subtracting(relatedDims.map(\.dimension.id)))
                }
            }
        }
    }
    
    private func setData(newState: DataState) {
        if let binding = dataBinding {
            binding.wrappedValue = newState
        } else {
            ownData = newState
        }
    }
    
    private func shouldInclude(option: QuizOption) -> Bool {
        if searchText.isEmpty {
            return true
        }
        
        return searchText.split(separator: " ").first { segment in
            option.quiz.name?.contains(segment) == true
            || option.preview?.contains(segment) == true
        } != nil
    }
    
    private struct Item : View {
        let data: QuizOption
        @Binding var isOn: Bool
        
        var body: some View {
            FileGridItem(
                title: Text(data.quiz.name ?? "New question"),
                caption: Text(Date(kt: data.quiz.creationTimeISO), format: .relative(presentation: .numeric))
            ) {
                FileIcon()
                    .contextMenu {
                        Button(isOn ? "Deselect" : "Select", systemImage: "checkmark.circle") {
                            isOn = !isOn
                        }
                    } preview: {
                        QuestionPreview(data: data.quiz)
                    }
            }
            .overlay {
                Image(systemName: isOn ? "checkmark.circle.fill" : "circle")
                    .checkmarkStyle(isEnabled: true)
            }
            .onTapGesture {
                isOn = !isOn
            }
        }
    }
}

#Preview {
    @Previewable @State var selection = SessionParameters.Selection()
    @Previewable @State var searchText = ""
    ScrollView {
        QuestionSelector(selection: $selection, searchText: searchText)
            .searchable(text: $searchText)
    }
}
