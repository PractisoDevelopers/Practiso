import Foundation
import SwiftUI
import ComposeApp
import ImageIO

private enum Modification {
    
}

struct QuestionEditor {
    @Binding var data: QuizFrames
    @State private var history: [Modification] = []
    private var nextFrameId: Int64 {
        (data.frames.last?.frame.id ?? -1) + 1
    }
    
}
