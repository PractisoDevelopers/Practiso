import Foundation
import SwiftUI

struct ScaleRelease : ViewModifier {
    let scale: Double
    let completion: () -> Void
    @State private var isPressed = false
    @State private var changes = 0
    
    func body(content: Content) -> some View {
        content
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged{ _ in
                        changes += 1
                    }
                    .onEnded { gesture in
                        if changes > 1 || gesture.translation.width > 3 || gesture.translation.height > 3 {
                            changes = 0
                            return
                        }
                        withAnimation(.linear(duration: 0.1)) {
                            isPressed = true
                        } completion: {
                            withAnimation(.linear(duration: 0.1), {
                                isPressed = false
                            }, completion: completion)
                        }
                    }
            )
            .scaleEffect(isPressed ? scale : 1)
    }
}

extension View {
    func scalesOnRelease(scale: Double = 0.97, completion: @escaping () -> Void) -> some View {
        modifier(ScaleRelease(scale: scale, completion: completion))
    }
}
