import Foundation
import SwiftUI

class HoverableDropDelegate : DropDelegate, ObservableObject {
    enum TimerState {
        case pending
        case confirm(Foundation.Timer)
        case flicker(Foundation.Timer)
    }
    
    func performDrop(info: DropInfo) -> Bool {
        return false
    }
    
    @Published var flicker = false
    var timer: TimerState = .pending
    let trigger: () -> Void
    
    func dropEntered(info: DropInfo) {
        if case .pending = timer {
        } else {
            return
        }
        
        flicker = true
        timer = .confirm(Foundation.Timer.scheduledTimer(withTimeInterval: 0.8, repeats: false) { [self] _ in
            var flickerCount = 0
            let timer: TimerState = .flicker(Foundation.Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [self] timer in
                DispatchQueue.main.async {
                    self.flicker = !self.flicker
                }
                flickerCount += 1
                if flickerCount >= 6 {
                    timer.invalidate()
                    DispatchQueue.main.async {
                        self.flicker = false
                        self.trigger()
                    }
                }
            })
            
            DispatchQueue.main.async {
                self.timer = timer
            }
        })
    }
    
    func dropExited(info: DropInfo) {
        flicker = false
        switch timer {
        case .pending:
            return
        case .confirm(let timer):
            timer.invalidate()
            self.timer = .pending
        case .flicker(let timer):
            timer.invalidate()
            self.timer = .pending
        }
    }
    
    init(trigger: @escaping () -> Void) {
        self.trigger = trigger
    }
}

