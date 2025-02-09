import Foundation
import SwiftUI
import ComposeApp

struct TemplateView: View {
    @State var data = OptionListData<OptionImpl<TemplateOption>>()
    
    var body: some View {
        OptionListView(data: data, onDelete: { _ in
            // TODO
        }) {
            OptionListItem(data: $0)
        }
            .task {
                data.isRefreshing = true
                for await items in LibraryDataModel.shared.templates {
                    data.items = items.map(OptionImpl.init)
                    data.isRefreshing = false
                }
            }
    }
}
