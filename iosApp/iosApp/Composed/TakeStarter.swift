@preconcurrency import ComposeApp
import Foundation
import SwiftUI
import Transmission

struct TakeStarter<Label: View>: View {
    let stat: TakeStat
    @State var answerData: AnswerViewDataState = .pending
    @ViewBuilder let label: (TakeStat, Binding<AnswerViewDataState>) -> Label

    @Environment(ContentView.Model.self) private var contentModel
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(\.presentationCoordinator) var presentationCoordinator

    var body: some View {
        PresentationLink(transition: .zoom) {
            AnswerView(takeId: stat.id, data: $answerData) {
                ClosePushButton {
                    presentationCoordinator.dismiss()
                }
            }
            .environment(errorHandler)
            .environment(contentModel)
        } label: {
            label(stat, $answerData)
        }
    }
}

struct TakeStarterDefaultLabel: View {
    let stat: TakeStat
    @Binding var answerData: AnswerViewDataState

    @State private var data: DataState = .pending
    @State private var isReady: Bool = false
    @State private var isLocked: Bool = false
    
    @Environment(\.takeStarterCache) private var cache

    private let placeholderNS = Namespace()

    enum DataState {
        case pending
        case ok(QuizFrames)
        case empty
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
                    Placeholder {
                        Image(systemName: "folder")
                    } content: {
                        Text("Session is empty")
                    }
                case let .ok(qf):
                    Question(frames: qf.frames, namespace: placeholderNS.wrappedValue)
                        .opacity(isReady ? 0.6 : 0)
                        .animation(.default, value: isReady)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .mask(LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.2), .init(color: .black, location: 1)], startPoint: .top, endPoint: .bottom))
            .mask(LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.2), .init(color: .black, location: 1)], startPoint: .leading, endPoint: .trailing))
        }
        .background(Color(accentColorFrom: "\(stat.name)\(stat.id)"), in: .rect)
        .clipShape(.rect(cornerRadius: 20))
        .frame(maxWidth: .infinity)
        .task(id: stat.id) {
            if let cached = await cache.get(name: stat.id) {
                data = .ok(cached)
                answerData = .transition(qf: cached)
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
        if case let .ok(v) = newValue {
            answerData = .transition(qf: v)
        } else {
            answerData = .pending
        }
        DispatchQueue.main.schedule {
            withAnimation {
                if case let .ok(v) = newValue {
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
