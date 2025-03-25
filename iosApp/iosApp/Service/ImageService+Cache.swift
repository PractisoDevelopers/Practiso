import Foundation
import ComposeApp
import CoreGraphics
import ImageIO

struct CachedImageService : ImageService {
    private enum Image {
        case ok(CGImage)
        case invalid
    }
    
    private let data: Dictionary<String, Image>
    
    init(_ loaded: Dictionary<String, CGImage>) {
        self.data = loaded.mapValues { .ok($0) }
    }
    
    init(data: Dictionary<String, Data>) {
        self.data = data.mapValues {
            if let source = CGImageSourceCreateWithData($0 as CFData, nil) {
                if let image = CGImageSourceCreateImageAtIndex(source, 0, nil) {
                    .ok(image)
                } else {
                    .invalid
                }
            } else {
                .invalid
            }
        }
    }
    
    func load(fileName: String) throws(ImageServiceError) -> CGImage {
        if let cache = data[fileName] {
            switch cache {
            case .ok(let im):
                return im
            case .invalid:
                throw .invalidData
            }
        } else {
            throw .sourceUnavailable(fileName)
        }
    }
}
