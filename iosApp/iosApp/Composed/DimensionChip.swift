import Foundation
import SwiftUI

struct DimensionChip: View {
    let emoji: String?
    let name: String
    let selected: Bool
    
    init(emoji: String?, name: String, selected: Bool = false) {
        self.emoji = emoji
        self.name = name
        self.selected = selected
    }

    var body: some View {
        ZStack {
            if selected {
                Color.accentColor.opacity(0.6)
            }
            Text(String("\(emoji ?? "📝") \(name)"))
                .padding(10)
                .chipMaterial()
        }
        .clipShape(.capsule)
    }
}

fileprivate extension View {
    func chipMaterial() -> some View {
        Group {
            if #available(iOS 26.0, *) {
                self.glassEffect(in: .capsule)
            } else {
                self.background(.regularMaterial, in: .capsule)
            }
        }
    }
}
