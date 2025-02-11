import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct TemplateView: View {
    @State var data = OptionListData<OptionImpl<TemplateOption>>()
    @State var editMode: EditMode = .inactive
    @State private var selection = Set<Int64>()

    var body: some View {
        OptionListView(
            data: data, editMode: $editMode, selection: $selection,
            onDelete: { _ in
                // TODO
            }) { OptionListItem(data: $0) }
            .task {
                data.isRefreshing = true
                let service = LibraryService(db: Database.shared.app)
                for await items in service.getTemplates() {
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
