import Foundation
import SwiftUI
import ComposeApp

extension AnswerView {
    struct Page : View {
        let quizId: Int64
        let frames: [Frame]
        let answer: [PractisoAnswer]
        let service: TakeService
        let namespace: Namespace.ID
        
        init(quizFrames: QuizFrames, answer: [PractisoAnswer], service: TakeService, namespace: Namespace.ID) {
            self.quizId = quizFrames.quiz.id
            self.frames = quizFrames.frames.sorted(by: { $0.priority < $1.priority }).map(\.frame)
            self.answer = answer
            self.service = service
            self.namespace = namespace
        }
        
        var body: some View {
            VStack {
                ForEach(frames, id: \.utid) { frame in
                    switch onEnum(of: frame) {
                    case .answerable(let frame):
                        StatefulFrame(quizId: quizId, data: frame, answers: answer.filter { $0.frameId == frame.id },
                                      service: service, namespace: namespace)
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
        @Environment(ContentView.ErrorHandler.self) private var errorHandler
        
        let quizId: Int64
        let data: FrameAnswerable
        let answers: [PractisoAnswer]
        let service: TakeServiceSync
        let namespace: Namespace.ID
        
        init(quizId: Int64, data: FrameAnswerable, answers: [PractisoAnswer], service: TakeService, namespace: Namespace.ID) {
            self.quizId = quizId
            self.data = data
            self.answers = answers
            self.service = TakeServiceSync(base: service)
            self.namespace = namespace
        }
        
        var body: some View {
            switch onEnum(of: data) {
            case .options(let options):
                VStack(alignment: .leading) {
                    ForEach(Array(options.frames.enumerated()), id: \.element.frame.utid) { index, option in
                        OptionAnswerFrame(
                            data: option,
                            isSelected: Binding(get: {
                                answers.contains(where: {
                                    if let o = $0 as? PractisoAnswerOption {
                                        o.optionId == option.frame.id
                                    } else {
                                        false
                                    }
                                })
                            }, set: { newValue in
                                let answer = PractisoAnswerOption(optionId: option.frame.id, frameId: data.id, quizId: quizId)
                                errorHandler.catchAndShowImmediately {
                                    if newValue {
                                        try service.commitAnswer(model: answer, priority: Int32(index))
                                    } else {
                                        try service.rollbackAnswer(model: answer)
                                    }
                                }
                            }),
                            namespace: namespace
                        )
                    }
                }
            }
        }
        
        struct OptionAnswerFrame : View {
            let data: KeyedPrioritizedFrame
            @Binding var isSelected: Bool
            let namespace: Namespace.ID
            
            var body: some View {
                Checkmark(isOn: $isSelected) {
                    StatelessFrame(data: data.frame, namespace: namespace)
                }
                .onTapGesture {
                    isSelected = !isSelected
                }
            }
        }
    }
}
