import Foundation
import SwiftUICore

struct ImageServiceKey : EnvironmentKey {
    static let defaultValue: any ImageService = ResourceImageService.shared
}

struct ImageCacheKey : EnvironmentKey {
    static let defaultValue: ImageFrameView.Cache = ImageFrameView.Cache()
}

extension EnvironmentValues {
    var imageService: any ImageService {
        get { self[ImageServiceKey.self] }
        set { self[ImageServiceKey.self] = newValue }
    }
    
    var imageCache: ImageFrameView.Cache {
        get { self[ImageCacheKey.self] }
        set { self[ImageCacheKey.self] = newValue }
    }
}
