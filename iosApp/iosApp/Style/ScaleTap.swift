import Foundation
import SwiftUI

struct ScaleTap : ViewModifier {
    @State var isPressed = false
    
    func body(content: Content) -> some View {
        content
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        isPressed = true
                    }
                    .onEnded { _ in
                        isPressed = false
                    }
            )
            .scaleEffect(isPressed ? 0.96 : 1)
            .animation(.linear(duration: 0.1), value: isPressed)
    }
}

extension View {
    func scalesOnTap() -> some View {
        modifier(ScaleTap())
    }
}
