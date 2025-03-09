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
    let namespace: Namespace.ID
    
    private let libraryService = LibraryService(db: Database.shared.app)
    @Environment(ContentView.Model.self) private var contentModel
    
    private let useOwnModel: Bool
    @State private var ownModel: ModelState = .pending
    @State private var isReady: Bool = false
    @State private var isLocked: Bool = false
    
    init(stat: TakeStat, namespace: Namespace.ID, model: Binding<ModelState>? = nil) {
        self.stat = stat
        self.namespace = namespace
        if let m = model {
            self._model = m
            self.useOwnModel = false
        } else {
            self._model = Binding.constant(.pending)
            self.useOwnModel = true
        }
    }
    
    var body: some View {
        VStack(alignment: .leading) {
            Spacer()
            HStack(spacing: 12) {
                CircularProgressView(value: Double(stat.countQuizDone) / Double(stat.countQuizTotal))
                VStack(alignment: .leading) {
                    Text(stat.name)
                    Text("\(100 * stat.countQuizDone / stat.countQuizTotal)% done")
                        .font(.subheadline)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background {
                Rectangle().fill(.regularMaterial)
            }
        }
        .frame(idealHeight: 160)
        .background {
            Group {
                switch getModel() {
                case .pending:
                    Spacer()
                case .empty:
                    Placeholder(image: Image(systemName: "folder"), text: Text("Session is empty"))
                case .ok(let model):
                    Question(frames: model.question, namespace: namespace)
                        .opacity(isReady ? 0.6 : 0)
                        .animation(.default, value: isReady)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .mask(LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.2), .init(color: .black, location: 1)], startPoint: .top, endPoint: .bottom))
            .mask(LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.2), .init(color: .black, location: 1)], startPoint: .leading, endPoint: .trailing))
        }
        .background {
            Rectangle().fill(Color(accentColorFrom: "\(stat.name)\(stat.id)"))
        }
        .clipShape(.rect(cornerRadius: 20))
        .frame(maxWidth: .infinity)
        .onTapGesture {
            let cache: [PrioritizedFrame]? = if case .ok(let model) = getModel() {
                model.question
            } else {
                nil
            }
            withAnimation {
                contentModel.answering = .shown(takeId: stat.id, cache: cache)
            }
        }
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
        DispatchQueue.main.schedule {
            withAnimation {
                if case .ok(_) = newValue {
                    isReady = true
                } else {
                    isReady = false
                }
            }
        }
    }
}
