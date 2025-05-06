import Foundation
import SwiftUI
import ComposeApp

struct SessionDetailView : View {
    let libraryService = LibraryService(db: Database.shared.app)
    let createService = CreateService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel
    
    let option: SessionOption
    let namespace: Namespace.ID
    private enum DataState {
        case pending
        case ok([TakeStat])
    }
    
    @State private var data: DataState = .pending
    @State private var isTakeCreatorShown = false
    @State private var takeParamsBuffer: TakeParameters
    @State private var isHiddenSectonExpanded = false
    
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
                        takeList(array)
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
                                        Task.detached {
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
        .task(id: option.id) {
            for await stats in libraryService.getTakesBySession(id: option.id) {
                data = .ok(stats)
            }
        }
    }
    
    func takeList(_ array: [TakeStat]) -> some View {
        List(selection: Binding(get: {
            Set<Int64>()
        }, set: { newValue in
            if let first = newValue.first {
                withAnimation {
                    contentModel.pathPeek = .answer(takeId: first)
                }
            }
        })) {
            Section {
                ForEach(array.filter({ $0.hidden == 0 }), id: \.id) { take in
                    TakeDetailHeader(stat: take)
                        .swipeActions {
                            Button("Hide", systemImage: "eye.slash", role: .destructive) {
                                let service = TakeServiceSync(base: TakeService(takeId: take.id, db: Database.shared.app))
                                errorHandler.catchAndShowImmediately {
                                    try service.updateVisibility(hidden: true)
                                }
                            }
                        }
                }
            }
            Section(isExpanded: $isHiddenSectonExpanded) {
                ForEach(array.filter({ $0.hidden == 1 }), id: \.id) { take in
                    TakeDetailHeader(stat: take)
                        .swipeActions {
                            Button("Unhide", systemImage: "arrow.up.to.line", role: .destructive) {
                                let service = TakeServiceSync(base: TakeService(takeId: take.id, db: Database.shared.app))
                                errorHandler.catchAndShowImmediately {
                                    try service.updateVisibility(hidden: false)
                                }
                            }
                        }
                }
            } header: {
                hiddenSectionHeader
            }
        }
        .listStyle(.plain)
    }
    
    var hiddenSectionHeader: some View {
        Button {
            withAnimation {
                isHiddenSectonExpanded.toggle()
            }
        } label: {
            HStack {
                Text("Hidden").fontWeight(.bold).foregroundStyle(.secondary)
                Spacer()
                Image(systemName: "chevron.down")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 12, height: 12)
                    .rotationEffect(isHiddenSectonExpanded ? .zero : .degrees(-90))
                    .animation(.default, value: isHiddenSectonExpanded)
                    .foregroundStyle(.tint)
            }
            .contentShape(Rectangle())
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}
