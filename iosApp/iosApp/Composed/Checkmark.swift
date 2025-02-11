import Foundation
import SwiftUI

struct Checkmark<Title: View> : View {
    @Binding var isOn: Bool
    @ViewBuilder let title: Title
    
    var body: some View {
        Button(action: {
            isOn = !isOn
        }) {
            HStack {
                if isOn {
                    Image(systemName: "checkmark.circle.fill")
                        .checkmarkStyle()
                } else {
                    Image(systemName: "circle")
                        .checkmarkStyle()
                }
                title
            }
        }.foregroundStyle(.primary).padding(.vertical, 4)
    }
}

extension Image {
    func checkmarkStyle() -> some View {
        self.resizable()
            .aspectRatio(contentMode: .fit)
            .frame(width: 24, height: 24)
            .foregroundStyle(Color("AccentColor"))
    }
}
