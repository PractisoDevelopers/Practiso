import Foundation
import ComposeApp
import ImageIO

enum ImageServiceError : Error {
    case sourceUnavailable(URL)
    case invalidData
}

struct ImageService {
    static func load(fileName: String) throws(ImageServiceError) -> CGImage {
        let url = ResourceService.shared.resolve(fileName: fileName)
        if let source = CGImageSourceCreateWithURL(url as CFURL, nil) {
            if let image = CGImageSourceCreateImageAtIndex(source, 0, nil) {
                return image
            } else {
                throw ImageServiceError.invalidData
            }
        } else {
            throw ImageServiceError.sourceUnavailable(url)
        }
    }
}
