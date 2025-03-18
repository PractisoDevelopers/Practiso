import Foundation
import SwiftUI

struct TimerView : View {
    @Binding var value: [Timer]
    @State private var shownTimerPicker: UUID? = nil
    
    var body: some View {
        ForEach(value) { timer in
            TimerItem(
                timer: Binding(get: {
                    timer
                }, set: { newValue in
                    let index = value.firstIndex(where: {$0.id == timer.id})!
                    value = Array(value[0..<index] + [newValue] + value[(index+1)...])
                }),
                isPickerShown: Binding(get: {
                    shownTimerPicker == timer.id
                }, set: { newValue in
                    if newValue {
                        shownTimerPicker = timer.id
                    } else {
                        shownTimerPicker = nil
                    }
                })
            )
            .swipeActions {
                Button("Remove", systemImage: "trash", role: .destructive) {
                    let index = value.firstIndex(where: {$0.id == timer.id})!
                    value = Array(value[0..<index] + value[(index+1)...])
                }
            }
        }
        
        Button("Add Timer") {
            withAnimation {
                value.append(Timer(timeout: Duration.seconds(600)))
            }
        }
    }
    
    private struct TimerItem : View {
        @Binding var timer: Timer
        @Binding var isPickerShown: Bool
        @State private var hours: Int
        @State private var minutes: Int
        @State private var seconds: Int
        
        init(timer: Binding<Timer>, isPickerShown: Binding<Bool>) {
            self._timer = timer
            self._isPickerShown = isPickerShown
            
            let hour = Duration.seconds(60*60)
            let minute = Duration.seconds(60)
            let hours = Int(timer.wrappedValue.timeout / hour)
            let minutes = Int((timer.wrappedValue.timeout - hour * hours) / minute)
            let seconds = Int((timer.wrappedValue.timeout - hour * hours - minute * minutes) / Duration.seconds(1))
            
            self.hours = hours
            self.minutes = minutes
            self.seconds = seconds
        }
        
        var body: some View {
            VStack(alignment: .leading) {
                Button {
                    withAnimation {
                        isPickerShown = !isPickerShown
                    }
                } label: {
                    HStack {
                        Text(timer.timeout.formatted())
                        Spacer()
                        Image(systemName: "chevron.right")
                            .rotationEffect(isPickerShown ? .degrees(90) : .degrees(0))
                            .foregroundStyle(.secondary)
                    }
                    .foregroundStyle(.foreground)
                }
            }
            
            if isPickerShown {
                DurationPicker(
                    hourSelection: $hours,
                    minuteSelection: $minutes,
                    secondSelection: $seconds
                )
                .frame(height: 200)
                .onChange(of: hours) { _, newValue in
                    timer.timeout = Duration(hours: newValue, minutes: minutes, seconds: seconds)
                }
                .onChange(of: minutes) { _, newValue in
                    timer.timeout = Duration(hours: hours, minutes: newValue, seconds: seconds)
                }
                .onChange(of: seconds) { _, newValue in
                    timer.timeout = Duration(hours: hours, minutes: minutes, seconds: newValue)
                }
            }
        }
    }
}
