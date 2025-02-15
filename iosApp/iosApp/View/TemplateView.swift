import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct TemplateView: View {
    @State var data = OptionListData<OptionImpl<TemplateOption>>()
    @State private var selection = Set<OptionImpl<TemplateOption>>()

    var body: some View {
        OptionList(
            data: data, selection: $selection,
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
