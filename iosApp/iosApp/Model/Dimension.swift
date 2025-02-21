import Foundation
import ComposeApp

extension QuizIntensity : @retroactive Identifiable {
    public var id: some Hashable {
        Double(quiz.id) + intensity
    }
}
