@preconcurrency import ComposeApp
import Foundation
import SwiftUI

struct CommunityAccountView: View {
    @State private var state: PageState = .loading

    enum PageState {
        case loading
        case some(info: Whoami)
        case none
    }

    var body: some View {
        Group {
            switch state {
            case .loading:
                ProgressView()
                    .controlSize(.large)
            case let .some(info):
                Form {
                    Section {
                        HStack {
                            Image(systemName: "person.crop.circle")
                            VStack(alignment: .leading) {
                                Text(info.name ?? info.name)
                                Text(info.clientName)
                                    .fontDesign(.monospaced)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            case .none:
                Form {
                    Section {
                        Text("No account")
                    }
                    Section {
                        Button("Via QR Code") {
                        }
                        Button("Type In Token") {
                        }
                    } header: {
                        Text("Add existing")
                    } footer: {
                        Text("You may create new accounts by sharing your content")
                    }
                }
            }
        }
        .task {
            state = .loading
            for await update in AppCommunityService.shared.getWhoami() {
                if let update {
                    state = .some(info: update)
                } else {
                    state = .none
                }
            }
        }
    }
}
