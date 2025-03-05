import SwiftUI
import Foundation
@preconcurrency import ComposeApp

struct SessionView: View {
    private let libraryService = LibraryService(db: Database.shared.app)
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    enum OptionState<T> {
        case pending
        case ok([T])
    }
    
    @State var sessions: OptionState<OptionImpl<SessionOption>> = .pending
    @State var takes: OptionState<TakeStat> = .pending
    @State var isCreatorShown = false

    var body: some View {
        Group {
            if case .ok(let sessions) = sessions, case .ok(let takes) = takes {
                if sessions.isEmpty && takes.isEmpty {
                    OptionListPlaceholder()
                } else {
                    List {
                        Section("Takes") {
                            ForEach(takes, id: \.id) { stat in
                                TakeStarter(stat: stat)
                                    .frame(maxWidth: .infinity)
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
                sessions = .ok(option.map(OptionImpl.init))
            }
        }
        .task {
            for await stat in libraryService.getRecentTakes() {
                takes = .ok(stat)
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
            SessionCreatorView { sessionParameters, takeParameters, task in
                isCreatorShown = false
                
                Task {
                    if let takeParam = takeParameters {
                        let creator = SessionTakeCreator(session: sessionParameters, take: takeParam)
                        if let (sessionId, takeId) = (await errorHandler.catchAndShowImmediately {
                            try await creator.create()
                        }) {
                        }
                    } else {
                        let creator = SessionCreator(params: sessionParameters)
                        if let sessionId = (await errorHandler.catchAndShowImmediately {
                            try await creator.create()
                        }) {
                            
                        }
                    }
                }
            } onCancel: {
                isCreatorShown = false
            }
        }
    }
}
