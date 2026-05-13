import Foundation
import SwiftUI
@preconcurrency import ComposeApp

struct CommunityArchiveIdView: View {
    let id: String
    
    var body: some View {
        Observing(AppCommunityService.shared.getArchiveMetadata(archiveId: id)) {
            ProgressView()
                .controlSize(.large)
        } content: { meta in
            if let meta {
                CommunityArchiveView(item: meta)
            } else {
                Text("Archive not found")
                    .font(.title)
                    .bold()
                Text(id)
                    .font(.caption)
            }
        }
    }
}
