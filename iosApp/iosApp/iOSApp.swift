import Combine
import ComposeApp
import CoreHaptics
import SwiftUI
import AsyncAlgorithms

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
                
            case let .addAccount(token):
                ContentViewAddingAccount(token: token)

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

fileprivate struct ContentViewAddingAccount: View {
    @State var token: String? = nil
    @State private var updates = AsyncChannel<AddAccountState>()
    @State private var error: AddAccountError? = nil
    @State private var updatedWhoami: Whoami? = nil
    
    var body: some View {
        ContentView(destination: .community)
            .addAccountAlert($token, stateNotifier: updates)
            .task {
                for await update in updates {
                    switch update {
                    case .working:
                        break
                    case .success:
                        for await info in AppCommunityService.shared.getWhoami() {
                            updatedWhoami = info
                            break
                        }
                    case .error(let addAccountError):
                        error = addAccountError
                    }
                }
            }
            .alert(isPresented: $error.isNotNil(), error: error) {
                Button("Cancel", role: .cancel) {
                }
            }
            .alert("Add account", isPresented: $updatedWhoami.isNotNil()) {
                Button("OK", role: .cancel) {
                    updatedWhoami = nil
                }
            } message: {
                if let updatedWhoami {
                    if let userName = updatedWhoami.name {
                        Text("New account is \(userName) on \(updatedWhoami.clientName)")
                    } else {
                        Text("New account is \(updatedWhoami.clientName)")
                    }
                } else {
                    Text("Invalid credentials")
                }
            }
    }
}
