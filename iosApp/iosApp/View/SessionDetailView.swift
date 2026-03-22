import ComposeApp
import Foundation
import SwiftUI
import Transmission

struct SessionDetailView: View {
    let libraryService = LibraryService(db: Database.shared.app)
    let createService = CreateService(db: Database.shared.app)
    let editService = EditService(db: Database.shared.app)
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    @Environment(ContentView.Model.self) private var contentModel

    let option: SessionOption

    @State private var isTakeCreatorShown = false
    @State private var takeParamsBuffer: TakeParameters
    @State private var isHiddenSectonExpanded = false
    @State private var takesFlow: SkieSwiftFlow<[TakeStat]>

    init(option: SessionOption) {
        self.option = option
        takeParamsBuffer = .init(sessionId: option.id)
        takesFlow = libraryService.getTakesBySession(id: option.id)
    }

    var body: some View {
        Delay { pastDelay in
            Observing(takesFlow) {
                OptionListPlaceholder()
                    .toolbar {
                        if pastDelay {
                            ToolbarItem(placement: .topBarTrailing) {
                                ProgressView()
                            }
                        }
                    }
            } content: { array in
                Group {
                    if array.isEmpty {
                        OptionListPlaceholder()
                    } else {
                        takeList(array)
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
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Create", systemImage: "plus") {
                    isTakeCreatorShown = true
                }
            }
        }
        .onAppear {
            takeParamsBuffer = .init(sessionId: option.id)
            takesFlow = libraryService.getTakesBySession(id: option.id)
        }
        .onChange(of: option) { _, newValue in
            takesFlow = libraryService.getTakesBySession(id: newValue.id)
        }
        .navigationTitle(Binding(get: {
            option.session.name
        }, set: { newValue in
            errorHandler.catchAndShowImmediately {
                try editService.renameSession(sessionId: option.session.id,
                                              newName: newValue.trimmingCharacters(in: .whitespacesAndNewlines))
            }
        }))
        .navigationBarTitleDisplayMode(.inline)
    }

    func takeList(_ array: [TakeStat]) -> some View {
        List {
            Section {
                ForEach(array.filter({ $0.hidden == 0 }), id: \.id) { take in
                    TakeStarter(stat: take) { take, _ in
                        TakeDetailHeader(stat: take)
                    }
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
                    TakeStarter(stat: take) { take, _ in
                        TakeDetailHeader(stat: take)
                    }
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
