import Foundation
import SwiftUI

fileprivate struct CheckmarkBase<Title: View> : View {
    @Binding var isOn: Bool
    let isWithButton: Bool
    let title: Title
    let filled: Image
    let outline: Image
    
    var body: some View {
        HStack {
            Group {
                if isOn {
                    filled.checkmarkStyle(isEnabled: isWithButton)
                } else {
                    outline.checkmarkStyle(isEnabled: isWithButton)
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
        CheckmarkBase(isOn: $isOn, isWithButton: isWithButton, title: title, filled: Image(systemName: "checkmark.circle.fill"), outline: Image(systemName: "circle"))
    }
}

struct CheckmarkSquare<Title: View> : View {
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
        CheckmarkBase(isOn: $isOn, isWithButton: isWithButton, title: title, filled: Image(systemName: "checkmark.square.fill"), outline: Image(systemName: "square"))
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
