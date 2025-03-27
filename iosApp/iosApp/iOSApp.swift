import Combine
import SwiftUI
import CoreHaptics
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.openWindow) private var openWindow
    @Environment(\.dismissWindow) private var dismissWindow
    @Environment(\.supportsMultipleWindows) private var supportsMultipleWindow
    
    @State private var url: URL? = nil
    
    var body: some Scene {
        WindowGroup(id: "content") {
            if let openingUrl = url {
                ArchiveDocumentView(url: openingUrl) {
                    self.url = nil
                }
            } else {
                ContentView()
                    .onOpenURL { value in
                        if supportsMultipleWindow {
                            openWindow(id: "browser", value: value)
                        } else {
                            withAnimation {
                                self.url = value
                            }
                        }
                    }
            }
        }
        
        WindowGroup(id: "browser", for: URL.self) { $url in
            Group {
                if let openingUrl = url {
                    ArchiveDocumentView(
                        url: openingUrl,
                        onClose: {
                            if supportsMultipleWindow {
                                openWindow(id: "content")
                                dismissWindow(id: "browser")
                            } else {
                                url = nil
                            }
                        }
                    )
                } else if supportsMultipleWindow {
                    OptionListPlaceholder()
                } else {
                    ContentView()
                        .onOpenURL { value in
                            withAnimation {
                                url = value
                            }
                        }
                }
            }
            .onOpenURL { value in
                url = value
            }
        }
        .handlesExternalEvents(matching: ["psarchive"])
    }
}
