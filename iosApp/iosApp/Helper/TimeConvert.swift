import Foundation
import ComposeApp

extension Date {
    init(kt: Kotlinx_datetimeInstant) {
        self.init(timeIntervalSince1970: TimeInterval(floatLiteral: Double(kt.toEpochMilliseconds()) / 1000))
    }
}

extension Duration {
    init(hours: Int, minutes: Int, seconds: Int) {
        self.init(secondsComponent: Int64(hours) * 60 * 60 + Int64(minutes) * 60 + Int64(seconds), attosecondsComponent: 0)
    }
}
