import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct TakeStarter : View {
    enum ModelState {
        case pending
        case ok(Model)
        case empty
    }
    
    let stat: TakeStat
    @Binding var model: ModelState
    let libraryService = LibraryService(db: Database.shared.app)
    
    private let useOwnModel: Bool
    @State private var ownModel: ModelState = .pending
    @Namespace private var internel
    
    init(stat: TakeStat, model: Binding<ModelState>? = nil) {
        self.stat = stat
        if let m = model {
            self._model = m
            self.useOwnModel = false
        } else {
            self._model = Binding.constant(.pending)
            self.useOwnModel = true
        }
    }
    
    var body: some View {
        ZStack {
            ZStack(alignment: .leading) {
                Group {
                    switch getModel() {
                    case .pending:
                        Spacer()
                    case .empty:
                        Placeholder(image: Image(systemName: "folder"), text: Text("Session is empty"))
                    case .ok(let model):
                        Question(frames: model.question, namespace: internel)
                            .opacity(0.6)
                    }
                }
                .fixedSize()
                .frame(height: 160, alignment: .top)
                .frame(maxWidth: .infinity)
                .clipped()
                .mask(LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.2), .init(color: .black, location: 1)], startPoint: .top, endPoint: .bottom))
                
                VStack(alignment: .leading) {
                    Spacer()
                    VStack(alignment: .leading) {
                        Text(stat.name)
                        Text("\(stat.countQuizTotal) questions")
                            .font(.subheadline)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background {
                        Rectangle().fill(.regularMaterial)
                    }
                }
            }
        }
        .background {
            Rectangle().fill(.tint)
        }
        .clipShape(.rect(cornerRadius: 20))
        .frame(maxWidth: .infinity)
        .task {
            let takeService = TakeService(db: Database.shared.app)
            if let quiz = try? await takeService.getCurrentQuiz(takeId: stat.id) {
                updateModel(newValue: .ok(.init(question: quiz.frames)))
            } else {
                updateModel(newValue: .empty)
            }
        }
    }
    
    private func getModel() -> ModelState {
        if useOwnModel {
            return ownModel
        } else {
            return model
        }
    }
    
    private func updateModel(newValue: ModelState) {
        if useOwnModel {
            ownModel = newValue
        } else {
            model = newValue
        }
    }
}
