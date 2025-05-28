import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct DimensionDetailView : View {
    private let libraryService = LibraryService(db: Database.shared.app)
    private let categorizeService = CategorizeServiceSync(db: Database.shared.app)
    
    let option: DimensionOption
    
    @State private var currentPopoverItem: QuizIntensity? = nil
    @State private var quizzesFlow: SkieSwiftFlow<[QuizIntensity]>
    
    init(option: DimensionOption) {
        self.option = option
        self.quizzesFlow = libraryService.getQuizIntensities(dimId: option.id)
    }
    
    var body: some View {
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
                VStack {
                    Placeholder(
                        image: Image(systemName: "folder"),
                        text: Text("Dimension is Empty")
                    )
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
        .navigationTitle(option.dimension.name)
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
