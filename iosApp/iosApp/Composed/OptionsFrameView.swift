import Foundation
import SwiftUI
import ComposeApp

struct OptionsFrameViewItem : View {
    let frame: Frame
    
    var body: some View {
        switch frame {
        case let text as FrameText:
            TextFrameView(frame: text.textFrame)
        case let image as FrameImage:
            ImageFrameView(frame: image.imageFrame)
        default:
            Text("Unsupported frame type \(String(describing: frame.self))")
                .foregroundStyle(.secondary)
                .padding()
                .border(.secondary, cornerRadius: 0.3)
        }
    }
}

struct OptionsFrameView<Label : View> : View {
    let frame: FrameOptions
    let content: (KeyedPrioritizedFrame) -> Label
    private var optionFrames: [KeyedPrioritizedFrame] {
        frame.frames.sorted { $0.priority < $1.priority }
    }
    
    var body: some View {
        VStack {
            if let name = frame.optionsFrame.name {
                Text(name)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            ForEach(optionFrames, id: \.frame.id) { optionFrame in
                content(optionFrame)
            }
        }
    }
}
