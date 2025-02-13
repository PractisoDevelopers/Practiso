import Foundation
import ComposeApp

enum Modification {
    case update(oldValue: Frame, newValue: Frame)
    case push(frame: Frame, at: Int)
    case delete(frame: Frame, at: Int)
    case renameQuiz(oldName: String?, newName: String?)
}
