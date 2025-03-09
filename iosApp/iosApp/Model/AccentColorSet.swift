import Foundation
import SwiftUICore

extension Color {
    init(accentColorSet: UInt) {
        let name = "AccentColorSet/\(accentColorSet % 5)"
        self.init(name)
    }
    
    init(accentColorFrom: String) {
        let id = accentColorFrom.utf8.reduce(0) { partialResult, unit in
            unit.words.reduce(partialResult) { partialResult, value in
                (partialResult + value) % 5
            }
        }
        self.init(accentColorSet: id)
    }
}
