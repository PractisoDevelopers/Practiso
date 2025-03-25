import Foundation
import ImageIO

extension ImageFrameView {
    struct ImageCacheDrop : CacheDrop {
        let capacity: Double // in metabytes
        
        func shouldDrop(inserting: CGImage, store: Dictionary<String, CGImage>, visit: [String]) -> Bool {
            let size = store.values.reduce(Int64(0)) { partialResult, image in
                partialResult + Int64(image.height * image.width) * Int64(image.bitsPerPixel)
            }
            return size > Int64(capacity * Double(1 << 20))
        }
    }
    
    final class Cache : CacheCompositor<LruCache<ImageCacheDrop>>, @unchecked Sendable {
        init(capacity: Double = 10) {
            super.init(inner: .init(drop: .init(capacity: capacity)))
        }
    }
}
