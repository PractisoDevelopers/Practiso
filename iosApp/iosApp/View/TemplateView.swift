import Foundation
import SwiftUI
import ComposeApp

struct TemplateView: View {
    @State var data = OptionListView.Data()
    
    var body: some View {
        OptionListView(data: data)
            .task {
                data.refreshing = true
                for await items in LibraryDataModel.shared.templates {
                    data.items = items.map(Option.init)
                    data.refreshing = false
                }
            }
    }
}
