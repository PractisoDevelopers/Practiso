import Combine
import SwiftUI
import CoreHaptics
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.openWindow) private var openWindow
    
    var body: some Scene {
        WindowGroup(id: "content") {
            ContentView()
                .onOpenURL { value in
                    openWindow(id: "browser", value: value)
                }
        }
        
        WindowGroup(id: "browser", for: URL.self) { $url in
            Group {
                if let url = url {
                    ArchiveDocumentView(
                        url: url,
                        onClose: {
                            openWindow(id: "content")
                        }
                    )
                } else {
                    OptionListPlaceholder()
                }
            }
            .onOpenURL { value in
                url = value
            }
        }
        .handlesExternalEvents(matching: ["psarchive"])
    }
}
