import Foundation
import SwiftUI

struct UIHideStatusBarModifier: ViewModifier {
    @Environment(ContentView.Model.self) private var contentModel
    
    func body(content: Content) -> some View {
        content.onAppear {
            contentModel.hideStatusBar = true
        }
        .onDisappear {
            contentModel.hideStatusBar = false
        }
    }
}

extension View {
    func hideUIKitStatusBar() -> some View {
        modifier(UIHideStatusBarModifier())
    }
}
