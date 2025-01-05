import SwiftUI
import CoreHaptics
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: ContentView.ViewModel())
        }
    }
    
    init() {
        if (CHHapticEngine.capabilitiesForHardware().supportsHaptics) {
            var engine: CHHapticEngine?
            do {
                engine = try CHHapticEngine()
            } catch let error {
                print("Error creating HapticEngine: \(error)")
            }
            engine?.resetHandler = {
                do {
                    try engine?.start()
                } catch let error {
                    print("Error restarting HapticEngine: \(error)")
                }
            }
            
            HapticFeedback_iosKt.sharedVibrator = Vibrator(wobble: {
                guard let url = Bundle.main.url(forResource: "AHAP/Wobble", withExtension: "ahap") else {
                    return
                }
                guard let engine = engine else {
                    return
                }
                
                do {
                    try engine.start()
                    try engine.playPattern(from: url)
                } catch let error {
                    print(error)
                }
            })
        }
    }
}
