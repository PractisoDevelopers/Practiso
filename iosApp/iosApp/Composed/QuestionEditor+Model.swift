import Foundation
import ComposeApp

enum Modification {
    case update(oldValue: Frame, newValue: Frame)
    case push(frame: Frame, at: Int)
    case delete(frame: Frame, at: Int)
    case renameQuiz(oldName: String?, newName: String?)
}

class History : ObservableObject {
    @Published private var rawValue: [Modification] = []
    @Published private var head: Int = 0
    
    var modifications: [Modification] {
        Array(rawValue[..<head])
    }
    var canUndo: Bool {
        head > 0
    }
    var canRedo: Bool {
        head < rawValue.count
    }
    var isEmpty: Bool {
        rawValue.isEmpty
    }
    
    func append(_ mod: Modification) {
        if rawValue.count >= head {
            rawValue.removeSubrange(head...)
        }
        rawValue.append(mod)
        head += 1
    }
    
    func undo() -> Modification? {
        if head > 0 {
            head -= 1
            return rawValue[head]
        } else {
            return nil
        }
    }
    
    func redo() -> Modification? {
        if head < rawValue.count {
            head += 1
            return rawValue[head - 1]
        } else {
            return nil
        }
    }
}
