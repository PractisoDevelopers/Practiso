import Foundation
import SwiftUI
import ComposeApp

struct DimensionView: View {
    @State var data = OptionListView.Data()
    
    var body: some View {
        OptionListView(data: data)
            .task {
                data.refreshing = true
                for await items in LibraryDataModel.shared.dimensions {
                    data.refreshing = false
                    data.items = items.map(Option.init)
                }
            }
    }
}
