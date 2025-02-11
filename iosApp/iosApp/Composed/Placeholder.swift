import Foundation
import SwiftUI

struct Placeholder : View {
    var image: Image
    var text: Text
    var body: some View {
        VStack(spacing: 10) {
            image
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 48)
            text
        }
        .foregroundStyle(.secondary)
        .edgesIgnoringSafeArea(.all)
    }
}

struct OptionListPlaceholder : View {
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
