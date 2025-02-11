import Foundation
import SwiftUI
import ComposeApp

struct Question : View {
    let data: QuizFrames
    
    private var frames: [Frame] {
        data.frames.sorted { $0.priority < $1.priority }.map(\.frame)
    }
    
    var body: some View {
        LazyVStack {
            ForEach(frames, id: \.id) { frame in
                switch frame {
                case let text as FrameText:
                    TextFrameView(frame: text.textFrame)
                case let image as FrameImage:
                    ImageFrameView(frame: image.imageFrame)
                default:
                    Text("Unknown frame type \(String(describing: frame.self))")
                        .padding()
                        .border(.secondary, cornerRadius: 12)
                }
            }
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .padding()
    }
}
