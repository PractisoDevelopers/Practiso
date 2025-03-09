import SwiftUI
import Foundation
@preconcurrency import ComposeApp

struct SessionView: View {
    let namespace: Namespace.ID
    
    private let libraryService = LibraryService(db: Database.shared.app)
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel
    
    private enum DataState<T> {
        case pending
        case ok([T])
    }
    
    @State private var sessions: DataState<OptionImpl<SessionOption>> = .pending
    @State private var takes: DataState<TakeStat> = .pending
    @State private var isCreatorShown = false
    @State private var creatorModel = SessionCreatorView.Model()

    var body: some View {
        Group {
            if case .ok(let sessions) = sessions, case .ok(let takes) = takes {
                if sessions.isEmpty && takes.isEmpty {
                    OptionListPlaceholder()
                } else {
                    List {
                        Section("Takes") {
                            ForEach(takes, id: \.id) { stat in
                                TakeItem(stat: stat, namespace: namespace)
                            }
                            .listRowSeparator(.hidden)
                        }
                        Section("Sessions") {
                            ForEach(sessions) { option in
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
            } else {
                OptionListPlaceholder()
            }
        }
        .task {
            for await option in libraryService.getSessions() {
                withAnimation {
                    sessions = .ok(option.map(OptionImpl.init))
                }
            }
        }
        .task {
            for await stat in libraryService.getRecentTakes() {
                withAnimation {
                    takes = .ok(stat)
                }
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if case .ok(_) = sessions, case .ok(_) = takes {
                } else {
                    ProgressView()
                }
            }
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
                            creatorModel = SessionCreatorView.Model()
                        }
                    } else {
                        let creator = SessionCreator(params: creatorModel.sessionParams)
                        if let sessionId = (await errorHandler.catchAndShowImmediately {
                            try await creator.create()
                        }) {
                            creatorModel = SessionCreatorView.Model()
                        }
                    }
                }
            } onCancel: {
                isCreatorShown = false
            }
        }
    }
    
    struct TakeItem : View {
        let stat: TakeStat
        let namespace: Namespace.ID
        
        @Environment(ContentView.Model.self) private var contentModel
        
        var body: some View {
            TakeStarter(stat: stat, namespace: namespace)
                .frame(maxWidth: .infinity)
                .matchedGeometryEffect(id: stat.id, in: namespace, isSource: true)
        }
    }
}
