import Foundation
import SwiftUI
import ComposeApp

struct DimensionView: View {
    @Environment(ContentView.ErrorHandler.self) private var errorHandler
    
    @State var data = OptionListData<OptionImpl<DimensionOption>>()
    @State var isDeletingActionsShown = false
    @State var deletionIdSet: Set<Int64>?
    
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    
    var body: some View {
        OptionListView(data: data, onDelete: { ids in
            deletionIdSet = ids
            isDeletingActionsShown = true
        }) { option in
            OptionListItem(data: option)
                .swipeActions {
                    if option.kt.quizCount <= 0 {
                        Button(role: .destructive) {
                            withAnimation {
                                errorHandler.catchAndShowImmediately {
                                    try removeService.removeDimensionKeepQuizzes(id: option.id)
                                }
                            }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    } else {
                        Button {
                            deletionIdSet = Set(arrayLiteral: option.id)
                            isDeletingActionsShown = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        .tint(.red)
                    }
                }
        }
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
            for await items in LibraryDataModel.shared.dimensions {
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
