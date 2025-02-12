import Foundation
import SwiftUI

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
