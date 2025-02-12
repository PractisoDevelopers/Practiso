import Foundation
import SwiftUI

struct Checkmark<Title: View> : View {
    @Binding private var isOn: Bool
    private let isWithButton: Bool
    private let title: Title
    
    init(isOn: Bool, title: () -> Title) {
        self._isOn = Binding.constant(isOn)
        self.isWithButton = false
        self.title = title()
    }
    
    init(isOn: Binding<Bool>, title: () -> Title) {
        self._isOn = isOn
        self.isWithButton = true
        self.title = title()
    }
    
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
            .disabled(!isWithButton)
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
