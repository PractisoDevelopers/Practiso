import Foundation
@preconcurrency import ComposeApp

extension LibraryService {
    func saveModification(data: [Modification], quizId: Int64) throws {
        let edits: [Edit] = data.map { mod in
            switch mod {
            case .update(oldValue: let oldValue, newValue: let newValue):
                EditUpdate(old: oldValue, new: newValue)
            case .push(frame: let frame, at: let at):
                EditAppend(frame: frame, insertIndex: Int32(at))
            case .delete(frame: let frame, at: let at):
                EditRemove(frame: frame, oldIndex: Int32(at))
            case .renameQuiz(oldName: let oldName, newName: let newName):
                EditRename(old: oldName, new: newName)
            }
        }
        try saveEdit(data: edits, quizId: quizId)
    }
}
