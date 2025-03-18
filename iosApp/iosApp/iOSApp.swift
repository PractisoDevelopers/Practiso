import Combine
import SwiftUI
import CoreHaptics
import ComposeApp

@main
struct iOSApp: App {
    @State var url: URL?
    
    var body: some Scene {
        WindowGroup {
            Group {
                if let url = url {
                    ArchiveDocumentView(
                        url: url,
                        onClose: {
                            self.url = nil
                        }
                    )
                }
                if url == nil {
                    ContentView()
                }
            }
            .onOpenURL { value in
                url = value
            }
        }
        .handlesExternalEvents(matching: ["psarchive"])
    }
}
