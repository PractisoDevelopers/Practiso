import Foundation
import SwiftUI

struct OptionListItem<Item : Option>: View {
    @State var data: Item
    var body: some View {
        VStack(spacing: 4) {
            Text(data.view.header)
                .lineLimit(1)
                .fontWeight(.medium)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let title = data.view.title {
                Text(title)
                    .lineLimit(1)
                    .font(.system(size: 15))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let subtitle = data.view.subtitle {
                Text(subtitle)
                    .lineLimit(1)
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

class OptionListData<Item : Option>: ObservableObject {
    @Published var items: [Item]
    @Published var isRefreshing: Bool
    
    init(items: [Item] = [], refreshing: Bool = true) {
        self.items = items
        self.isRefreshing = refreshing
    }
}

enum SortOrder {
    case acending
    case decending
}

enum OptionListSort {
    case name(SortOrder)
    case modification(SortOrder)
    case creation(SortOrder)
}
