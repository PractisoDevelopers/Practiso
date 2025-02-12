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
            Question.UnknownItem(frame: frame)
        }
    }
}

struct OptionsFrameView<Label : View> : View {
    let frame: FrameOptions
    var showName: Bool = true
    let content: (KeyedPrioritizedFrame) -> Label
    private var optionFrames: [KeyedPrioritizedFrame] {
        frame.frames.sorted { $0.priority < $1.priority }
    }
    
    var body: some View {
        VStack {
            if let name = frame.optionsFrame.name {
                if showName {
                    Text(name)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .foregroundStyle(.secondary)
                }
            }
            ForEach(optionFrames, id: \.frame.id) { optionFrame in
                content(optionFrame)
            }
        }
    }
}
