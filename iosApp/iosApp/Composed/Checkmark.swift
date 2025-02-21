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
        HStack {
            Group {
                if isOn {
                    Image(systemName: "checkmark.circle.fill")
                        .checkmarkStyle(isEnabled: isWithButton)
                } else {
                    Image(systemName: "circle")
                        .checkmarkStyle(isEnabled: isWithButton)
                }
            }
            .hoverEffect(isEnabled: isWithButton)
            .onTapGesture {
                isOn = !isOn
            }
            title
        }
        .padding(.vertical, 4)
        .foregroundStyle(.primary)
    }
}

extension Image {
    func checkmarkStyleBase() -> some View {
        self.imageScale(.medium)
    }
    
    func checkmarkStyle(isEnabled: Bool) -> some View {
        Group {
            if isEnabled {
                checkmarkStyleBase()
                    .foregroundStyle(.tint)
            } else {
                checkmarkStyleBase()
            }
        }
    }
}
