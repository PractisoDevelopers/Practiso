import Foundation
import SwiftUICore
import ComposeApp
import ImageIO

enum ImageServiceError : LocalizedError {
    case sourceUnavailable(String)
    case invalidData
    
    public var errorDescription: String {
        switch self {
        case .sourceUnavailable(let string):
            String(localized: "Source \"\(string)\" is unavailable.")
        case .invalidData:
            String(localized: "Loaded data represents an invalid image.")
        }
    }
}

protocol ImageService : Observable, Sendable {
    func load(fileName: String) throws(ImageServiceError) -> CGImage
}

struct ImageServiceKey : EnvironmentKey {
    static let defaultValue: any ImageService = ResourceImageService.shared
}

extension EnvironmentValues {
    var imageService: any ImageService {
        get { self[ImageServiceKey.self] }
        set { self[ImageServiceKey.self] = newValue }
    }
}
