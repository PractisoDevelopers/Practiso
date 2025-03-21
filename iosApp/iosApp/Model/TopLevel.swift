import Foundation
import ComposeApp

indirect enum TopLevel {
    case library
    case answer(takeId: Int64, cache: QuizFrames?)
    case transition(foreground: TopLevel, displacement: CGPoint)
}
