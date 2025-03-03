import Foundation
import SwiftUI

struct DurationPicker : View {
    private static let HOURS = [Int](0...24)
    private static let MINUTES = [Int](0...60)
    private static let SECONDS = [Int](0...60)
    
    @Binding var hourSelection: Int
    @Binding var minuteSelection: Int
    @Binding var secondSelection: Int
    
    var body: some View {
        GeometryReader { geometry in
            HStack(spacing: .zero) {
                Picker(selection: $hourSelection, label: Text("")) {
                    ForEach(DurationPicker.HOURS, id: \.self) { value in
                        Text("\(value) hr")
                            .tag(value)
                    }
                }
                .pickerStyle(.wheel)
                .frame(width: geometry.size.width / 3, alignment: .center)
                
                Picker(selection: $minuteSelection, label: Text("")) {
                    ForEach(DurationPicker.MINUTES, id: \.self) { value in
                        Text("\(value) min")
                            .tag(value)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                }
                .pickerStyle(.wheel)
                .frame(width: geometry.size.width / 3, alignment: .center)
                
                Picker(selection: $secondSelection, label: Text("")) {
                    ForEach(DurationPicker.SECONDS, id: \.self) { value in
                        Text("\(value) sec")
                            .tag(value)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                }
                .pickerStyle(.wheel)
                .frame(width: geometry.size.width / 3, alignment: .center)
            }
        }
    }
}
