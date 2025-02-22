import Foundation
import Combine
import SwiftUI

struct DimensionIntensitySlider : View {
    @Binding var value: Double
    
    var body: some View {
        Slider(value: $value, in: 0...1, step: 0.01) {
            Text("\(Int(value * 100))%")
        } minimumValueLabel: {
            Text("0%")
        } maximumValueLabel: {
            Text("100%")
        }
    }
}
