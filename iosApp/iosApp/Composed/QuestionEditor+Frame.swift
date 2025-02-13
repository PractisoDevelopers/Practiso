import Foundation
import Combine
import SwiftUI
import ComposeApp

struct FrameEditor : View {
    @Binding var frame: Frame
    let onDelete: () -> Void
    
    private class Model : ObservableObject, Observable, Cancellable {
        @Published var textBuffer: String? = nil
        @Published var textOutput: String? = nil
        
        private var subscriptions = Set<AnyCancellable>()
        
        init() {
            $textBuffer.debounce(for: .milliseconds(400), scheduler: DispatchQueue.main)
                .sink { [weak self] output in
                    self?.textOutput = output
                }
                .store(in: &subscriptions)
        }
        
        func snapTo(text: String) {
            textOutput = text
            textBuffer = text
        }
        
        func cancel() {
            subscriptions.forEach { $0.cancel() }
        }
    }
    
    private struct DebouncedTextField : View {
        @Binding var text: String
        @StateObject private var model = Model()
        
        var body: some View {
            TextField(text: Binding(get: {
                model.textBuffer ?? text
            }, set: {
                model.textBuffer = $0
            }), axis: .vertical, label: {
                Text("Type text here...")
            })
            .onAppear {
                model.snapTo(text: text)
            }
            .onDisappear {
                if let buffer = model.textBuffer {
                    text = buffer
                }
                model.cancel()
            }
            .onReceive(model.$textOutput) { output in
                if output != text, let output = output {
                    text = output
                }
            }
            .onChange(of: text) { _, newValue in
                if newValue != model.textOutput {
                    model.snapTo(text: newValue)
                }
            }
        }
    }
    
    var body: some View {
        switch frame {
        case let text as FrameText:
            DebouncedTextField(text: Binding(get: {
                text.textFrame.content
            }, set: { newValue, _ in
                if newValue == text.textFrame.content {
                    return
                }
                let textFrame = TextFrame(id: text.textFrame.id, embeddingsId: text.textFrame.embeddingsId, content: newValue)
                frame = FrameText(id: frame.id, textFrame: textFrame)
            }))
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
