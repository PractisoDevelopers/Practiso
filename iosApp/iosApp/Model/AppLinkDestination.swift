import Foundation
import ComposeApp

enum AppLinkDestination: Encodable, Decodable, Hashable {
    case archiveDocument(URL)
    case communityArchive(archiveId: String?)
    case addAccount(token: String)

    init?(url: URL) {
        do {
            let proto = try ComposeApp.Protocol_(urlString: url.absoluteString)
            switch onEnum(of: proto.action) {
            case let .importAuthToken(action):
                self = .addAccount(token: action.tokenString)
            case let .revealCommunityArchive(action):
                self = .communityArchive(archiveId: action.id)
            }
        } catch {
            self = .archiveDocument(url)
        }
    }
}
