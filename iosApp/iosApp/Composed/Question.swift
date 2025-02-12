import Foundation
import SwiftUI
import ComposeApp

struct Question : View {
    let data: [PrioritizedFrame]
    
    private var frames: [Frame] {
        data.sorted { $0.priority < $1.priority }.map(\.frame)
    }
    
    var body: some View {
        LazyVStack {
            ForEach(frames, id: \.id) { frame in
                switch frame {
                case let text as FrameText:
                    TextFrameView(frame: text.textFrame)
                case let image as FrameImage:
                    ImageFrameView(frame: image.imageFrame)
                case let options as FrameOptions:
                    OptionsFrameView(frame: options) { item in
                        Checkmark(isOn: item.isKey) {
                            OptionsFrameViewItem(frame: item.frame)
                        }
                    }
                default:
                    Text("Unknown frame type \(String(describing: frame.self))")
                        .foregroundStyle(.secondary)
                        .padding()
                        .border(.secondary, cornerRadius: 12)
                }
            }
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .padding()
    }
}

#Preview {
    Question(data: [
        PrioritizedFrame(frame: FrameText(id: 0, textFrame: TextFrame(id: 0, embeddingsId: nil, content: "What's the meaning of life?")), priority: 0),
        PrioritizedFrame(frame: FrameOptions(optionsFrame: OptionsFrame(id: 1, name: "Choose several of the following 4"), frames: [
            KeyedPrioritizedFrame(frame: FrameText(id: 1, textFrame: TextFrame(id: 1, embeddingsId: nil, content: "To have fun")), isKey: true, priority: 0),
            KeyedPrioritizedFrame(frame: FrameText(id: 2, textFrame: TextFrame(id: 2, embeddingsId: nil, content: "To find one's meaning")), isKey: true, priority: 1),
            KeyedPrioritizedFrame(frame: FrameText(id: 3, textFrame: TextFrame(id: 3, embeddingsId: nil, content: "To help others")), isKey: true, priority: 3),
            KeyedPrioritizedFrame(frame: FrameText(id: 4, textFrame: TextFrame(id: 4, embeddingsId: nil, content: "To be remembered")), isKey: true, priority: 4),
        ]), priority: 1)
    ])
}
