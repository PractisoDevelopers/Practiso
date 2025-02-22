import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct DimensionView: View {
    @Environment(ContentView.Model.self) private var contentModel
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    @State private var data = OptionListData<OptionImpl<DimensionOption>>()
    @State private var isDeletingActionsShown = false
    @State private var deletionIdSet: Set<Int64>?
    @State private var editMode: EditMode = .inactive
    @State private var selection = Set<OptionImpl<DimensionOption>>()
    
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    
    var body: some View {
        OptionList(
            data: data,
            selection: Binding(get: {
                selection
            }, set: { newValue in
                if !editMode.isEditing, let dim = newValue.first {
                    contentModel.detail = .dimension(dim.kt)
                }
                selection = newValue
            }),
            onDelete: { options in
                deletionIdSet = Set(options.map(\.kt.id))
                isDeletingActionsShown = true
            }
        ) { option in
            OptionListItem(data: option)
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
                            deletionIdSet = Set(arrayLiteral: option.kt.id)
                            isDeletingActionsShown = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        .tint(.red)
                    }
                }
        }
        .environment(\.editMode, $editMode)
        .alert("Deleting Dimension", isPresented: $isDeletingActionsShown, presenting: deletionIdSet, actions: { idSet in
            Button("Delete All", role: .destructive) {
                for id in idSet {
                    errorHandler.catchAndShowImmediately {
                        try removeService.removeDimensionWithQuizzes(id: id)
                    }
                }
            }
            Button("Keep Questions") {
                for id in idSet {
                    errorHandler.catchAndShowImmediately {
                        try removeService.removeDimensionKeepQuizzes(id: id)
                    }
                }
            }
        }, message: { _ in
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
    }
}
