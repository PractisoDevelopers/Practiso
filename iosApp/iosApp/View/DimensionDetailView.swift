import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct DimensionDetailView : View {
    private let libraryService = LibraryService(db: Database.shared.app)
    private let categorizeService = CategorizeServiceSync(db: Database.shared.app)
    private let clusterService = ClusterService(db: Database.shared.app, fei: Database.shared.fei)
    
    let option: DimensionOption
    
    @State private var currentPopoverItem: QuizIntensity? = nil
    
    @State private var dimensionFlow: SkieSwiftOptionalFlow<ComposeApp.Dimension>
    @State private var quizzesFlow: SkieSwiftFlow<[QuizIntensity]>
    @State private var clusterFlow: SkieSwiftFlow<ClusterState>? = nil
    
    init(option: DimensionOption) {
        self.option = option
        self.quizzesFlow = libraryService.getQuizIntensities(dimId: option.id)
        self.dimensionFlow = libraryService.getDimension(dimId: option.id)
    }
    
    var body: some View {
        Observing(dimensionFlow.withInitialValue(option.dimension)) { dimension in
            if let dim = dimension {
                Group {
                    if let clusterState = clusterFlow {
                        clusteringBody(clusterState) {
                            clusterFlow = nil
                        }
                    } else {
                        contentBody
                    }
                }
                .navigationTitle(dim.name)
                .onChange(of: option) { _, _ in
                    clusterFlow = nil
                }
            } else {
                Placeholder {
                    Image(systemName: "folder.badge.questionmark")
                } content: {
                    Text("Dimension not found")
                }
            }
        }
        .onChange(of: option.dimension.id) { _, newValue in
            self.dimensionFlow = libraryService.getDimension(dimId: newValue)
        }
    }
    
    private var contentBody: some View {
        Observing(quizzesFlow) {
            VStack {
                ProgressView()
                Text("Loading dimension...")
            }
        } content: { data in
            if !data.isEmpty {
                ScrollView {
                    FileGrid {
                        ForEach(data) { item in
                            Item(
                                data: item,
                                dimensionId: option.dimension.id,
                                service: categorizeService
                            )
                        }
                    }
                    .padding()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                Placeholder {
                    Image(systemName: "folder")
                } content: {
                    Text("Dimension is Empty")
                    Text("click the \(Image(systemName: "lasso.badge.sparkles")) button to fill automatically")
                        .font(.caption)
                }
            }
        }
        .dropDestination(for: QuizOption.self) { items, _ in
            handleQuizDrop(items: items)
        }
        .onAppear {
            quizzesFlow = libraryService.getQuizIntensities(dimId: option.id)
        }
        .onChange(of: option) { _, newValue in
            quizzesFlow = libraryService.getQuizIntensities(dimId: newValue.id)
        }
        .toolbar {
            ToolbarItem {
                Button("Automatic Clustering", systemImage: "lasso.badge.sparkles") {
                    clusterFlow = clusterService.cluster(dimensionId: option.dimension.id, minSimilarity: 0.6)
                }
            }
        }
    }
    
    private func clusteringBody(_ stateFlow: SkieSwiftFlow<ClusterState>, onComplete: @escaping () -> Void) -> some View {
        Observing(stateFlow) {
            Placeholder {
                Image(systemName: "lasso.badge.sparkles")
            } content: {
                Text("Getting started...")
            }
        } content: { state in
            Placeholder {
                Image(systemName: "lasso.badge.sparkles")
            } content: {
                switch onEnum(of: state) {
                case .preparing(_):
                    Text("Preparing for clustering...")
                case .inference(let inference):
                    Text("Thinking about \(inference.text)...")
                case .search(_):
                    Text("Searching for related questions...")
                case .complete(let completion):
                    Text("Completed")
                    if completion.found > 0 {
                        Text("found \(completion.found) items related to \(option.dimension.name)")
                            .font(.caption)
                    } else {
                        Text("nothing was found related to \(option.dimension.name)")
                            .font(.caption)
                    }
                    Button("Continue", action: onComplete)
                        .buttonStyle(.plain)
                        .foregroundStyle(.tint)
                }
            }
            .animation(.default, value: state)
        }
    }
    
    private func handleQuizDrop(items: [QuizOption]) -> Bool {
        for item in items {
            try? categorizeService.associate(quizId: item.id, dimensionId: option.id)
        }
        return true
    }
}


extension DimensionDetailView {
    struct Item : View {
        let data: QuizIntensity
        let dimensionId: Int64
        let service: CategorizeServiceSync
        
        @State private var isPopoverPresented = false
        @State private var intensityBuffer: Double
        
        private var quizName: String {
            data.quiz.name ?? String(localized: "Empty question")
        }

        init(data: QuizIntensity, dimensionId: Int64, service: CategorizeServiceSync) {
            self.data = data
            self.dimensionId = dimensionId
            self.service = service
            
            intensityBuffer = data.intensity
        }
        
        var body: some View {
            FileGridItem(title: Text(quizName), caption: Text("\(Int((intensityBuffer * 100).rounded()))%")) {
                FileIcon()
                    .contextMenu {
                        Button("Change Intensity", systemImage: "dial.high") {
                            isPopoverPresented = true
                        }
                        Button("Exclude", systemImage: "folder.badge.minus", role: .destructive) {
                            service.disassociate(quizId: data.quiz.id, dimensionId: dimensionId)
                        }
                    } preview: {
                        QuestionPreview(data: data.quiz)
                            .padding()
                    }
            }
            .popover(isPresented: $isPopoverPresented) {
                DimensionIntensitySlider(value: $intensityBuffer)
                    .padding()
                    .frame(minWidth: 300, idealWidth: 360)
                    .presentationCompactAdaptation(.popover)
            }
            .onTapGesture {
                isPopoverPresented = true
            }
            .onChange(of: isPopoverPresented) { _, newValue in
                if !newValue {
                    service.updateIntensity(quizId: data.quiz.id, dimensionId: dimensionId, value: intensityBuffer)
                }
            }
            .draggable({
                let service = QueryService(db: Database.shared.app)
                return service.getQuizOption(quizId: data.quiz.id)!
            }())
        }
        
        private func updateIntensity(_ newValue: Double) {
            service.updateIntensity(quizId: data.quiz.id, dimensionId: dimensionId, value: newValue)
        }
    }
}
