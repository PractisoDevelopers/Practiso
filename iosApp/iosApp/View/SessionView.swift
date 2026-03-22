@preconcurrency import ComposeApp
import Foundation
import SwiftUI
import Transmission

struct SessionView: View {
    private let libraryService = LibraryService(db: Database.shared.app)
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel

    @State private var isCreatorShown = false
    @State private var creatorModel = SessionCreatorView.Model()
    @State private var sessionFlow: SkieSwiftFlow<[SessionOption]>
    @State private var takeFlow: SkieSwiftFlow<[TakeStat]>
    
    init() {
        self.sessionFlow = libraryService.getSessions()
        self.takeFlow = libraryService.getRecentTakes()
    }
    
    var body: some View {
        Observing(sessionFlow, takeFlow) {
            OptionListPlaceholder()
        } content: { sessions, takes in
            if sessions.isEmpty && takes.isEmpty {
                OptionListPlaceholder()
            } else {
                List(selection: Binding<Set<Int128>>(get: {
                    Set()
                }, set: { newValue in
                    if let id = newValue.first {
                        let id = Int64(id & Int128(Int64.max))
                        if let option = sessions.first(where: { $0.id == id }) {
                            contentModel.detail = .session(option)
                            contentModel.column = .detail
                        }
                    }
                })) {
                    Section("Takes") {
                        ForEach(takes, id: \.sessionId) { stat in
                            TakeStarter(stat: stat) {
                                TakeStarterDefaultLabel(stat: $0, answerData: $1)
                            }
                        }
                        .listRowSeparator(.hidden)
                    }
                    Section("Sessions") {
                        ForEach(sessions.map(OptionImpl.init), id: \.sessionId) { option in
                            OptionListItem(data: option)
                                .swipeActions {
                                    Button("Remove", systemImage: "trash", role: .destructive) {
                                        errorHandler.catchAndShowImmediately {
                                            try removeService.removeSession(id: option.kt.id)
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Create", systemImage: "plus") {
                    isCreatorShown = true
                }
            }
        }
        .sheet(isPresented: $isCreatorShown) {
            SessionCreatorView(model: $creatorModel) { task in
                isCreatorShown = false
                
                Task {
                    if let takeParam = creatorModel.takeParams {
                        let creator = SessionTakeCreator(session: creatorModel.sessionParams, take: takeParam)
                        if let (sessionId, takeId) = (await errorHandler.catchAndShowImmediately {
                            try await creator.create()
                        }) {
                            creatorModel.reset()
                            switch task {
                            case .reveal:
                                let session = await libraryService.getSession(id: sessionId).makeAsyncIterator().next()!
                                contentModel.detail = .session(session)
                                contentModel.column = .detail
                            case .startTake:
                                contentModel.pathPeek = .answer(takeId: takeId)
                            case .none:
                                return
                            }
                        }
                    } else {
                        let creator = SessionCreator(params: creatorModel.sessionParams)
                        if let sessionId = (await errorHandler.catchAndShowImmediately {
                            try await creator.create()
                        }) {
                            creatorModel.reset()
                            switch task {
                            case .reveal:
                                let session = await libraryService.getSession(id: sessionId).makeAsyncIterator().next()!
                                contentModel.detail = .session(session)
                            default:
                                return
                            }
                        }
                    }
                }
            } onCancel: {
                isCreatorShown = false
            }
        }
    }
}

extension TakeStat {
    var sessionId: Int128 {
        1 << 64 | Int128(self.id)
    }
}

extension OptionImpl<SessionOption> {
    var sessionId: Int128 {
        2 << 64 | Int128(self.id)
    }
}
