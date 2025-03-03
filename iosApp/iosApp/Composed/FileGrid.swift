import Foundation
import SwiftUI

struct FileGrid<Content : View> : View {
    @ViewBuilder let content: Content
    
    var body: some View {
        LazyVGrid(columns: [.init(.adaptive(minimum: 100), alignment: .top)], spacing: 24) {
            content
        }
    }
}
