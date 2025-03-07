import Foundation
import ComposeApp

typealias SendChannelKt = Kotlinx_coroutines_coreSendChannel

enum ImportState {
    case idle
    case unarchiving(name: String)
    case confirmation(total: Int32, proceed: SendChannelKt, cancel: SendChannelKt)
    case importing(total: Int32, done: Int32)
    case error(model: ErrorModel, cancel: SendChannelKt, skip: SendChannelKt?, retry: SendChannelKt?, ignore: SendChannelKt?)
    
    init(kt: ComposeApp.ImportState) {
        switch onEnum(of: kt) {
        case .confirmation(let m):
            self = .confirmation(total: m.total, proceed: m.ok, cancel: m.dismiss)
        case .error(let m):
            self = .error(model: m.model, cancel: m.cancel, skip: m.skip, retry: m.retry, ignore: m.ignore)
        case .idle(_):
            self = .idle
        case .importing(let m):
            self = .importing(total: m.total, done: m.done)
        case .unarchiving(let m):
            self = .unarchiving(name: m.target)
        }
    }
}
