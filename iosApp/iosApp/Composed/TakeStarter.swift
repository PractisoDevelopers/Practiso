import Foundation
import SwiftUI
import ComposeApp

struct TakeStarter : View {
    enum ModelState {
        case pending
        case ok(Model)
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
            VStack(alignment: .leading) {
                Group {
                    switch getModel() {
                    case .pending:
                        Spacer()
                    case .ok(let model):
                        Question(frames: model.question, namespace: internel)
                    }
                }
                .frame(height: 66)
                .frame(maxWidth: .infinity)
                Text(stat.name)
                Text("\(stat.countQuizTotal) questions")
                    .font(.subheadline)
            }
            .padding()
        }
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.tint)
        }
        .frame(maxWidth: .infinity)
        .task {
            let takeService = TakeService(db: Database.shared.app)
            if let currentQuizId = try? await takeService.getCurrentQuizId(takeId: stat.id) {
                takeService.getQuizzes(takeId: stat.id, shuffleSeed: <#T##Int64#>)
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
