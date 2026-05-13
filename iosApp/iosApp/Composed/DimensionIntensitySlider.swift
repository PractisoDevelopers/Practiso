import Foundation
import Combine
import SwiftUI

struct DimensionIntensitySlider : View {
    @Binding var value: Double
    
    var body: some View {
        Slider(value: $value, in: 0...1) {
            Text(Int(value * 100), format: .percent)
        } minimumValueLabel: {
            Text(0, format: .percent)
        } maximumValueLabel: {
            Text(100, format: .percent)
        }
    }
}
