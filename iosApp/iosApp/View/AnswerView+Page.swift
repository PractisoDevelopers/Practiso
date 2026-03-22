import Foundation
import SwiftUI
import ComposeApp

extension AnswerView {
    struct Page : View {
        let quizId: Int64
        let frames: [Frame]
        let answer: [PractisoAnswer]
        
        init(quizFrames: QuizFrames, answer: [PractisoAnswer]) {
            self.quizId = quizFrames.quiz.id
            self.frames = quizFrames.frames.sorted(by: { $0.priority < $1.priority }).map(\.frame)
            self.answer = answer
        }
        
        var body: some View {
            VStack {
                ForEach(frames, id: \.utid) { frame in
                    switch onEnum(of: frame) {
                    case .answerable(let frame):
                        StatefulFrame(quizId: quizId, data: frame, answers: answer.filter { $0.frameId == frame.id })
                    default:
                        StatelessFrame(data: frame)
                    }
                }
            }
        }
    }
    
    struct StatelessFrame : View {
        let data: Frame
        var body: some View {
            switch onEnum(of: data) {
            case .answerable(_):
                fatalError("Stateless frame with Answerable model.")
            case .image(let image):
                ImageFrameView(frame: image.imageFrame)
            case .text(let text):
                TextFrameView(frame: text.textFrame)
            }
        }
    }
    
    struct StatefulFrame : View {
        @AppStorage(.showAccuracy) private var showAccuracy = false
        
        let quizId: Int64
        let data: FrameAnswerable
        let answers: [PractisoAnswer]
        
        init(quizId: Int64, data: FrameAnswerable, answers: [PractisoAnswer]) {
            self.quizId = quizId
            self.data = data
            self.answers = answers
        }
        
        var body: some View {
            Group {
                switch onEnum(of: data) {
                case .options(let options):
                    if options.frames.count(where: {$0.isKey}) <= 1 {
                        SingleAnswerOptionsFrame(options: options, answers: answers, quizId: quizId)
                    } else {
                        MultipleAnswerOptionsFrame(options: options, answers: answers, quizId: quizId)
                    }
                }
            }
            .background()
            .contextMenu {
                Toggle(isOn: $showAccuracy) {
                    Text("Show Accuracy")
                }
            }
        }
        
        struct SingleAnswerOptionsFrame : View {
            @Environment(ContentView.ErrorHandler.self) private var errorHandler
            @Environment(\.takeService) private var service
            @AppStorage(.showAccuracy) private var showAccuracy = false

            let options: FrameOptions
            let answers: [PractisoAnswer]
            let quizId: Int64
            
            @State private var xOffsets: [Double]
            
            init(options: FrameOptions, answers: [PractisoAnswer], quizId: Int64) {
                self.options = options
                self.answers = answers
                self.quizId = quizId
                
                self.xOffsets = Array(repeating: 0.0, count: options.frames.count)
            }

            var body: some View {
                VStack(alignment: .leading) {
                    ForEach(Array(options.frames.enumerated()), id: \.element.frame.utid) { index, option in
                        Checkmark(isOn: itemBindings[index]) {
                            StatelessFrame(data: option.frame)
                                .offset(x: xOffsets[index])
                                .onTapGesture {
                                    itemBindings[index].wrappedValue.toggle()
                                }
                        }
                    }
                }
            }
            
            var itemBindings: [Binding<Bool>] {
                options.frames.enumerated().map { (index, option) in
                    Binding(get: {
                        answers.contains(where: {
                            if let o = $0 as? PractisoAnswerOption {
                                o.optionId == option.frame.id
                            } else {
                                false
                            }
                        })
                    }, set: { newValue in
                        let service = TakeServiceSync(base: service)
                        errorHandler.catchAndShowImmediately {
                            let answer = PractisoAnswerOption(optionId: option.frame.id, frameId: options.id, quizId: quizId)
                            if newValue {
                                try options.frames
                                    .filter { $0.frame.id != option.frame.id }
                                    .map { PractisoAnswerOption(optionId: $0.frame.id, frameId: options.id, quizId: quizId) }
                                    .forEach { otherAnswer in
                                        try service.rollbackAnswer(model: otherAnswer)
                                    }
                                try service.commitAnswer(model: answer, priority: Int32(index)) // TODO: use quiz index as priority
                            } else {
                                try service.rollbackAnswer(model: answer)
                            }
                        }
                        
                        if showAccuracy && newValue && !option.isKey {
                            CoreHapticFeedback.shared?.wobble()
                            withAnimation(.wobbleAnimation) {
                                xOffsets[index] = 1
                            } completion: {
                                xOffsets[index] = 0
                            }
                        }
                    })
                }
            }
        }
        
        struct MultipleAnswerOptionsFrame : View {
            @Environment(ContentView.ErrorHandler.self) private var errorHandler
            @Environment(\.takeService) private var service
            @AppStorage(.showAccuracy) private var showAccuracy = false

            let options: FrameOptions
            let answers: [PractisoAnswer]
            let quizId: Int64
            
            @State private var xOffsets: [Double]
            
            init(options: FrameOptions, answers: [PractisoAnswer], quizId: Int64) {
                self.options = options
                self.answers = answers
                self.quizId = quizId
                
                self.xOffsets = Array(repeating: 0.0, count: options.frames.count)
            }
            
            var body: some View {
                VStack(alignment: .leading) {
                    ForEach(Array(options.frames.enumerated()), id: \.element.frame.utid) { index, option in
                        CheckmarkSquare(isOn: itemBindings[index]) {
                            StatelessFrame(data: option.frame)
                                .offset(x: xOffsets[index])
                                .onTapGesture {
                                    itemBindings[index].wrappedValue.toggle()
                                }
                        }
                    }
                }
            }
            
            var itemBindings: [Binding<Bool>] {
                options.frames.enumerated().map { (index, option) in
                    Binding(get: {
                        answers.contains(where: {
                            if let o = $0 as? PractisoAnswerOption {
                                o.optionId == option.frame.id
                            } else {
                                false
                            }
                        })
                    }, set: { newValue in
                        let answer = PractisoAnswerOption(optionId: option.frame.id, frameId: options.id, quizId: quizId)
                        let service = TakeServiceSync(base: service)
                        errorHandler.catchAndShowImmediately {
                            if newValue {
                                try service.commitAnswer(model: answer, priority: Int32(index)) // TODO: use quiz index as priority
                            } else {
                                try service.rollbackAnswer(model: answer)
                            }
                        }
                        if showAccuracy && newValue && !option.isKey {
                            CoreHapticFeedback.shared?.wobble()
                            withAnimation(.wobbleAnimation) {
                                xOffsets[index] = 1
                            } completion: {
                                xOffsets[index] = 0
                            }
                        }
                    })
                }
            }
        }
    }
}

extension Animation {
    static var wobbleAnimation: Animation {
        .interpolatingSpring(.init(mass: 0.1, stiffness: 30, damping: 1.5), initialVelocity: 2000)
    }
}
