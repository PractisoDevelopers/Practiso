import Foundation
import CoreTransferable

struct UrlTransfer: Transferable {
    let url: URL

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(contentType: .data) { data in
            SentTransferredFile(data.url)
        } importing: { received in
            Self(url: received.file)
        }
    }
}

enum CopiedTransfer: Transferable, Codable {
    case url(url: URL)
    case binary(data: Data)
    case error(String)
    
    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .data) { representation in
            let resultingUrl = NSURL.fileURL(withPath: NSTemporaryDirectory(), isDirectory: true)
                .appendingPathComponent(representation.file.lastPathComponent)
            do {
                _ = try FileManager.default.replaceItemAt(resultingUrl, withItemAt: representation.file)
                return CopiedTransfer.url(url: resultingUrl)
            } catch {
                return CopiedTransfer.error(error.localizedDescription)
            }
        }
        ProxyRepresentation {
            CopiedTransfer.binary(data: $0)
        }
    }
}

