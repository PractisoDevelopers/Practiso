import ComposeApp
import Foundation

indirect enum TopLevel: Hashable {
    case library
    case answer(takeId: Int64)
}
