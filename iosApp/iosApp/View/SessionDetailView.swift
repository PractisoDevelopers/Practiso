import Foundation
import SwiftUI
import ComposeApp

struct SessionDetailView : View {
    let libraryService = LibraryService(db: Database.shared.app)
    let createService = CreateService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    let option: SessionOption
    let namespace: Namespace.ID
    private enum DataState {
        case pending
        case ok([TakeStat])
    }
    
    @State private var data: DataState = .pending
    @State private var isTakeCreatorShown = false
    @State private var takeParamsBuffer: TakeParameters
    
    init(option: SessionOption, namespace: Namespace.ID) {
        self.option = option
        self.namespace = namespace
        self.takeParamsBuffer = .init(sessionId: option.id)
    }
    
    var body: some View {
        Group {
            switch data {
            case .pending:
                OptionListPlaceholder()
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            ProgressView()
                        }
                    }
            case .ok(let array):
                Group {
                    if array.isEmpty {
                        OptionListPlaceholder()
                    } else {
                        List(array, id: \.id) { take in
                            TakeStarter(stat: take, namespace: namespace)
                                .listRowSeparator(.hidden)
                        }
                        .listStyle(.plain)
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        Button("Create", systemImage: "plus") {
                            isTakeCreatorShown = true
                        }
                    }
                }
                .sheet(isPresented: $isTakeCreatorShown) {
                    NavigationStack {
                        TakeCreatorView(session: option, takeParams: $takeParamsBuffer)
                            .toolbar {
                                ToolbarItem(placement: .topBarLeading) {
                                    Button("Cancel") {
                                        isTakeCreatorShown = false
                                    }
                                }
                                ToolbarItem(placement: .primaryAction) {
                                    Button("Create") {
                                        isTakeCreatorShown = false
                                        Task {
                                            await errorHandler.catchAndShowImmediately {
                                                let creator = TakeCreator(take: takeParamsBuffer)
                                                _ = try await creator.create()
                                                takeParamsBuffer = TakeParameters(sessionId: option.id)
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
        .navigationTitle(option.view.header)
        .task {
            for await stats in libraryService.getTakesBySession(id: option.id) {
                data = .ok(stats)
            }
        }
    }
}
