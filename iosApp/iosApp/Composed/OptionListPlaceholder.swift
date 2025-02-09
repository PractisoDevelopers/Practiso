import Foundation
import SwiftUI

struct OptionListPlaceholder: View {
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "folder")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 48)
            Text("No items available")
        }
        .foregroundStyle(.secondary)
        .edgesIgnoringSafeArea(.all)
    }
}
