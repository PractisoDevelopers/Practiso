import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct TakeStarter : View {
    enum DataState {
        case pending
        case ok(QuizFrames)
        case empty
    }
    
    let stat: TakeStat
    let namespace: Namespace.ID
    
    @Environment(ContentView.Model.self) private var contentModel
    @Environment(\.takeStarterCache) private var cache
    
    @State private var isReady: Bool = false
    @State private var isLocked: Bool = false
    @State private var data: DataState = .pending
    
    init(stat: TakeStat, namespace: Namespace.ID) {
        self.stat = stat
        self.namespace = namespace
    }

    var body: some View {
        VStack(alignment: .leading) {
            Spacer()
            TakeStatHeader(stat: stat)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background {
                    Rectangle().fill(.regularMaterial)
                }
        }
        .frame(idealHeight: 160)
        .background {
            Group {
                switch data {
                case .pending:
                    Spacer()
                case .empty:
                    Placeholder(image: Image(systemName: "folder"), text: Text("Session is empty"))
                case .ok(let qf):
                    Question(frames: qf.frames, namespace: namespace)
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
                .matchedGeometryEffect(id: stat.id, in: namespace)
        }
        .clipShape(.rect(cornerRadius: 20))
        .frame(maxWidth: .infinity)
        .scalesOnTap()
        .onTapGesture {
            if case .ok(let qf) = data {
                contentModel.answerData = .transition(qf: qf)
            }
            withAnimation {
                contentModel.pathPeek = .answer(takeId: stat.id)
            }
        }
        .task(id: stat.id) {
            if let cached = await cache.get(name: stat.id) {
                data = .ok(cached)
            }
            let takeService = TakeService(takeId: stat.id, db: Database.shared.app)
            if let quiz = try? await takeService.getCurrentQuiz() {
                updateModel(newValue: .ok(quiz))
            } else {
                updateModel(newValue: .empty)
            }
        }
    }
    
    private func updateModel(newValue: DataState) {
        data = newValue
        DispatchQueue.main.schedule {
            withAnimation {
                if case .ok(let v) = newValue {
                    Task {
                        await cache.put(name: stat.id, value: v)
                    }
                    isReady = true
                } else {
                    isReady = false
                }
            }
        }
    }
}
