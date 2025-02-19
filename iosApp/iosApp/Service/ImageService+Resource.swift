import Foundation
import CoreGraphics
import ImageIO
import ComposeApp

final class ResourceImageService : ImageService {
    func load(fileName: String) throws(ImageServiceError) -> CGImage {
        let url = ResourceService.shared.resolve(fileName: fileName)
        if let source = CGImageSourceCreateWithURL(url as CFURL, nil) {
            if let image = CGImageSourceCreateImageAtIndex(source, 0, nil) {
                return image
            } else {
                throw ImageServiceError.invalidData
            }
        } else {
            throw ImageServiceError.sourceUnavailable(fileName)
        }
    }
    
    static let shared = ResourceImageService()
}

