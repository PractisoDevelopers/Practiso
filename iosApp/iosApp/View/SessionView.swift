import SwiftUI
import Foundation
@preconcurrency import ComposeApp

struct SessionView: View {
    let namespace: Namespace.ID
    
    private let libraryService = LibraryService(db: Database.shared.app)
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel
    
    @State private var isCreatorShown = false
    @State private var creatorModel = SessionCreatorView.Model()
    @State private var selection = Set<Int64>()
    @State private var sessionFlow: SkieSwiftFlow<[SessionOption]>
    @State private var takeFlow: SkieSwiftFlow<[TakeStat]>
    
    init(namespace: Namespace.ID) {
        self.namespace = namespace
        self.sessionFlow = libraryService.getSessions()
        self.takeFlow = libraryService.getRecentTakes()
    }
    
    var body: some View {
        Observing(sessionFlow, takeFlow) {
            OptionListPlaceholder()
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        ProgressView()
                    }
                }
        } content: { sessions, takes in
            if sessions.isEmpty && takes.isEmpty {
                OptionListPlaceholder()
            } else {
                List(selection: Binding(get: {
                    selection
                }, set: { newValue in
                    if newValue.count == 1 {
                        let id = newValue.first!
                        if let option = sessions.first(where: { $0.id == id }) {
                            contentModel.detail = .session(option)
                        }
                    }
                    selection = newValue
                })) {
                    Section("Takes") {
                        ForEach(takes, id: \.id) { stat in
                            TakeStarter(stat: stat, namespace: namespace)
                        }
                        .listRowSeparator(.hidden)
                    }
                    Section("Sessions") {
                        ForEach(sessions.map(OptionImpl.init), id: \.id) { option in
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
