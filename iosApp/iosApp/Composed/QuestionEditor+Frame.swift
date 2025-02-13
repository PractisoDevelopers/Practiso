import Foundation
import SwiftUI
import ComposeApp

struct FrameEditor : View {
    @Binding var frame: Frame
    let onDelete: () -> Void
    
    var body: some View {
        switch frame {
        case let text as FrameText:
            TextField(text: Binding(get: {
                text.textFrame.content
            }, set: { newValue, _ in
                if newValue == text.textFrame.content {
                    return
                }
                let textFrame = TextFrame(id: text.textFrame.id, embeddingsId: text.textFrame.embeddingsId, content: newValue)
                frame = FrameText(id: frame.id, textFrame: textFrame)
            }), axis: .vertical, label: {
                Text("Type text here...")
            })
            .onKeyPress(.delete) {
                if text.textFrame.content.isEmpty {
                    onDelete()
                    return .handled
                }
                return .ignored
            }
        case let image as FrameImage:
            ImageFrameView(frame: image.imageFrame)
                .frame(maxWidth: .infinity, alignment: .leading)
        default:
            Question.UnknownItem(frame: frame)
        }
    }
}
