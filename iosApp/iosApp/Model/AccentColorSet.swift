import Foundation
import SwiftUICore

extension Color {
    init(accentColorFrom: any Hashable) {
        let name = "AccentColorSet/\(abs(accentColorFrom.hashValue) % 5)"
        self.init(name)
    }
}
