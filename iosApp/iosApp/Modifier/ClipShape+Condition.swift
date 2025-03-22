import Foundation
import SwiftUI

struct ClipShapeConditional<TargetShape : Shape> : ViewModifier {
    let shape: TargetShape
    let isEnabled: Bool
    func body(content: Content) -> some View {
        if isEnabled {
            content.clipShape(shape)
        } else {
            content
        }
    }
}

extension View {
    func clipShape<S : Shape>(isEnabled: Bool, shape: S) -> some View {
        self.modifier(ClipShapeConditional(shape: shape, isEnabled: isEnabled))
    }
}
