import Foundation
import SwiftUI
import ComposeApp
import ImageIO

struct QuestionEditor : View {
    @Binding var data: [Frame]
    let namespace: Namespace.ID
    @Binding var history: History
    
    var body: some View {
        List(data, id: \.id) { frame in
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
                        Button("Delete Options", systemImage: "trash", role: .destructive) {
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
            Button("Undo", systemImage: "arrow.uturn.backward.circle") {
                if let mod = history.undo() {
                    undo(mod)
                }
            }
            .disabled(!history.canUndo)
            Button("Redo", systemImage: "arrow.uturn.forward.circle") {
                if let mod = history.redo() {
                    redo(mod)
                }
            }
            .disabled(!history.canRedo)
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
    }
    
    func updateIsKey(options: FrameOptions, item: KeyedPrioritizedFrame, newValue: Bool) {
        let newOption = KeyedPrioritizedFrame(frame: item.frame, isKey: newValue, priority: item.priority)
        updateOption(options: options, newValue: newOption)
    }
    
    func updateOptionFrame(options: FrameOptions, item: KeyedPrioritizedFrame, newValue: Frame) {
        updateOption(options: options, newValue: KeyedPrioritizedFrame(frame: newValue, isKey: item.isKey, priority: item.priority))
    }
    
    func deleteOption(options: FrameOptions, item: KeyedPrioritizedFrame) {
        let index = data.firstIndex { $0.id == options.optionsFrame.id }!
        let optionIndex = options.frames.firstIndex { $0.frame.id == item.frame.id }!
        let newFrame = FrameOptions(optionsFrame: options.optionsFrame, frames: Array(options.frames[..<optionIndex] + options.frames[(optionIndex + 1)...]))
        let mod: Modification = .update(oldValue: data[index], newValue: newFrame)
        history.append(mod)
        redo(mod)
    }
    
    func deleteFrame(id: Int64) {
        let index = data.firstIndex { $0.id == id }!
        let mod: Modification = .delete(frame: data[index], at: index)
        history.append(mod)
        redo(mod)
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
        let nextId = (data.max(by: { $0.id < $1.id })?.id ?? 0) + 1
        let newFrame = createFrameFromType(id: nextId, itemType: itemType)
        let mod: Modification = .push(frame: newFrame, at: data.count)
        history.append(mod)
        redo(mod)
    }

    func updateFrame(newValue: Frame) {
        let index = data.firstIndex { $0.id == newValue.id }!
        let mod: Modification = .update(oldValue: data[index], newValue: newValue)
        history.append(mod)
        redo(mod)
    }
    
    func undo(_ mod: Modification) {
        switch mod {
        case .update(let oldValue, let newValue):
            let index = data.firstIndex { $0.id == newValue.id }!
            data = Array(data[..<index] + [oldValue] + data[(index + 1)...])
            
        case .push(_, let at):
            data = Array(data[..<at] + data[(at + 1)...])
            
        case .delete(let frame, let at):
            data = Array(data[..<at] + [frame] + data[at...])
            
        case .renameQuiz(_, _):
            return
        }
    }
    
    func redo(_ mod: Modification) {
        switch mod {
        case .update(_, let newValue):
            let index = data.firstIndex { $0.id == newValue.id }!
            data = Array(data[..<index] + [newValue] + data[(index + 1)...])

        case .push(let value, let at):
            data = Array(data[..<at] + [value] + data[at...])
            
        case .delete(_, let at):
            data = Array(data[..<at] + data[(at + 1)...])

        case .renameQuiz(_, _):
            return
        }
    }
}

#Preview {
    @Previewable @State var frames: [Frame] = [
        FrameText(id: 0, textFrame: TextFrame(id: 0, embeddingsId: nil, content: "What's the meaning of life?")),
        FrameImage(id: 1, imageFrame: ImageFrame(id: 0, embeddingsId: nil, filename: "", width: -1, height: -1, altText: nil)),
        FrameOptions(optionsFrame: OptionsFrame(id: 2, name: nil), frames: [
            KeyedPrioritizedFrame(frame: FrameText(id: 1, textFrame: TextFrame(id: 1, embeddingsId: nil, content: "To have fun")), isKey: true, priority: 0)
        ])
    ]
    @Previewable @Namespace var namespace
    @Previewable @State var history = History()
    NavigationStack {
        QuestionEditor(data: $frames, namespace: namespace, history: $history)
            .navigationTitle("Sample Question")
    }
}
