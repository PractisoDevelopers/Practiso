import SwiftUI
import Combine

// Source - https://stackoverflow.com/a/75927476
// Posted by Okayokay, modified by community. See post 'Timeline' for change history
// Retrieved 2026-05-11, License - CC BY-SA 4.0

public struct ShakeEffect: GeometryEffect {
    public var amount: CGFloat = 10
    public var shakesPerUnit = 3
    public var animatableData: CGFloat
    
    public init(amount: CGFloat = 10, shakesPerUnit: Int = 3, animatableData: CGFloat) {
        self.amount = amount
        self.shakesPerUnit = shakesPerUnit
        self.animatableData = animatableData
    }

    public func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX: amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)), y: 0))
    }
}

extension View {
    public func shakeAnimation(_ shake: Binding<Bool>, sink: PassthroughSubject<Void, Never>) -> some View {
        modifier(ShakeEffect(animatableData: shake.wrappedValue ? 2 : 0))
            .animation(.default, value: shake.wrappedValue)
            .onReceive(sink) {
                shake.wrappedValue = true
                withAnimation(.default.delay(0.15)) { shake.wrappedValue = false }
            }
    }
}
