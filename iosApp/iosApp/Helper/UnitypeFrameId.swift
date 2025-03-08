import Foundation
import ComposeApp

extension Frame {
    var utid: Int128 {
        let typeId : Int128 = switch self {
        case is FrameText:
            1
        case is FrameImage:
            2
        case is FrameOptions:
            3
        default:
            0
        }
        return (typeId << 64) | Int128(self.id)
    }
}
