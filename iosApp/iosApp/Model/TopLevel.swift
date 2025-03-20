import Foundation
import ComposeApp

enum TopLevel {
    case library
    case answer(takeId: Int64, cache: QuizFrames?)
}
