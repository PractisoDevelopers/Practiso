import Foundation
import SwiftUI

struct Pannable : ViewModifier {
    let onPan: PanChange
    
    func body(content: Content) -> some View {
        PannableView(content: { content }, onPan: onPan)
    }
}

extension View {
    func pannable(change: @escaping PanChange) -> some View {
        modifier(Pannable(onPan: change))
    }
}
