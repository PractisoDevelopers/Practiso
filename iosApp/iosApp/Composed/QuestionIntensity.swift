import Foundation
import SwiftUI
import ComposeApp

struct QuestionIntensity : View {
    let quiz: Quiz
    let intensity: Double
    
    private var quizName: String {
        quiz.name ?? String(localized: "Empty question")
    }
    
    var body: some View {
        VStack {
            Image("Document")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 54, height: 54)
            Text(quizName)
                .multilineTextAlignment(.center)
            Text("\(Int((intensity * 100).rounded()))%")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}
