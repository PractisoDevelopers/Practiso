import Foundation
import SwiftUI

extension View {
    nonisolated func border<S : ShapeStyle>(_ style: S, cornerRadius: CGFloat, lineWidth: CGFloat = 0.3) -> some View {
        self.overlay {
            RoundedRectangle(cornerRadius: cornerRadius)
                .stroke(style, lineWidth: lineWidth)
        }
    }
}
