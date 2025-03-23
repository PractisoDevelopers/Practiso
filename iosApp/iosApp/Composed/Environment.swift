import Foundation
import SwiftUICore
import ComposeApp

struct ImageServiceKey : EnvironmentKey {
    static let defaultValue: any ImageService = ResourceImageService.shared
}

struct ImageCacheKey : EnvironmentKey {
    static let defaultValue: ImageFrameView.Cache = ImageFrameView.Cache()
}

struct TakeServiceKey : EnvironmentKey {
    static let defaultValue: TakeService = TakeService(takeId: 0, db: Database.shared.app)
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
    
    var takeService: TakeService {
        get { self[TakeServiceKey.self] }
        set { self[TakeServiceKey.self] = newValue }
    }
}
