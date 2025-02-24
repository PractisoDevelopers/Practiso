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
    @State private var currentPopoverItem: QuizIntensity? = nil
    
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
                                dimensionId: option.dimension.id,
                                service: categorizeService
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
        
        init(data: QuizIntensity, dimensionId: Int64, service: CategorizeServiceSync) {
            self.data = data
            self.dimensionId = dimensionId
            self.service = service
            
            intensityBuffer = data.intensity
        }
        
        var body: some View {
            QuestionIntensity(quiz: data.quiz, intensity: intensityBuffer)
                .contextMenu {
                    Button("Change Intensity", systemImage: "dial.high") {
                        isPopoverPresented = true
                    }
                    Button("Exclude", systemImage: "folder.badge.minus", role: .destructive) {
                        service.disassociate(quizId: data.quiz.id, dimensionId: dimensionId)
                    }
                } preview: {
                    QuestionPreview(data: data.quiz)
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
