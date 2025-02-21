import Foundation
import SwiftUI
import ComposeApp

struct QuestionPreview : View {
    let data: Quiz
    private let errorHandler = ContentView.ErrorHandler()
    
    var body: some View {
        switch errorHandler.state {
        case .hidden:
            QuestionDetailView(option: QuizOption(quiz: data, preview: nil))
                .frame(minWidth: 200)
                .environmentObject(errorHandler)
        case .shown(let message):
            Text(message)
                .foregroundStyle(.secondary)
        }
    }
}
