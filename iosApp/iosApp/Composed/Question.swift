import Foundation
import SwiftUI
import ComposeApp

struct Question : View {
    let frames: [Frame]
    let namespace: Namespace.ID
    
    init(frames: [Frame], namespace: Namespace.ID) {
        self.frames = frames
        self.namespace = namespace
    }
    
    init(frames: [PrioritizedFrame], namespace: Namespace.ID) {
        self.frames = frames.sorted(by: { $0.priority < $1.priority }).map(\.frame)
        self.namespace = namespace
    }
    
    var body: some View {
        ScrollView {
            LazyVStack {
                ForEach(frames, id: \.id) { frame in
                    Item(frame: frame, namespace: namespace)
                }
            }
            .padding()
        }
    }
}

#Preview {
    @Previewable @Namespace var namespace
    Question(
        frames: [
            FrameText(id: 0, textFrame: TextFrame(id: 0, embeddingsId: nil, content: "What's the meaning of life?")),
            FrameOptions(optionsFrame: OptionsFrame(id: 1, name: "Choose several of the following 4"), frames: [
                KeyedPrioritizedFrame(frame: FrameText(id: 1, textFrame: TextFrame(id: 1, embeddingsId: nil, content: "To have fun")), isKey: true, priority: 0),
                KeyedPrioritizedFrame(frame: FrameText(id: 2, textFrame: TextFrame(id: 2, embeddingsId: nil, content: "To find one's meaning")), isKey: true, priority: 1),
                KeyedPrioritizedFrame(frame: FrameText(id: 3, textFrame: TextFrame(id: 3, embeddingsId: nil, content: "To help others")), isKey: true, priority: 3),
                KeyedPrioritizedFrame(frame: FrameText(id: 4, textFrame: TextFrame(id: 4, embeddingsId: nil, content: "To be remembered")), isKey: true, priority: 4),
            ])
        ],
        namespace: namespace
    )
}
