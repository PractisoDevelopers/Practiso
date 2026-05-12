import Combine
import ComposeApp
import CoreHaptics
import SwiftUI

@main
struct iOSApp: App {
    @Environment(\.openWindow) private var openWindow
    @Environment(\.dismissWindow) private var dismissWindow
    @Environment(\.supportsMultipleWindows) private var supportsMultipleWindow
    
    @AppStorage(.feiModel) private var feiModelIndex = 0
    
    @State private var mainWindowAppLinks: AppLinkDestination? = nil
    
    init() {
        try? Database.shared.fei.setFeiModelSync(model: UserDefaults.standard.feiModel)
    }
    
    var body: some Scene {
        WindowGroup {
            contentView($mainWindowAppLinks)
        }
        
        WindowGroup(id: "appLinks", for: AppLinkDestination.self) { dest in
            contentView(dest)
        }
        .handlesExternalEvents(matching: ["psarchive"])
    }
    
    func contentView(_ dest: Binding<AppLinkDestination?>) -> some View {
        Group {
            switch dest.wrappedValue {
            case let .archiveDocument(url):
                ArchiveDocumentView(url: url, onClose: {
                    dest.wrappedValue = nil
                })

            case let .communityArchive(archiveId):
                if let archiveId {
                    ContentView(communityArchiveId: archiveId)
                } else {
                    ContentView(destination: .community)
                }

            case nil:
                ContentView()
                    .onChange(of: feiModelIndex) { _, newValue in
                        try? Database.shared.fei.setFeiModelSync(model: KnownModel(index: Int32(newValue)))
                    }
            }
        }
        .onOpenURL { value in
            dest.wrappedValue = nil
            dest.wrappedValue = .init(url: value)
        }
    }
}
