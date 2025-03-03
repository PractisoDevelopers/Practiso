import Foundation
import SwiftUI

struct ButtonListItem : ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration
            .label
            .frame(
                maxWidth: .greatestFiniteMagnitude,
                alignment: .leading)
            .contentShape(.rect)
            .background {
                if configuration.isPressed {
                    Rectangle()
                        .fill(.foreground.opacity(0.1))
                }
            }
    }
}

extension ButtonStyle where Self == ButtonListItem {
    static var listItem: Self {
        ButtonListItem()
    }
}
