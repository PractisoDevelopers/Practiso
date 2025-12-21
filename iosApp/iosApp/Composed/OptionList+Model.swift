import Foundation
import SwiftUI

class OptionListData<Item: Option>: ObservableObject {
    @Published var items: [Item]
    @Published var isRefreshing: Bool

    init(items: [Item] = [], refreshing: Bool = true) {
        self.items = items
        isRefreshing = refreshing
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

extension OptionListSort {
    var order: SortOrder {
        switch self {
        case let .creation(order):
            order
        case let .modification(order):
            order
        case let .name(order):
            order
        }
    }

    func ordered(by: SortOrder) -> OptionListSort {
        switch self {
        case .creation:
            .creation(by)
        case .modification:
            .modification(by)
        case .name:
            .name(by)
        }
    }
}
