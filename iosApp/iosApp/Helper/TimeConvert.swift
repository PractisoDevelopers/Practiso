import Foundation
import ComposeApp

extension Date {
    init(kt: Kotlinx_datetimeInstant) {
        self.init(timeIntervalSince1970: TimeInterval(floatLiteral: Double(kt.toEpochMilliseconds()) / 1000))
    }
}
