import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: ContentView.ViewModel())
                .task {
                    _ = try! await Database.shared.migrate()
                }
        }
    }
}
