import Foundation
import SwiftUI
import ComposeApp

struct SessionDetailView : View {
    let option: SessionOption
    
    var body: some View {
        Text("Session \(option.session.name) here")
    }
}
