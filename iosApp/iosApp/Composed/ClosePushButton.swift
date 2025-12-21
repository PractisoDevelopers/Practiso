import Foundation
import SwiftUI

struct ClosePushButton: View {
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            image
                .background(in: .circle)
                .glassEffect(.clear, in: .circle)
        } else {
            image
                .background {
                    Circle()
                        .foregroundStyle(.background)
                }
        }
    }

    var image: some View {
        Image(systemName: "xmark")
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 16, height: 16)
            .padding()
            .onTapGesture(perform: action)
    }
}

#Preview {
    ClosePushButton {}
}
