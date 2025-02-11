import Foundation
import ImageIO

extension ImageFrameView {
    actor Cache {
        let capacity: Double = 10 // Mega bytes
        private var store: Dictionary<String, CGImage> = Dictionary()
        private var visit: [String] = []
        
        func get(name: String) -> CGImage? {
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
        
        func exceedsCapacity(image: CGImage) -> Bool {
            let size = store.values.reduce(Int64(0)) { partialResult, image in
                partialResult + Int64(image.height * image.width) * Int64(image.bitsPerPixel)
            }
            return size > Int64(capacity * Double(1 << 20))
        }
        
        func put(name: String, image: CGImage) {
            visit.append(name)
            if exceedsCapacity(image: image) {
                let removing = visit.removeFirst()
                store.removeValue(forKey: removing)
            }
            store[name] = image
        }
    }
}
