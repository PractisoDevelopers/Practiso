import Foundation
import SwiftUI
import ComposeApp
import ImageIO

struct QuestionEditor : View {
    @Binding var data: QuizFrames
    let namespace: Namespace.ID
    @Binding var history: [Modification]
    
    private var frames: [Frame] {
        data.frames.sorted { $0.priority < $1.priority }.map(\.frame)
    }
    
    var body: some View {
        List(frames, id: \.id) { frame in
            Group {
                switch frame {
                case let options as FrameOptions:
                    VStack {
                        TextField(text: Binding(get: {
                            options.optionsFrame.name ?? ""
                        }, set: { newValue, _ in
                            let name: String? = if newValue.isEmpty {
                                nil
                            } else {
                                newValue
                            }
                            updateFrame(newValue: FrameOptions(optionsFrame: OptionsFrame(id: options.optionsFrame.id, name: name), frames: options.frames))
                        }), label: { Text("New options frame") })
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

                        Menu {
                            Button("Text", systemImage: "character.textbox") {
                                withAnimation {
                                    appendToOptionFrame(options: options, itemType: FrameText.self)
                                }
                            }
                            Button("Image", systemImage: "photo") {
                                withAnimation {
                                    appendToOptionFrame(options: options, itemType: FrameImage.self)
                                }
                            }
                        } label: {
                            HStack {
                                Image(systemName: "plus.circle.fill")
                                    .checkmarkStyleBase()
                                    .foregroundStyle(.green)
                                Text("Add Option")
                            }
                        }
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .animation(.default, value: options)
                    .contextMenu {
                        Button("Delete All", systemImage: "trash", role: .destructive) {
                            deleteFrame(id: options.id)
                        }
                    }
                    .matchedGeometryEffect(id: frame.id, in: namespace)
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
                    .matchedGeometryEffect(id: frame.id, in: namespace)
                    .contextMenu {
                        Button("Delete", systemImage: "trash", role: .destructive) {
                            deleteFrame(id: frame.id)
                        }
                    }
                }
            }
            .padding(.vertical, 8)
            .swipeActions(edge: .trailing) {
                Button("Delete", systemImage: "trash.fill", role: .destructive) {
                    deleteFrame(id: frame.id)
                }
            }
        }
        .environment(\.editMode, Binding.constant(.inactive))
        .listStyle(.plain)
        .toolbar {
            Menu("Add", systemImage: "plus") {
                Button("Text", systemImage: "character.textbox") {
                    withAnimation {
                        appendFrame(itemType: FrameText.self)
                    }
                }
                Button("Image", systemImage: "photo") {
                    withAnimation {
                        appendFrame(itemType: FrameImage.self)
                    }
                }
                Button("Options", systemImage: "checklist") {
                    withAnimation {
                        appendFrame(itemType: FrameOptions.self)
                    }
                }
            }
        }
        .onAppear {
            history = [] // editor always starts with empty history
        }
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
        history.append(.update(oldValue: data.frames[index].frame, newValue: wrapped.frame))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + [wrapped] + data.frames[(index + 1)...]))
    }
    
    func deleteFrame(id: Int64) {
        let index = data.frames.firstIndex { $0.frame.id == id }!
        history.append(.delete(frame: data.frames[index].frame, at: index))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + data.frames[(index + 1)...]))
    }

    func updateOption(options: FrameOptions, newValue: KeyedPrioritizedFrame) {
        let optionIndex = options.frames.firstIndex { $0.frame.id == newValue.frame.id }!
        let newFrame = FrameOptions(optionsFrame: options.optionsFrame, frames: Array(options.frames[..<optionIndex] + [newValue] + options.frames[(optionIndex + 1)...]))
        updateFrame(newValue: newFrame)
    }
    
    func createFrameFromType(id: Int64, itemType: Frame.Type) -> any Frame {
        let item: Frame? = if itemType is FrameImage.Type {
            FrameImage(id: id, imageFrame: ImageFrame(id: id, embeddingsId: nil, filename: "", width: 0, height: 0, altText: nil))
        } else if itemType is FrameText.Type {
            FrameText(id: id, textFrame: TextFrame(id: id, embeddingsId: nil, content: ""))
        } else if itemType is FrameOptions.Type {
            FrameOptions(optionsFrame: OptionsFrame(id: id, name: nil), frames: [])
        } else {
            nil
        }
        if item == nil {
            assertionFailure("Unsupported item type \(itemType)")
        }
        return item!
    }
    
    func appendToOptionFrame(options: FrameOptions, itemType: Frame.Type) {
        let nextId = (options.frames.max(by: { $0.frame.id < $1.frame.id })?.frame.id ?? -1) + 1
        let wrapped = KeyedPrioritizedFrame(frame: createFrameFromType(id: nextId, itemType: itemType),
                                            isKey: false, priority: (options.frames.max(by: { $0.priority < $1.priority })?.priority ?? -1) + 1)
        updateFrame(newValue: FrameOptions(optionsFrame: options.optionsFrame, frames: options.frames + [wrapped]))
    }
    
    func appendFrame(itemType: Frame.Type) {
        let nextId = (data.frames.max(by: { $0.frame.id < $1.frame.id })?.frame.id ?? 0) + 1
        let nextPriority = (data.frames.max(by: {$0.priority < $1.priority })?.priority ?? -1) + 1
        let wrapped = PrioritizedFrame(frame: createFrameFromType(id: nextId, itemType: itemType), priority: nextPriority)
        history.append(.push(frame: wrapped.frame, at: data.frames.count))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames + [wrapped]))
    }

    func updateFrame(newValue: Frame) {
        let index = data.frames.firstIndex { $0.frame.id == newValue.id }!
        let wrapped = PrioritizedFrame(frame: newValue, priority: data.frames[index].priority)
        history.append(.update(oldValue: data.frames[index].frame, newValue: newValue))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + [wrapped] + data.frames[(index + 1)...]))
    }
    
    func updateFrame(newValue: PrioritizedFrame) {
        let index = data.frames.firstIndex { $0.frame.id == newValue.frame.id }!
        history.append(.update(oldValue: data.frames[index].frame, newValue: newValue.frame))
        data = QuizFrames(quiz: data.quiz, frames: Array(data.frames[..<index] + [newValue] + data.frames[(index + 1)...]))
    }
}

#Preview {
    @Previewable @State var frames = QuizFrames(quiz: Quiz(id: 0, name: "Sample quiz", creationTimeISO: Kotlinx_datetimeInstant.Companion.shared.DISTANT_FUTURE, modificationTimeISO: nil), frames: [
        PrioritizedFrame(frame: FrameText(id: 0, textFrame: TextFrame(id: 0, embeddingsId: nil, content: "What's the meaning of life?")), priority: 0),
        PrioritizedFrame(frame: FrameImage(id: 1, imageFrame: ImageFrame(id: 0, embeddingsId: nil, filename: "", width: -1, height: -1, altText: nil)), priority: 1),
        PrioritizedFrame(frame: FrameOptions(optionsFrame: OptionsFrame(id: 2, name: nil), frames: [
            KeyedPrioritizedFrame(frame: FrameText(id: 1, textFrame: TextFrame(id: 1, embeddingsId: nil, content: "To have fun")), isKey: true, priority: 0)
        ]), priority: 2)
    ])
    @Previewable @Namespace var namespace
    @Previewable @State var history: [Modification] = []
    NavigationStack {
        QuestionEditor(data: $frames, namespace: namespace, history: $history)
            .navigationTitle("Sample Question")
    }
}
