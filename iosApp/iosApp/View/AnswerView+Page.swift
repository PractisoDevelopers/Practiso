import Foundation
import SwiftUI
import ComposeApp

extension AnswerView {
    struct Page : View {
        let frames: [Frame]
        let answer: [Answer]
        let namespace: Namespace.ID
        
        init(frames: [Frame], answer: [Answer], namespace: Namespace.ID) {
            self.frames = frames
            self.answer = answer
            self.namespace = namespace
        }
        
        init(pframes: [PrioritizedFrame], answer: [Answer], namespace: Namespace.ID) {
            self.init(frames: pframes.sorted(by: {$0.priority < $1.priority}).map(\.frame), answer: answer, namespace: namespace)
        }
        
        var body: some View {
            VStack {
                ForEach(frames, id: \.utid) { frame in
                    switch onEnum(of: frame) {
                    case .answerable(let frame):
                        StatefulFrame(data: frame, answers: answer.filter { $0.frameId == frame.id }, namespace: namespace)
                    default:
                        StatelessFrame(data: frame, namespace: namespace)
                    }
                }
            }
        }
    }
    
    struct StatelessFrame : View {
        let data: Frame
        let namespace: Namespace.ID

        var body: some View {
            switch onEnum(of: data) {
            case .answerable(_):
                fatalError("Stateless frame with Answerable model.")
            case .image(let image):
                ImageFrameView(frame: image.imageFrame)
                    .matchedGeometryEffect(id: image.utid, in: namespace)
            case .text(let text):
                TextFrameView(frame: text.textFrame)
                    .matchedTransitionSource(id: text.utid, in: namespace)
            }
        }
    }
    
    struct StatefulFrame : View {
        let data: FrameAnswerable
        let answers: [Answer]
        let namespace: Namespace.ID
        
        init(data: FrameAnswerable, answers: [Answer], namespace: Namespace.ID) {
            self.data = data
            self.answers = answers
            self.namespace = namespace
        }
        
        var body: some View {
            switch onEnum(of: data) {
            case .options(let options):
                VStack(alignment: .leading) {
                    ForEach(options.frames, id: \.frame.utid) { option in
                        OptionAnswerFrame(data: option, isSelected: answers.contains(where: { $0.frameId == option.frame.id }), namespace: namespace)
                    }
                }
            }
        }
        
        struct OptionAnswerFrame : View {
            let data: KeyedPrioritizedFrame
            @State var isSelected: Bool
            let namespace: Namespace.ID
            
            var body: some View {
                Checkmark(isOn: Binding(get: {
                    isSelected
                }, set: { newValue in
                    isSelected = newValue
                })) {
                    StatelessFrame(data: data.frame, namespace: namespace)
                }
            }
        }
    }
}
