import Foundation
import ComposeApp

extension Frame {
    var utid: Int128 {
        switch self {
        case let t as FrameText:
            t.textFrame.utid
        case let i as FrameImage:
            i.imageFrame.utid
        case let o as FrameOptions:
            o.optionsFrame.utid
        default:
            Int128(self.id)
        }
    }
}

protocol UnitypeFrameId {
    var utid: Int128 { get }
}

extension ImageFrame : UnitypeFrameId {
    var utid: Int128 {
        return (2 << 64) | Int128(self.id)
    }
}

extension TextFrame : UnitypeFrameId {
    var utid: Int128 {
        return (1 << 64) | Int128(self.id)
    }
}

extension OptionsFrame : UnitypeFrameId {
    var utid: Int128 {
        return (3 << 64) | Int128(self.id)
    }
}
