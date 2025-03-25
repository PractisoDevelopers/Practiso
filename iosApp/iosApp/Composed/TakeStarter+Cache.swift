import Foundation
@preconcurrency import ComposeApp

extension TakeStarter {
    struct TakeCacheDrop : CacheDrop {
        let capacity: Int
        func shouldDrop(inserting: QuizFrames, store: Dictionary<Int64, QuizFrames>, visit: [Int64]) -> Bool {
            visit.count > capacity
        }
    }
    
    final class Cache : CacheCompositor<LruCache<TakeCacheDrop>>, @unchecked Sendable {
        init(capacity: Int = 5) {
            super.init(inner: .init(drop: .init(capacity: capacity)))
        }
    }
}

extension QuizFrames : @retroactive @unchecked Sendable {
}
