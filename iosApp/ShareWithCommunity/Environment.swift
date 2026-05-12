import Foundation
import SwiftUI

struct ArchiveURLKey: EnvironmentKey {
    static let defaultValue: ArchiveResourceInfo = .init()
}

struct FinishTaskKey: EnvironmentKey {
    static let defaultValue: () -> Void = {}
}

extension EnvironmentValues {
    var archiveResource: ArchiveResourceInfo {
        get { self[ArchiveURLKey.self] }
        set { self[ArchiveURLKey.self] = newValue }
    }
    
    var finishTask: () -> Void {
        get { self[FinishTaskKey.self] }
        set { self[FinishTaskKey.self] = newValue }
    }
}
