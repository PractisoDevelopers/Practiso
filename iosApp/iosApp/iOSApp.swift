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
        do {
            HapticFeedback_iosKt.sharedVibrator = try CoreHapticFeedback()
        } catch {
            print("Failed to initialize share vibrator.")
        }
    }
}
