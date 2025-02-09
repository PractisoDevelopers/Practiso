import Foundation
import SwiftUI
import ComposeApp

struct DimensionView: View {
    @State var data = OptionListData()
    @State var isDeletingActionsShown = false
    @State var deletionId: Int64?
    
    private let removeService = RemoveServiceSync(db: Database.shared.app)
    
    var body: some View {
        OptionListView(data: data) { option in
            OptionListItem(data: option)
                .swipeActions {
                    if (option.kt as! DimensionOption).quizCount <= 0 {
                        Button(role: .destructive) {
                            withAnimation {
                                removeService.removeDimensionKeepQuizzes(id: option.id)
                            }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    } else {
                        Button {
                            withAnimation {
                                deletionId = option.id
                                isDeletingActionsShown = true
                            }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        .tint(.red)
                    }
                }
        }
        .alert("Deleting Dimension", isPresented: $isDeletingActionsShown, presenting: deletionId, actions: { id in
            Button("Delete All", role: .destructive) {
                removeService.removeDimensionWithQuizzes(id: id)
            }
            Button("Keep Questions") {
                removeService.removeDimensionKeepQuizzes(id: id)
            }
        }, message: { _ in
            Text("Would you like to delete questions contained as well?")
        })
        .task {
            data.isRefreshing = true
            for await items in LibraryDataModel.shared.dimensions {
                data.isRefreshing = false
                data.items = items.map(Option.init)
            }
        }
    }
}
