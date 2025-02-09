import Foundation
import SwiftUI
import ComposeApp

struct TemplateView: View {
    @State var data = OptionListData()
    
    var body: some View {
        OptionListView(data: data) {
            OptionListItem(data: $0)
        }
            .task {
                data.isRefreshing = true
                for await items in LibraryDataModel.shared.templates {
                    data.items = items.map(Option.init)
                    data.isRefreshing = false
                }
            }
    }
}
