import Foundation
import SwiftUI

struct ScaleTap : ViewModifier {
    let scale: Double
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
            .scaleEffect(isPressed ? scale : 1)
            .animation(.linear(duration: 0.1), value: isPressed)
    }
}

extension View {
    func scalesOnTap(scale: Double = 0.97) -> some View {
        modifier(ScaleTap(scale: scale))
    }
}
