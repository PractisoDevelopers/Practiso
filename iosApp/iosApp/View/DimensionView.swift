import Foundation
import SwiftUI
import ComposeApp

struct DimensionView: View {
    @State var data = OptionListView.Data()
    
    var body: some View {
        OptionListView(data: data)
            .task {
                data.isRefreshing = true
                for await items in LibraryDataModel.shared.dimensions {
                    data.isRefreshing = false
                    data.items = items.map(Option.init)
                }
            }
    }
}
