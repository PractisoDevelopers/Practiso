import Foundation
import SwiftUI
import ComposeApp
import ImageIO

private enum Modification {
    case update(oldValue: PrioritizedFrame, newValue: PrioritizedFrame)
    case push(Frame)
    case delete(PrioritizedFrame)
    case renameQuiz(oldName: String?, newName: String?)
}

struct QuestionEditor : View {
    @Binding var data: QuizFrames
    let namespace: Namespace.ID
    
    @State private var history: [Modification] = []
    
    private var nextFrameId: Int64 {
        (data.frames.last?.frame.id ?? -1) + 1
    }
    private var frames: [Frame] {
        data.frames.sorted { $0.priority < $1.priority }.map(\.frame)
    }
    
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
                    EmptyView()
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
    
    var body: some View {
        List(frames, id: \.id) { frame in
            Group {
                switch frame {
                case let options as FrameOptions:
                    VStack {
                        TextField(text: Binding(get: {
                            options.optionsFrame.name ?? String(localized: "New option frame")
                        }, set: { newValue, _ in
                            let name: String? = if newValue.isEmpty {
                                nil
                            } else {
                                newValue
                            }
                            updateFrame(newValue: FrameOptions(optionsFrame: OptionsFrame(id: options.optionsFrame.id, name: name), frames: options.frames))
                        }), label: { EmptyView() })
                        .foregroundStyle(.secondary)
                        OptionsFrameView(frame: options, showName: false) { option in
                            Checkmark(isOn: Binding(get: { option.isKey }, set: { newValue, _ in
                                updateIsKey(options: options, item: option, newValue: newValue)
                            })) {
                                FrameEditor(
                                    frame: Binding(get: {
                                        option.frame
                                    }, set: { newValue in
                                        updateOptionFrame(options: options, item: option, newValue: newValue)
                                    }),
                                    onDelete: {
                                        deleteOption(options: options, item: option)
                                    }
                                )
                            }
                        }
                    }
                    .contextMenu {
                        Button("Delete All", systemImage: "trash", role: .destructive) {
                            deleteFrame(id: options.id)
                        }
                    }
                default:
                    FrameEditor(
                        frame: Binding(get: {
                            frame
                        }, set: { newValue in
                            updateFrame(newValue: newValue)
                        }),
                        onDelete: {
                            deleteFrame(id: frame.id)
                        }
                    )
                    .contextMenu {
                        Button("Delete", systemImage: "trash", role: .destructive) {
                            deleteFrame(id: frame.id)
                        }
                    }
                }
            }
            .matchedGeometryEffect(id: frame.id, in: namespace)
            .padding(.vertical, 8)
            .swipeActions(edge: .trailing) {
                Button("Delete", systemImage: "trash.fill", role: .destructive) {
                    let index = data.frames.firstIndex { $0.frame.id == frame.id }!
                    history.append(.delete(data.frames[index]))
                    data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + data.frames[(index+1)...]))
                }
            }
        }
        .environment(\.editMode, Binding.constant(.inactive))
        .listStyle(.plain)
    }
    
    func updateIsKey(options: FrameOptions, item: KeyedPrioritizedFrame, newValue: Bool) {
        let newOption = KeyedPrioritizedFrame(frame: item.frame, isKey: newValue, priority: item.priority)
        updateOption(options: options, newValue: newOption)
    }
    
    func updateOptionFrame(options: FrameOptions, item: KeyedPrioritizedFrame, newValue: Frame) {
        updateOption(options: options, newValue: KeyedPrioritizedFrame(frame: newValue, isKey: item.isKey, priority: item.priority))
    }
    
    func deleteOption(options: FrameOptions, item: KeyedPrioritizedFrame) {
        let index = data.frames.firstIndex { $0.frame.id == options.optionsFrame.id }!
        let optionIndex = options.frames.firstIndex { $0.frame.id == item.frame.id }!
        let newFrame = FrameOptions(optionsFrame: options.optionsFrame, frames: Array(options.frames[..<optionIndex] + options.frames[(optionIndex + 1)...]))
        let wrapped = PrioritizedFrame(frame: newFrame, priority: data.frames[index].priority)
        history.append(.update(oldValue: data.frames[index], newValue: wrapped))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + [wrapped] + data.frames[(index + 1)...]))
    }
    
    func deleteFrame(id: Int64) {
        let index = data.frames.firstIndex { $0.frame.id == id }!
        history.append(.delete(data.frames[index]))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + data.frames[(index + 1)...]))
    }

    func updateOption(options: FrameOptions, newValue: KeyedPrioritizedFrame) {
        let optionIndex = options.frames.firstIndex { $0.frame.id == newValue.frame.id }!
        let newFrame = FrameOptions(optionsFrame: options.optionsFrame, frames: Array(options.frames[..<optionIndex] + [newValue] + options.frames[(optionIndex + 1)...]))
        updateFrame(newValue: newFrame)
    }
    
    func updateFrame(newValue: Frame) {
        let index = data.frames.firstIndex { $0.frame.id == newValue.id }!
        let wrapped = PrioritizedFrame(frame: newValue, priority: data.frames[index].priority)
        history.append(.update(oldValue: data.frames[index], newValue: wrapped))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + [wrapped] + data.frames[(index + 1)...]))
    }
    
    func updateFrame(newValue: PrioritizedFrame) {
        let index = data.frames.firstIndex { $0.frame.id == newValue.frame.id }!
        history.append(.update(oldValue: data.frames[index], newValue: newValue))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + [newValue] + data.frames[(index + 1)...]))
    }
}

#Preview {
    @Previewable @State var frames = QuizFrames(quiz: Quiz(id: 0, name: "Sample quiz", creationTimeISO: Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE, modificationTimeISO: nil), frames: [
        PrioritizedFrame(frame: FrameText(id: 0, textFrame: TextFrame(id: 0, embeddingsId: nil, content: "What's the meaning of life?")), priority: 0),
        PrioritizedFrame(frame: FrameImage(id: 1, imageFrame: ImageFrame(id: 0, embeddingsId: nil, filename: "", width: -1, height: -1, altText: nil)), priority: 1),
        PrioritizedFrame(frame: FrameOptions(optionsFrame: OptionsFrame(id: 0, name: nil), frames: []), priority: 2)
    ])
    @Previewable @Namespace var namespace
    NavigationStack {
        QuestionEditor(data: $frames, namespace: namespace)
            .navigationTitle("Sample Question")
    }
}
