import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct DimensionView: View {
    @Environment(ContentView.Model.self) private var contentModel
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    @State private var data = OptionListData<OptionImpl<DimensionOption>>()
    @State private var isDeletingActionsShown = false
    @State private var editMode: EditMode = .inactive
    @State private var selection = Set<Int64>()
    @State private var isNamingAlertShown = false
    @State private var namingBuffer = ""
    
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    private let categorizeService = CategorizeServiceSync(db: Database.shared.app)

    var body: some View {
        OptionList(
            data: data,
            selection: Binding(get: {
                selection
            }, set: { newValue in
                if !editMode.isEditing, let id = newValue.first {
                    contentModel.detail = .dimension(data.items.first(where: {$0.id == id})!.kt)
                }
                selection = newValue
            }),
            onDelete: { options in
                if data.items.first(where: { options.contains($0.id) && $0.kt.quizCount > 0 }) != nil {
                    isDeletingActionsShown = true
                } else {
                    withAnimation {
                        errorHandler.catchAndShowImmediately {
                            for id in options {
                                try removeService.removeDimensionKeepQuizzes(id: id)
                            }
                        }
                    }
                }
            }
        ) { option in
            Item(option: option)
                .swipeActions {
                    if option.kt.quizCount <= 0 {
                        Button(role: .destructive) {
                            withAnimation {
                                errorHandler.catchAndShowImmediately {
                                    try removeService.removeDimensionKeepQuizzes(id: option.kt.id)
                                }
                            }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    } else {
                        Button {
                            selection = Set([option.id])
                            isDeletingActionsShown = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        .tint(.red)
                    }
                }
        }
        .environment(\.editMode, $editMode)
        .alert("Deleting Dimension", isPresented: $isDeletingActionsShown, actions: {
            Button("Delete All", role: .destructive) {
                for id in selection {
                    errorHandler.catchAndShowImmediately {
                        try removeService.removeDimensionWithQuizzes(id: id)
                    }
                }
                selection = Set()
            }
            Button("Keep Questions") {
                for id in selection {
                    errorHandler.catchAndShowImmediately {
                        try removeService.removeDimensionKeepQuizzes(id: id)
                    }
                }
                selection = Set()
            }
        }, message: {
            Text("Would you like to delete questions contained as well?")
        })
        .task {
            data.isRefreshing = true
            let service = LibraryService(db: Database.shared.app)
            for await items in service.getDimensions() {
                DispatchQueue.main.schedule {
                    withAnimation {
                        data.items = items.map(OptionImpl.init)
                    }
                }
                data.isRefreshing = false
            }
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Create", systemImage: "plus") {
                    isNamingAlertShown = true
                }
            }
        }
        .alert("Create Dimension", isPresented: $isNamingAlertShown) {
            TextField("Name of the dimension", text: $namingBuffer)
            Button("Cancel", role: .cancel) {
            }
            Button("OK") {
                categorizeService.createDimension(name: namingBuffer.trimmingCharacters(in: .whitespaces))
                namingBuffer = ""
                isNamingAlertShown = false
            }
            .disabled(namingBuffer.trimmingCharacters(in: .whitespaces).isEmpty)
        } message: {
        }
    }
}

fileprivate struct Item : View {
    let option: OptionImpl<DimensionOption>
    private let dimensionId: Int64
    
    init(option: OptionImpl<DimensionOption>) {
        self.option = option
        self.dimensionId = option.kt.id
    }
    
    var body: some View {
        OptionListItem(data: option)
            .onDrop(of: [.psarchive], isTargeted: Binding.constant(false)) { providers in
                for provider in providers {
                    _ = provider.loadTransferable(type: QuizOption.self) { result in
                        if let quizOption = try? result.get() {
                            let service = CategorizeServiceSync(db: Database.shared.app)
                            try? service.associate(quizId: quizOption.quiz.id, dimensionId: dimensionId)
                        }
                    }
                }
                return true
            }
    }
}
