import Foundation

protocol Cache : Observable, ObservableObject {
    associatedtype V : Sendable
    associatedtype K : Hashable, Sendable
    func get(name: K) async -> V?
    func put(name: K, value: V) async
}

actor LruCache<D: CacheDrop> : Cache where D.V : Sendable, D.K : Sendable {
    private var store: Dictionary<D.K, D.V> = Dictionary()
    private var visit: [D.K] = []
   
    let drop: D
    
    init(drop: D) {
        self.drop = drop
    }
    
    func get(name: D.K) -> D.V? {
        if let image = store[name] {
            if let vIndex = visit.firstIndex(of: name) {
                visit.remove(at: vIndex)
            }
            visit.append(name)
            
            return image
        } else {
            return nil
        }
    }
    
    func put(name: D.K, value: D.V) {
        visit.append(name)
        if drop.shouldDrop(inserting: value, store: store, visit: visit) {
            let removing = visit.removeFirst()
            store.removeValue(forKey: removing)
        }
        store[name] = value
    }
}

protocol CacheDrop {
    associatedtype V
    associatedtype K : Hashable
    func shouldDrop(inserting: V, store: Dictionary<K, V>, visit: [K]) -> Bool
}

/**
    This class is meant for inheritance. Any subclass should not introduce mutating factors.
 */
class CacheCompositor<I: Cache> : Cache, @unchecked Sendable where I : Sendable {
    private let inner: I
    
    init(inner: I) {
        self.inner = inner
    }
    
    func get(name: I.K) async -> I.V? {
        await self.inner.get(name: name)
    }
    
    func put(name: I.K, value: I.V) async {
        await self.inner.put(name: name, value: value)
    }
}
