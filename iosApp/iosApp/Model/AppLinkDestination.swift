import Foundation

enum AppLinkDestination: Encodable, Decodable, Hashable {
    case archiveDocument(URL)
    case communityArchive(archiveId: String?)

    init?(url: URL) {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true) else {
            self = .archiveDocument(url)
            return
        }
        guard components.scheme == "practiso" else {
            self = .archiveDocument(url)
            return
        }
        
        let comp = components.path.split(separator: "/")
        switch comp.first {
        case "community":
            switch comp.dropFirst().first {
            case "archive":
                let id = comp.dropFirst(2).first
                self = .communityArchive(archiveId: id != nil ? String(id!) : nil)
                return
            default:
                return nil
            }
        default:
            return nil
        }
    }
}
