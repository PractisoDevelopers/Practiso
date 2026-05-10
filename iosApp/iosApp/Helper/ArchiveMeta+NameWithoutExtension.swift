import Foundation
import ComposeApp

extension ArchiveMetadata {
    var nameWithoutExtension: String {
        if let dot = name.lastIndex(of: ".") {
            String(name[name.startIndex..<dot])
        } else {
            name
        }
    }
}
