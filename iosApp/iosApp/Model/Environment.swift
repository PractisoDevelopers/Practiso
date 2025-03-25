import Foundation
import SwiftUICore
@preconcurrency import ComposeApp

struct ImageServiceKey : EnvironmentKey {
    static let defaultValue: any ImageService = ResourceImageService.shared
}

struct ImageCacheKey : EnvironmentKey {
    static let defaultValue: ImageFrameView.Cache = .init()
}

struct TakeServiceKey : EnvironmentKey {
    static let defaultValue: TakeService = .init(takeId: 0, db: Database.shared.app)
}

struct TakeStarterCacheKey : EnvironmentKey {
    static let defaultValue: TakeStarter.Cache = .init()
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
    
    var takeStarterCache: TakeStarter.Cache {
        get { self[TakeStarterCacheKey.self] }
        set { self[TakeStarterCacheKey.self] = newValue }
    }
}
